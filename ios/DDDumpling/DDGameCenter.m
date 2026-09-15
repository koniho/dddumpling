#import "DDGameCenter.h"
#import "com/dddumpling/game/BuildFlags.h"
#if DEBUG
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
@property(nonatomic, readwrite) NSString *playerID;
@property(nonatomic) BOOL enabled, started, active, presenting;
@end

@implementation DDGameCenter
- (instancetype)initWithPresenter:(UIViewController *)presenter {
    id<DDGameCenterPlayer> player = nil;
    BOOL enabled = DDBuildFlags_DEVELOPER;
#if DEBUG
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
        _enabled = enabled && DDBuildFlags_DEVELOPER;
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
            service.pending = controller == service.presented ? nil : controller;
            service.status = controller ? @"Sign in" : player.length ? @"Connected" : @"Playing locally";
            if (error) service.status = @"Playing locally";
            [service presentIfReady];
        }];
    }
    [self presentIfReady];
}
- (void)presentIfReady {
    UIViewController *host = self.presenter;
    if (!self.active || !self.pending || self.presented || !host.view.window
            || host.presentedViewController || host.isBeingDismissed || host.isBeingPresented) return;
    self.presented = self.pending;
    self.pending = nil;
    self.presenting = YES;
    if (self.presentationChanged) self.presentationChanged(YES);
    __weak DDGameCenter *weakSelf = self;
    [host presentViewController:self.presented animated:YES completion:^{ weakSelf.presenting = NO; }];
}
@end
