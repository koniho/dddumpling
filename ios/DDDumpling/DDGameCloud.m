#import "DDGameCloud.h"
#import "DDGameCenter.h"
#import "DDStore.h"
#import "DDGameServices.h"
#if DDDUMPLING_GAME_CENTER
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
#if DEBUG
@property(nonatomic, copy) NSString *probeMode;
@property(nonatomic) BOOL probeStarted, probeBusy;
@property(nonatomic) double probeElapsed;
#endif
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
#if DDDUMPLING_GAME_CENTER
    NSString *status = self.availability ?: (self.cloud ? [self.cloud status] : @"Disabled");
    return self.lastError ? [NSString stringWithFormat:@"%@\n%@", status, self.lastError] : status;
#else
    return @"Disabled";
#endif
}
- (NSInteger)uiStatus {
#if DDDUMPLING_GAME_CENTER
    // CloudSaveUi states; a GameKit save callback does not prove server upload.
    if ([self.center.status isEqualToString:@"Disabled"]) return 0;
    if (!self.center.playerID.length) return 1;
    if (!self.identity) return 2;
    if (self.lastError || self.store.error) return 5;
    if ([[self.cloud status] hasPrefix:@"Cloud save unreadable"]) return 5;
    if (self.availability) return 6;
    return [[self.cloud status] isEqualToString:@"Synced"] ? 4 : 3;
#else
    return 0;
#endif
}
- (instancetype)initWithGame:(DDIOSGame *)game store:(DDIOSStore *)store center:(DDGameCenter *)center {
    if ((self = [super init])) {
#if DDDUMPLING_GAME_CENTER
        {
#if DEBUG
            NSString *mode = NSProcessInfo.processInfo.environment[@"DDD_GAMEKIT_PROBE"];
            if ([mode isEqualToString:@"write"] || [mode isEqualToString:@"read"]) {
                _probeMode = mode;
                NSLog(@"GameKit probe: %@ mode; normal cloud coordinator bypassed", mode);
                return self;
            }
#endif
            _store = store; _center = center;
            DDCloudHost *host = [DDCloudHost new]; host.owner = self;
            _host = host;
            _cloud = [[DDIOSCloud alloc] initWithDDIOSGame:game withDDIOSCloud_Host:host withBoolean:YES];
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
#if DDDUMPLING_GAME_CENTER
#if DEBUG
    if (self.probeMode) { [self updateProbe:active elapsed:elapsed]; return; }
#endif
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
#if DDDUMPLING_GAME_CENTER
#if DEBUG
- (void)updateProbe:(BOOL)active elapsed:(double)elapsed {
    if (!active || !GKLocalPlayer.localPlayer.isAuthenticated) return;
    // Separate fixed slot; no progress serialization, account binding or merge logic.
    NSString *name = @"ddd-direct-gamekit-probe-20261021";
    if (!self.probeStarted) {
        self.probeElapsed += MAX(0, MIN(elapsed, 1));
        if (self.probeElapsed < 15) return;
        self.probeStarted = YES;
        NSLog(@"GameKit probe: authenticated; starting %@", self.probeMode);
        if ([self.probeMode isEqualToString:@"write"]) {
            NSString *marker = [@"direct-gamekit-" stringByAppendingString:NSUUID.UUID.UUIDString];
            NSLog(@"GameKit probe: writing marker %@", marker);
            self.probeBusy = YES;
            [GKLocalPlayer.localPlayer saveGameData:[marker dataUsingEncoding:NSUTF8StringEncoding]
                withName:name completionHandler:^(GKSavedGame *game, NSError *error) {
                dispatch_async(dispatch_get_main_queue(), ^{
                    self.probeBusy = NO;
                    NSLog(@"GameKit probe: save callback name=%@ error=%@", game.name, error);
                });
            }];
        }
        self.probeElapsed = 15;
    }
    self.probeElapsed += MAX(0, MIN(elapsed, 1));
    if (self.probeBusy || self.probeElapsed < 15) return;
    self.probeElapsed = 0; self.probeBusy = YES;
    [GKLocalPlayer.localPlayer fetchSavedGamesWithCompletionHandler:^(NSArray<GKSavedGame *> *games, NSError *error) {
        NSUInteger matches = 0;
        for (GKSavedGame *game in games) {
            if (![game.name isEqualToString:name]) continue;
            matches++;
            [game loadDataWithCompletionHandler:^(NSData *data, NSError *loadError) {
                NSLog(@"GameKit probe: loaded marker=%@ error=%@",
                    data ? [[NSString alloc] initWithData:data encoding:NSUTF8StringEncoding] : nil, loadError);
            }];
        }
        NSLog(@"GameKit probe: fetched total=%lu matching=%lu error=%@",
            (unsigned long)games.count, (unsigned long)matches, error);
        dispatch_async(dispatch_get_main_queue(), ^{ self.probeBusy = NO; });
    }];
}
#endif
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
#if DEBUG
            NSLog(@"Cloud test: fetched %lu matching records", (unsigned long)records.count);
#endif
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
            else {
                [sync.cloud savedWithInt:token];
#if DEBUG
                NSLog(@"Cloud test: GameKit accepted save (upload unverified)");
                NSString *saveName = [sync.saveName copy];
                dispatch_after(dispatch_time(DISPATCH_TIME_NOW, 30 * NSEC_PER_SEC),
                    dispatch_get_global_queue(QOS_CLASS_UTILITY, 0), ^{
                    NSURL *root = [NSFileManager.defaultManager URLForUbiquityContainerIdentifier:nil];
                    NSURL *url = [root URLByAppendingPathComponent:[saveName stringByAppendingString:@".bundle"]];
                    NSError *error = nil;
                    NSDictionary *values = [url resourceValuesForKeys:@[NSURLUbiquitousItemIsUploadedKey,
                        NSURLUbiquitousItemIsUploadingKey, NSURLUbiquitousItemUploadingErrorKey,
                        NSURLUbiquitousItemDownloadingErrorKey] error:&error];
                    NSLog(@"Cloud test: package upload state %@ error %@", values, error);
                });
#endif
            }
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
