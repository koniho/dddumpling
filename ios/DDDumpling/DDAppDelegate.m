#import "DDAppDelegate.h"
#import "DDGameView.h"
#import <UserNotifications/UserNotifications.h>
#if DEBUG
#import "DDRenderCheck.h"
#endif

@interface DDViewController : UIViewController
@end
@implementation DDViewController
- (void)loadView { self.view = [[DDGameView alloc] initWithFrame:UIScreen.mainScreen.bounds]; }
- (BOOL)prefersStatusBarHidden { return YES; }
- (UIInterfaceOrientationMask)supportedInterfaceOrientations { return UIInterfaceOrientationMaskPortrait; }
@end

@interface DDAppDelegate () <UNUserNotificationCenterDelegate>
@property(nonatomic, copy) NSDictionary *leaderboardTap;
@end

@implementation DDAppDelegate
- (BOOL)application:(UIApplication *)application didFinishLaunchingWithOptions:(NSDictionary *)options {
    UNUserNotificationCenter.currentNotificationCenter.delegate = self;
    self.window = [[UIWindow alloc] initWithFrame:UIScreen.mainScreen.bounds];
    self.window.rootViewController = [DDViewController new];
    [self.window makeKeyAndVisible];
    application.idleTimerDisabled = YES;
#if DEBUG
    if ([NSProcessInfo.processInfo.environment[@"DDD_RENDER_CHECK"] isEqualToString:@"1"]) {
        DDRunRenderChecks();
    }
#endif
    return YES;
}
- (NSDictionary *)takeLeaderboardTap {
    NSDictionary *tap = self.leaderboardTap; self.leaderboardTap = nil; return tap;
}
- (void)notifyLeaderboard:(NSInteger)board rank:(long long)rank title:(NSString *)title owner:(NSString *)owner
                 preview:(BOOL)preview valid:(BOOL (^)(void))valid completion:(void (^)(BOOL))completion {
    UNUserNotificationCenter *center = UNUserNotificationCenter.currentNotificationCenter;
    [center getNotificationSettingsWithCompletionHandler:^(UNNotificationSettings *settings) {
        if (settings.authorizationStatus == UNAuthorizationStatusNotDetermined) {
            dispatch_async(dispatch_get_main_queue(), ^{
                if (!valid()) { completion(NO); return; }
                [center requestAuthorizationWithOptions:UNAuthorizationOptionAlert | UNAuthorizationOptionSound
                    completionHandler:^(BOOL granted, NSError *error) {
                    if (granted && !error) [self notifyLeaderboard:board rank:rank title:title owner:owner preview:preview valid:valid completion:completion];
                    else dispatch_async(dispatch_get_main_queue(), ^{ completion(NO); });
                }];
            });
            return;
        }
        if ((settings.authorizationStatus != UNAuthorizationStatusAuthorized
                && settings.authorizationStatus != UNAuthorizationStatusProvisional)
                || settings.alertSetting != UNNotificationSettingEnabled) {
            dispatch_async(dispatch_get_main_queue(), ^{ completion(NO); }); return;
        }
        dispatch_async(dispatch_get_main_queue(), ^{
            if (!valid()) { completion(NO); return; }
            UNMutableNotificationContent *content = [UNMutableNotificationContent new];
            content.title = preview ? @"Leaderboard notification preview" : @"You're on the leaderboard!";
            content.body = [NSString stringWithFormat:@"%@#%lld global · %@", preview ? @"Sample: " : @"", rank, title];
            content.sound = UNNotificationSound.defaultSound;
            content.userInfo = @{@"leaderboard_board":@(board), @"leaderboard_owner":owner};
            UNNotificationRequest *request = [UNNotificationRequest requestWithIdentifier:
                (preview ? @"leaderboard.preview" : [NSString stringWithFormat:@"leaderboard.%ld", (long)board]) content:content trigger:nil];
            [center addNotificationRequest:request withCompletionHandler:^(NSError *error) {
                dispatch_async(dispatch_get_main_queue(), ^{ completion(error == nil); });
            }];
        });
    }];
}
- (void)userNotificationCenter:(UNUserNotificationCenter *)center willPresentNotification:(UNNotification *)notification
        withCompletionHandler:(void (^)(UNNotificationPresentationOptions))completionHandler {
    BOOL leaderboard = [notification.request.identifier hasPrefix:@"leaderboard."];
    completionHandler(leaderboard ? UNNotificationPresentationOptionBanner | UNNotificationPresentationOptionList
        | UNNotificationPresentationOptionSound : UNNotificationPresentationOptionNone);
}
- (void)userNotificationCenter:(UNUserNotificationCenter *)center didReceiveNotificationResponse:(UNNotificationResponse *)response
        withCompletionHandler:(void (^)(void))completionHandler {
    NSDictionary *info = response.notification.request.content.userInfo;
    dispatch_async(dispatch_get_main_queue(), ^{
        if ([response.actionIdentifier isEqual:UNNotificationDefaultActionIdentifier]
                && [response.notification.request.identifier hasPrefix:@"leaderboard."]
                && [info[@"leaderboard_board"] isKindOfClass:NSNumber.class]
                && [info[@"leaderboard_owner"] isKindOfClass:NSString.class]) self.leaderboardTap = info;
        completionHandler();
    });
}
- (void)applicationWillResignActive:(UIApplication *)application {
    [(DDGameView *)self.window.rootViewController.view setActive:NO];
}
- (void)applicationDidBecomeActive:(UIApplication *)application {
    [(DDGameView *)self.window.rootViewController.view setActive:YES];
}
@end
