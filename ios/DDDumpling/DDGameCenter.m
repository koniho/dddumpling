#import "DDGameCenter.h"
#import "DDGameServices.h"
#import "DDStore.h"
#import "com/dddumpling/game/IOSGame.h"
#if DDDUMPLING_GAME_CENTER
#import <GameKit/GameKit.h>

@interface DDGameKitPlayer : NSObject <DDGameCenterPlayer>
@end
@implementation DDGameKitPlayer
- (void)authenticate:(DDGameCenterAuthentication)callback {
    GKLocalPlayer.localPlayer.authenticateHandler = ^(UIViewController *controller, NSError *error) {
        NSString *player = GKLocalPlayer.localPlayer.isAuthenticated
            ? GKLocalPlayer.localPlayer.gamePlayerID : nil;
        dispatch_async(dispatch_get_main_queue(), ^{ callback(controller, player, error); });
    };
}
@end
#endif

@interface DDGameCenter ()
@property(nonatomic, weak) UIViewController *presenter;
@property(nonatomic, strong) id<DDGameCenterPlayer> player;
@property(nonatomic, strong) UIViewController *pending;
@property(nonatomic, strong) UIViewController *presented;
@property(nonatomic, readwrite) NSString *status;
@property(nonatomic, readwrite) NSString *lastError;
@property(nonatomic, readwrite) NSString *playerID;
@property(nonatomic) BOOL enabled, started, active, presenting, promptRequested;
@property(nonatomic) double achievementClock;
@property(nonatomic) BOOL achievementBusy;
@property(nonatomic, copy) NSString *achievementOwner;
@property(nonatomic, strong) NSMutableDictionary *achievementSent;
@end

@implementation DDGameCenter
- (instancetype)initWithPresenter:(UIViewController *)presenter {
    id<DDGameCenterPlayer> player = nil;
    BOOL enabled = DDDUMPLING_GAME_CENTER;
#if DDDUMPLING_GAME_CENTER
    NSDictionary *environment = NSProcessInfo.processInfo.environment;
    enabled = enabled && ![environment[@"DDD_GAME_CENTER_DISABLED"] boolValue]
        && !environment[@"XCTestConfigurationFilePath"];
    if (enabled) player = [DDGameKitPlayer new];
#endif
    return [self initWithPresenter:presenter player:player enabled:enabled];
}
- (instancetype)initWithPresenter:(UIViewController *)presenter
                           player:(id<DDGameCenterPlayer>)player enabled:(BOOL)enabled {
    if ((self = [super init])) {
        _presenter = presenter;
        _enabled = enabled && DDDUMPLING_GAME_CENTER;
        _player = _enabled ? player : nil;
        _status = _enabled ? @"Waiting" : @"Disabled";
    }
    return self;
}
- (void)refreshActive:(BOOL)active {
    if (!self.enabled) return;
    self.active = active;
    if (self.presented && !self.presenting && !self.presented.presentingViewController
            && !self.presented.isBeingDismissed) {
        self.presented = nil;
        self.promptRequested = NO;
        if (self.presentationChanged) self.presentationChanged(NO);
    }
    if (!active) return;
    if (!self.started && self.player) {
        self.started = YES;
        self.status = @"Connecting";
        __weak DDGameCenter *weakSelf = self;
        [self.player authenticate:^(UIViewController *controller, NSString *player, NSError *error) {
            DDGameCenter *service = weakSelf;
            if (!service) return;
            service.playerID = error ? nil : player;
            service.lastError = error ? [NSString stringWithFormat:@"%@ (%ld): %@",
                error.domain, (long)error.code, error.localizedDescription] : nil;
            service.pending = controller == service.presented ? nil : controller;
            service.status = controller ? @"Sign in" : player.length ? @"Connected" : @"Playing locally";
            if (error) service.status = @"Playing locally";
            NSLog(@"Game Center: %@%@", service.status,
                service.lastError ? [@" — " stringByAppendingString:service.lastError] : @"");
            [service presentIfReady];
        }];
    }
    [self presentIfReady];
}
- (void)requestSignIn {
    self.promptRequested = YES;
    if (!self.playerID.length && !self.presented && !self.pending) self.started = NO;
    [self refreshActive:YES];
}
- (void)updateAchievements:(DDIOSGame *)game store:(DDIOSStore *)store elapsed:(double)elapsed {
    [game achievementsEnabledWithBoolean:self.enabled];
#if DDDUMPLING_GAME_CENTER
    NSString *owner = self.playerID;
    if (!self.enabled || !self.active || !owner.length || store.error
            || !GKLocalPlayer.localPlayer.isAuthenticated
            || ![GKLocalPlayer.localPlayer.gamePlayerID isEqual:owner]) return;
    if (![self.achievementOwner isEqual:owner]) {
        self.achievementOwner = owner; self.achievementSent = [NSMutableDictionary new];
        self.achievementClock = 10; self.achievementBusy = NO;
    }
    self.achievementClock += MAX(0, MIN(elapsed, 1));
    if (self.achievementBusy || self.achievementClock < 10) return;
    self.achievementClock = 0;
    if (![store bindAchievementPlayer:owner]) return;
    NSMutableArray *reports = [NSMutableArray new];
    for (int i = 0; i < [game achievementCount]; i++) {
        NSString *identifier = [game achievementIDWithInt:i];
        if ([NSBundle.mainBundle.bundleIdentifier hasSuffix:@".dev"])
            identifier = [identifier stringByReplacingOccurrencesOfString:@"com.dddumpling.game."
                withString:@"com.dddumpling.game.dev."];
        double percent = [game achievementPercentWithInt:i];
        if (percent <= [self.achievementSent[identifier] doubleValue]) continue;
        GKAchievement *report = [[GKAchievement alloc] initWithIdentifier:identifier];
        report.percentComplete = percent; report.showsCompletionBanner = YES;
        [reports addObject:report];
    }
    if (!reports.count) return;
    self.achievementBusy = YES;
    NSMutableDictionary *sent = self.achievementSent;
    __weak DDGameCenter *weakSelf = self;
    [GKAchievement reportAchievements:reports withCompletionHandler:^(NSError *error) {
        dispatch_async(dispatch_get_main_queue(), ^{
            DDGameCenter *service = weakSelf;
            if (service.achievementSent != sent) return;
            service.achievementBusy = NO;
            if (!error) for (GKAchievement *report in reports)
                sent[report.identifier] = @(report.percentComplete);
        });
    }];
#endif
}
- (void)showAchievements {
#if DDDUMPLING_GAME_CENTER
    if (!self.playerID.length) { [self requestSignIn]; return; }
    UIViewController *host = self.presenter;
    if (!host.view.window || host.presentedViewController) return;
    GKGameCenterViewController *controller = [[GKGameCenterViewController alloc]
        initWithState:GKGameCenterViewControllerStateAchievements];
    controller.gameCenterDelegate = (id<GKGameCenterControllerDelegate>)self;
    if (self.presentationChanged) self.presentationChanged(YES);
    [host presentViewController:controller animated:YES completion:nil];
#endif
}
#if DDDUMPLING_GAME_CENTER
- (void)gameCenterViewControllerDidFinish:(GKGameCenterViewController *)controller {
    [controller dismissViewControllerAnimated:YES completion:^{
        if (self.presentationChanged) self.presentationChanged(NO);
    }];
}
#endif
- (void)presentIfReady {
    UIViewController *host = self.presenter;
    if (!self.promptRequested || !self.active || !self.pending || self.presented || !host.view.window
            || host.presentedViewController || host.isBeingDismissed || host.isBeingPresented) return;
    self.presented = self.pending;
    self.pending = nil;
    self.presenting = YES;
    if (self.presentationChanged) self.presentationChanged(YES);
    __weak DDGameCenter *weakSelf = self;
    [host presentViewController:self.presented animated:YES completion:^{ weakSelf.presenting = NO; }];
}
@end
