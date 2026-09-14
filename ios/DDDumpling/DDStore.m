#import "DDStore.h"

#import <CommonCrypto/CommonDigest.h>
#import "IOSPrimitiveArray.h"
#import "com/dddumpling/game/Collect.h"

static NSString *const DDStoreMagic = @"dddumpling-store";
static NSString *const DDStoreVersion = @"v1";
static NSString *const DDStoreWriterKey = @"progressWriter";
@interface DDIOSStore ()
@property(nonatomic) NSMutableDictionary<NSString *, id> *values;
@property(nonatomic) NSURL *fileURL;
@property(nonatomic) NSLock *lock;
@property(nonatomic, readwrite, nullable) NSString *error;
@property(nonatomic) BOOL healthy;
@end

@implementation DDIOSStore

- (instancetype)init {
  return [self initWithURL:[DDIOSStore defaultStoreURL]];
}

- (instancetype)initWithURL:(NSURL *)fileURL {
  if ((self = [super init])) {
    _lock = [[NSLock alloc] init];
    _values = [NSMutableDictionary dictionary];
    _fileURL = [fileURL copy];
    _healthy = YES;
    [self read];
    if (_healthy && ![self stringForKey:DDStoreWriterKey validWriter:YES].length) {
      _values[DDStoreWriterKey] = NSUUID.UUID.UUIDString;
      [self persist];
    }
  }
  return self;
}

+ (NSURL *)defaultStoreURL {
  NSFileManager *files = NSFileManager.defaultManager;
  NSURL *base = [files URLForDirectory:NSApplicationSupportDirectory
                              inDomain:NSUserDomainMask
                     appropriateForURL:nil create:YES error:nil];
  if (!base) base = [files URLsForDirectory:NSLibraryDirectory inDomains:NSUserDomainMask].firstObject;
  NSString *bundle = NSBundle.mainBundle.bundleIdentifier ?: @"com.dddumpling.game";
  NSURL *directory = [base URLByAppendingPathComponent:bundle isDirectory:YES];
  [files createDirectoryAtURL:directory withIntermediateDirectories:YES attributes:nil error:nil];
  return [directory URLByAppendingPathComponent:@"game-store-v1.plist" isDirectory:NO];
}

- (void)fail:(NSString *)reason {
  _healthy = NO;
  _error = reason;
}

- (NSData *)checksum:(NSData *)payload {
  unsigned char hash[CC_SHA256_DIGEST_LENGTH];
  CC_SHA256(payload.bytes, (CC_LONG)payload.length, hash);
  return [NSData dataWithBytes:hash length:sizeof(hash)];
}

- (void)read {
  NSError *readError = nil;
  NSData *raw = [NSData dataWithContentsOfURL:_fileURL options:0 error:&readError];
  if (!raw) {
    if (readError.code == NSFileReadNoSuchFileError) return;
    [self fail:@"Your saved game could not be read. It has not been changed."];
    return;
  }
  if (!raw.length) {
    [self fail:@"Your saved game appears damaged. It has not been changed."];
    return;
  }
  NSError *error = nil;
  id envelope = [NSPropertyListSerialization propertyListWithData:raw options:NSPropertyListImmutable
                                                             format:nil error:&error];
  if (error || ![envelope isKindOfClass:NSDictionary.class]) {
    [self fail:@"Your saved game could not be read. It has not been changed."];
    return;
  }
  NSDictionary *container = envelope;
  NSData *payload = [container[@"payload"] isKindOfClass:NSData.class] ? container[@"payload"] : nil;
  NSData *sum = [container[@"checksum"] isKindOfClass:NSData.class] ? container[@"checksum"] : nil;
  if (![container[@"magic"] isEqual:DDStoreMagic] || ![container[@"version"] isEqual:DDStoreVersion]
      || !payload.length || ![sum isEqualToData:[self checksum:payload]]) {
    [self fail:@"Your saved game appears damaged. It has not been changed."];
    return;
  }
  id decoded = [NSPropertyListSerialization propertyListWithData:payload options:NSPropertyListMutableContainers
                                                           format:nil error:&error];
  if (!error && [decoded isKindOfClass:NSDictionary.class]) {
    _values = [decoded mutableCopy];
  } else {
    [self fail:@"Your saved game appears damaged. It has not been changed."];
  }
}

/** Serializes an envelope first, then atomically replaces the last fully valid save. */
- (BOOL)persist {
  if (!_healthy) return NO;
  NSError *error = nil;
  NSData *payload = [NSPropertyListSerialization dataWithPropertyList:_values
                                                                 format:NSPropertyListBinaryFormat_v1_0
                                                                options:0 error:&error];
  if (!payload || error) { [self fail:@"Your saved game could not be encoded."]; return NO; }
  NSDictionary *envelope = @{ @"magic": DDStoreMagic, @"version": DDStoreVersion,
                               @"payload": payload, @"checksum": [self checksum:payload] };
  NSData *data = [NSPropertyListSerialization dataWithPropertyList:envelope
                                                              format:NSPropertyListBinaryFormat_v1_0
                                                             options:0 error:&error];
  if (!data || error) { [self fail:@"Your saved game could not be encoded."]; return NO; }
  if (![data writeToURL:_fileURL options:NSDataWritingAtomic error:&error]) {
    [self fail:@"Your saved game could not be written."];
    return NO;
  }
  return YES;
}

- (NSNumber *)numberForKey:(NSString *)key {
  id value = _values[key];
  return [value isKindOfClass:NSNumber.class] ? value : nil;
}

- (NSString *)stringForKey:(NSString *)key validWriter:(BOOL)writer {
  id value = _values[key];
  if (![value isKindOfClass:NSString.class]) return nil;
  if (writer) {
    NSString *text = value;
    NSMutableCharacterSet *allowed = [[NSCharacterSet alphanumericCharacterSet] mutableCopy];
    [allowed addCharactersInString:@"_-"];
    if (text.length < 1 || text.length > 64
        || [text rangeOfCharacterFromSet:allowed.invertedSet].location != NSNotFound) return nil;
  }
  return value;
}

- (void)setValue:(id)value forKey:(NSString *)key {
  [_lock lock];
  if (!_healthy) { [_lock unlock]; return; }
  if (value) _values[key] = value; else [_values removeObjectForKey:key];
  [self persist];
  [_lock unlock];
}

- (jint)intForKey:(NSString *)key defaultValue:(jint)fallback {
  NSNumber *number = [self numberForKey:key];
  if (!number) return fallback;
  long long value = number.longLongValue;
  return value < INT32_MIN ? INT32_MIN : value > INT32_MAX ? INT32_MAX : (jint)value;
}

// Progress.Store -----------------------------------------------------------

- (IOSByteArray *)loadProgress {
  [_lock lock];
  NSData *data = [_values[@"progress"] isKindOfClass:NSData.class] ? _values[@"progress"] : nil;
  IOSByteArray *out = [IOSByteArray arrayWithLength:data.length];
  if (data.length) memcpy(out->buffer_, data.bytes, data.length);
  [_lock unlock];
  return out;
}

- (void)saveProgressWithByteArray:(IOSByteArray *)data {
  NSData *copy = data ? [NSData dataWithBytes:data->buffer_ length:data->size_] : [NSData data];
  [self setValue:copy forKey:@"progress"];
}

- (NSString *)progressReplica {
  [_lock lock];
  NSString *writer = [self stringForKey:DDStoreWriterKey validWriter:YES];
  [_lock unlock];
  return writer ?: @"memory";
}

// GameCore.Store -----------------------------------------------------------

- (jint)loadBest { return MAX(0, [self intForKey:@"best" defaultValue:0]); }
- (void)saveBestWithInt:(jint)best { [self setValue:@(best) forKey:@"best"]; }
- (NSString *)loadReleaseSeen { return [self stringForKey:@"releaseSeen" validWriter:NO] ?: @""; }
- (void)saveReleaseSeenWithNSString:(NSString *)value { [self setValue:value forKey:@"releaseSeen"]; }
- (jint)loadLandState { return [self intForKey:@"landState" defaultValue:0]; }
- (void)saveLandStateWithInt:(jint)value { [self setValue:@(value) forKey:@"landState"]; }

- (jint)loadLandBestWithInt:(jint)land {
  if (land == 0) return [self loadBest];
  NSString *key = [NSString stringWithFormat:@"landBest.%d", land];
  return MAX(0, [self intForKey:key defaultValue:0]);
}
- (void)saveLandBestWithInt:(jint)land withInt:(jint)value {
  if (land == 0) { [self saveBestWithInt:value]; return; }
  [self setValue:@(value) forKey:[NSString stringWithFormat:@"landBest.%d", land]];
}

- (jfloat)loadSpeed {
  NSNumber *number = [self numberForKey:@"speed"];
  float value = number ? number.floatValue : 1.f;
  return isfinite(value) ? value : 1.f;
}
- (void)saveSpeedWithFloat:(jfloat)speed { [self setValue:@(speed) forKey:@"speed"]; }
- (jint)loadBgm { return [self intForKey:@"bgm" defaultValue:0]; }
- (void)saveBgmWithInt:(jint)choice { [self setValue:@(choice) forKey:@"bgm"]; }

- (jlong)loadCollected {
  NSNumber *value = [self numberForKey:@"collected"];
  return value ? value.longLongValue : 0;
}
- (void)saveCollectedWithLong:(jlong)owned { [self setValue:@(owned) forKey:@"collected"]; }

- (IOSIntArray *)loadCollectionCounts {
  IOSIntArray *out = [IOSIntArray arrayWithLength:DDCollect_COUNT];
  [_lock lock];
  NSArray *saved = [_values[@"collectionCounts"] isKindOfClass:NSArray.class] ? _values[@"collectionCounts"] : nil;
  for (NSUInteger i = 0; i < MIN(saved.count, (NSUInteger)DDCollect_COUNT); ++i) {
    id item = saved[i];
    if ([item isKindOfClass:NSNumber.class]) out->buffer_[i] = MAX(0, [(NSNumber *)item intValue]);
  }
  [_lock unlock];
  return out;
}

- (void)saveCollectionCountsWithIntArray:(IOSIntArray *)counts {
  NSMutableArray *saved = [NSMutableArray arrayWithCapacity:counts ? counts->size_ : 0];
  for (jint i = 0; counts && i < counts->size_; ++i) [saved addObject:@(MAX(0, counts->buffer_[i]))];
  [self setValue:saved forKey:@"collectionCounts"];
}

- (jint)loadCollectTotal { return MAX(0, [self intForKey:@"collectTotal" defaultValue:0]); }
- (void)saveCollectTotalWithInt:(jint)total { [self setValue:@(total) forKey:@"collectTotal"]; }
- (jint)loadSteamerOpens { return MAX(0, [self intForKey:@"steamerOpens" defaultValue:0]); }
- (void)saveSteamerOpensWithInt:(jint)opens { [self setValue:@(opens) forKey:@"steamerOpens"]; }
- (jint)loadStarWins { return MAX(0, [self intForKey:@"starWins" defaultValue:0]); }
- (void)saveStarWinsWithInt:(jint)wins { [self setValue:@(wins) forKey:@"starWins"]; }

- (jint)loadRosterState {
  NSNumber *saved = [self numberForKey:@"roster"];
  if (saved) return saved.intValue;
  // Matches Android's upgrade path: a legacy save starts with the established deck.
  return _values[@"best"] || _values[@"collected"] || _values[@"collectTotal"] ? 1 : 0;
}
- (void)saveRosterStateWithInt:(jint)state { [self setValue:@(state) forKey:@"roster"]; }

@end
