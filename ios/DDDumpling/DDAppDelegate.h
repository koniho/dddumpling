#import <UIKit/UIKit.h>
@interface DDAppDelegate : UIResponder <UIApplicationDelegate>
@property(nonatomic, strong) UIWindow *window;
- (NSDictionary *)takeLeaderboardTap;
- (void)notifyLeaderboard:(NSInteger)board rank:(long long)rank title:(NSString *)title owner:(NSString *)owner
                 preview:(BOOL)preview valid:(BOOL (^)(void))valid completion:(void (^)(BOOL))completion;
@end
