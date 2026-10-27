#import <UIKit/UIKit.h>
@class DDIOSGame;
@interface DDAppReview : NSObject
- (void)configure:(DDIOSGame *)game;
- (void)request:(DDIOSGame *)game view:(UIView *)view preview:(BOOL)preview;
@end
