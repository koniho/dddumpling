#import "DDGameCloud.h"
#import "DDGameCenter.h"
#import "DDStore.h"
#import "com/dddumpling/game/BuildFlags.h"
#if DEBUG
#import <GameKit/GameKit.h>
#import <CommonCrypto/CommonDigest.h>
#import "com/dddumpling/game/IOSCloud.h"
#import "IOSPrimitiveArray.h"
#import "IOSObjectArray.h"
#import "IOSClass.h"

@interface DDCloudHost : NSObject <DDIOSCloud_Host, GKLocalPlayerListener>
@property(nonatomic, weak) DDGameCloud *owner;
@end

@interface DDGameCloud ()
@property(nonatomic, strong) DDIOSCloud *cloud;
@property(nonatomic, strong) DDIOSStore *store;
@property(nonatomic, strong) DDCloudHost *host;
@property(nonatomic, strong) id identityObserver;
@property(nonatomic, weak) DDGameCenter *center;
@property(nonatomic, strong) id identity;
@property(nonatomic, strong) NSData *identityData;
@property(nonatomic, copy) NSString *playerID, *saveName;
@property(nonatomic, copy) NSString *availability, *lastError;
@property(nonatomic, strong) NSArray<GKSavedGame *> *records;
@property(nonatomic) BOOL active, registered;
@property(nonatomic) jint request;
- (void)fetchWithInt:(jint)token;
- (void)saveWithByteArray:(IOSByteArray *)data withInt:(jint)token;
- (void)invalidateIdentity;
- (void)conflictsChanged;
@end
@implementation DDCloudHost
- (void)fetchWithInt:(jint)token { [self.owner fetchWithInt:token]; }
- (void)saveWithByteArray:(IOSByteArray *)data withInt:(jint)token { [self.owner saveWithByteArray:data withInt:token]; }
- (void)player:(GKPlayer *)player hasConflictingSavedGames:(NSArray<GKSavedGame *> *)games {
    dispatch_async(dispatch_get_main_queue(), ^{ [self.owner conflictsChanged]; });
}
- (void)player:(GKPlayer *)player didModifySavedGame:(GKSavedGame *)game {
    // Our own writes also notify; fetch remote changes on the periodic cadence.
}
@end
#endif

@implementation DDGameCloud
- (NSString *)status {
#if DEBUG
    NSString *status = self.availability ?: (self.cloud ? [self.cloud status] : @"Disabled");
    return self.lastError ? [NSString stringWithFormat:@"%@\n%@", status, self.lastError] : status;
#else
    return @"Disabled";
#endif
}
- (instancetype)initWithGame:(DDIOSGame *)game store:(DDIOSStore *)store center:(DDGameCenter *)center {
    if ((self = [super init])) {
#if DEBUG
        if (DDBuildFlags_DEVELOPER) {
            _store = store; _center = center;
            DDCloudHost *host = [DDCloudHost new]; host.owner = self;
            _host = host;
            _cloud = [[DDIOSCloud alloc] initWithDDIOSGame:game withDDIOSCloud_Host:host];
            __weak DDGameCloud *weakSelf = self;
            _identityObserver = [NSNotificationCenter.defaultCenter
                addObserverForName:NSUbiquityIdentityDidChangeNotification object:nil
                queue:NSOperationQueue.mainQueue usingBlock:^(NSNotification *note) {
                    [weakSelf invalidateIdentity];
                }];
        }
#endif
    }
    return self;
}
- (void)updateActive:(BOOL)active elapsed:(double)elapsed {
#if DEBUG
    if (!self.cloud) return;
    self.active = active;
    id identity = NSFileManager.defaultManager.ubiquityIdentityToken;
    NSString *player = self.center.playerID;
    if (identity != self.identity && ![identity isEqual:self.identity]) {
        self.identity = identity;
        self.identityData = identity ? [NSKeyedArchiver archivedDataWithRootObject:identity
            requiringSecureCoding:NO error:nil] : nil;
        // Even a transient iCloud sign-out invalidates callbacks from the previous session.
        [self.cloud sessionWithNSString:nil withBoolean:NO];
        self.records = nil;
    }
    BOOL allowed = player.length && identity && !self.store.error
        && GKLocalPlayer.localPlayer.isAuthenticated
        && [GKLocalPlayer.localPlayer.gamePlayerID isEqual:player];
    if (allowed && (![player isEqual:self.playerID] || !self.saveName)) {
        self.playerID = player;
        NSData *bytes = [player dataUsingEncoding:NSUTF8StringEncoding];
        unsigned char hash[CC_SHA256_DIGEST_LENGTH];
        CC_SHA256(bytes.bytes, (CC_LONG)bytes.length, hash);
        NSMutableString *name = [@"dddumpling-progress-v1-" mutableCopy];
        for (NSUInteger i = 0; i < sizeof(hash); i++) [name appendFormat:@"%02x", hash[i]];
        self.saveName = name;
    }
    BOOL bound = allowed && [self.store bindCloudPlayer:player identity:self.identityData];
    self.availability = !player.length ? @"Waiting for Game Center sign-in"
        : !identity ? @"iCloud Drive unavailable"
        : self.store.error ? self.store.error
        : !allowed ? @"Game Center account changed"
        : !bound ? @"Account differs from this local save; sync paused" : nil;
    allowed = bound;
    if (allowed && !self.registered) {
        [GKLocalPlayer.localPlayer registerListener:self.host]; self.registered = YES;
    }
    [self.cloud sessionWithNSString:allowed ? player : nil withBoolean:active];
    [self.cloud updateWithDouble:elapsed];
#endif
}
#if DEBUG
- (void)dealloc {
    if (self.registered) [GKLocalPlayer.localPlayer unregisterListener:self.host];
    if (self.identityObserver) [NSNotificationCenter.defaultCenter removeObserver:self.identityObserver];
    [self.cloud sessionWithNSString:nil withBoolean:NO];
}
- (void)invalidateIdentity {
    self.identity = nil; self.identityData = nil; self.records = nil;
    [self.cloud sessionWithNSString:nil withBoolean:NO];
}
- (BOOL)valid:(jint)request {
    if (request != self.request) return NO;
    BOOL valid = self.center.playerID.length
        && [self.center.playerID isEqual:self.playerID] && !self.store.error
        && GKLocalPlayer.localPlayer.isAuthenticated
        && [GKLocalPlayer.localPlayer.gamePlayerID isEqual:self.playerID]
        && [NSFileManager.defaultManager.ubiquityIdentityToken isEqual:self.identity];
    if (!valid) [self.cloud sessionWithNSString:nil withBoolean:self.active];
    return valid;
}
- (void)fetchWithInt:(jint)token {
    self.request = token;
    if (![self valid:token]) return;
    self.lastError = nil;
    __weak DDGameCloud *weakSelf = self;
    [GKLocalPlayer.localPlayer fetchSavedGamesWithCompletionHandler:^(NSArray<GKSavedGame *> *games, NSError *error) {
        dispatch_async(dispatch_get_main_queue(), ^{
            DDGameCloud *sync = weakSelf;
            if (!sync || ![sync valid:token]) return;
            if (error) { [sync failed:token error:error]; return; }
            NSMutableArray *records = [NSMutableArray new];
            for (GKSavedGame *game in games) if ([game.name isEqual:sync.saveName]) [records addObject:game];
            if (records.count > 32) { [sync.cloud failedWithInt:token withBoolean:YES]; return; }
            sync.records = records;
            [sync loadRecords:records index:0 payloads:[NSMutableArray new] token:token];
        });
    }];
}
- (void)loadRecords:(NSArray<GKSavedGame *> *)records index:(NSUInteger)index
           payloads:(NSMutableArray<NSData *> *)payloads token:(jint)token {
    if (![self valid:token]) return;
    if (index == records.count) {
        IOSObjectArray *saves = [IOSObjectArray arrayWithLength:payloads.count type:IOSClass_byteArray(1)];
        for (NSUInteger i = 0; i < payloads.count; i++) {
            NSData *data = payloads[i];
            IOSByteArray *bytes = [IOSByteArray arrayWithBytes:data.bytes count:data.length];
            IOSObjectArray_Set(saves, i, bytes);
        }
        [self.cloud fetchedWithByteArray2:saves withInt:token];
        return;
    }
    __weak DDGameCloud *weakSelf = self;
    [records[index] loadDataWithCompletionHandler:^(NSData *data, NSError *error) {
        dispatch_async(dispatch_get_main_queue(), ^{
            DDGameCloud *sync = weakSelf;
            if (!sync || ![sync valid:token]) return;
            if (error || !data) { [sync failed:token error:error]; return; }
            if (data.length > 512 * 1024) { [sync.cloud failedWithInt:token withBoolean:YES]; return; }
            [payloads addObject:data];
            [sync loadRecords:records index:index + 1 payloads:payloads token:token];
        });
    }];
}
- (void)saveWithByteArray:(IOSByteArray *)data withInt:(jint)token {
    if (![self valid:token]) return;
    NSData *payload = [NSData dataWithBytes:data->buffer_ length:data->size_];
    __weak DDGameCloud *weakSelf = self;
    void (^finished)(NSError *) = ^(NSError *error) {
        dispatch_async(dispatch_get_main_queue(), ^{
            DDGameCloud *sync = weakSelf;
            if (!sync || ![sync valid:token]) return;
            sync.records = nil;
            if (error) [sync failed:token error:error];
            else [sync.cloud savedWithInt:token];
        });
    };
    if (self.records.count > 1) {
        [GKLocalPlayer.localPlayer resolveConflictingSavedGames:self.records withData:payload
            completionHandler:^(NSArray<GKSavedGame *> *games, NSError *error) { finished(error); }];
    } else {
        [GKLocalPlayer.localPlayer saveGameData:payload withName:self.saveName
            completionHandler:^(GKSavedGame *game, NSError *error) { finished(error); }];
    }
}
- (void)conflictsChanged { [self.cloud changed]; }
- (void)failed:(jint)token error:(NSError *)error {
    self.lastError = error ? [NSString stringWithFormat:@"%@ (%ld): %@",
        error.domain, (long)error.code, error.localizedDescription] : @"Cloud data unavailable";
    NSLog(@"Game Center cloud: %@", self.lastError);
    [self.cloud failedWithInt:token withBoolean:NO];
}
#endif
@end
