#import "DDAppReview.h"
#import "com/dddumpling/game/IOSGame.h"
#import <StoreKit/StoreKit.h>

@implementation DDAppReview
- (void)configure:(DDIOSGame *)game {
    BOOL available=NO;
#if !DEBUG
    NSURL *receipt=NSBundle.mainBundle.appStoreReceiptURL;
    available=[receipt.lastPathComponent isEqualToString:@"receipt"]
            && [NSFileManager.defaultManager fileExistsAtPath:receipt.path];
#endif
    [game reviewAvailableWithBoolean:available];
}
- (void)nativeRequest:(DDIOSGame *)game view:(UIView *)view {
    UIWindowScene *scene=view.window.windowScene;
    if(!scene || scene.activationState!=UISceneActivationStateForegroundActive || ![game reviewSafe]
            || view.window.rootViewController.presentedViewController)return;
    [SKStoreReviewController requestReviewInScene:scene];
}
- (void)request:(DDIOSGame *)game view:(UIView *)view preview:(BOOL)preview {
#if DEBUG
    if(preview) {
        UIViewController *presenter=view.window.rootViewController;
        if(!presenter || presenter.presentedViewController)return;
        UIAlertController *alert=[UIAlertController alertControllerWithTitle:@"Review prompt preview"
                message:@"The store review sheet would be requested here. This preview does not submit a rating or change the review cooldown. TestFlight suppresses native review requests."
                preferredStyle:UIAlertControllerStyleAlert];
        [alert addAction:[UIAlertAction actionWithTitle:@"Close" style:UIAlertActionStyleCancel handler:nil]];
        [alert addAction:[UIAlertAction actionWithTitle:@"Try store flow" style:UIAlertActionStyleDefault
                handler:^(UIAlertAction *action) {
            dispatch_after(dispatch_time(DISPATCH_TIME_NOW,(int64_t)(.35*NSEC_PER_SEC)),dispatch_get_main_queue(),^{
                [self nativeRequest:game view:view];
            });
        }]];
        [presenter presentViewController:alert animated:YES completion:nil];
        return;
    }
#endif
    [self nativeRequest:game view:view];
}
@end
