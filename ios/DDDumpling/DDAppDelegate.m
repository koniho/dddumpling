#import "DDAppDelegate.h"
#import "DDGameView.h"
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

@implementation DDAppDelegate
- (BOOL)application:(UIApplication *)application didFinishLaunchingWithOptions:(NSDictionary *)options {
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
- (void)applicationWillResignActive:(UIApplication *)application {
    [(DDGameView *)self.window.rootViewController.view setActive:NO];
}
- (void)applicationDidBecomeActive:(UIApplication *)application {
    [(DDGameView *)self.window.rootViewController.view setActive:YES];
}
@end
