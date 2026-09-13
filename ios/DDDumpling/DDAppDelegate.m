#import "DDAppDelegate.h"
#import "DDGameView.h"
#if DEBUG
#import "DDRenderCheck.h"
#endif

@interface DDViewController : UIViewController
@end
@implementation DDViewController
- (void)loadView { self.view = [[DDGameView alloc] initWithFrame:CGRectZero]; }
- (BOOL)prefersStatusBarHidden { return YES; }
- (UIInterfaceOrientationMask)supportedInterfaceOrientations {
    return self.traitCollection.userInterfaceIdiom == UIUserInterfaceIdiomPad
        ? UIInterfaceOrientationMaskAll : UIInterfaceOrientationMaskPortrait;
}
@end

@implementation DDAppDelegate
- (BOOL)application:(UIApplication *)application didFinishLaunchingWithOptions:(NSDictionary *)options {
    application.idleTimerDisabled = YES;
#if DEBUG
    if ([NSProcessInfo.processInfo.environment[@"DDD_RENDER_CHECK"] isEqualToString:@"1"]) {
        DDRunRenderChecks();
    }
#endif
    return YES;
}
@end

@interface DDSceneDelegate : UIResponder <UIWindowSceneDelegate>
@property(nonatomic, strong) UIWindow *window;
@end

@implementation DDSceneDelegate
- (void)scene:(UIScene *)scene willConnectToSession:(UISceneSession *)session
        options:(UISceneConnectionOptions *)connectionOptions {
    if (![scene isKindOfClass:UIWindowScene.class]) return;
    self.window = [[UIWindow alloc] initWithWindowScene:(UIWindowScene *)scene];
    self.window.rootViewController = [DDViewController new];
    [self.window makeKeyAndVisible];
}
- (void)sceneWillResignActive:(UIScene *)scene {
    [(DDGameView *)self.window.rootViewController.view setActive:NO];
}
- (void)sceneDidBecomeActive:(UIScene *)scene {
    [(DDGameView *)self.window.rootViewController.view setActive:YES];
}
- (void)sceneDidDisconnect:(UIScene *)scene {
    [(DDGameView *)self.window.rootViewController.view setActive:NO];
}
@end
