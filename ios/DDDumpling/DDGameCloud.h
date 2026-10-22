#import <Foundation/Foundation.h>
@class DDIOSGame, DDIOSStore, DDGameCenter;
@interface DDGameCloud : NSObject
- (NSString *)status;
- (NSInteger)uiStatus;
- (instancetype)initWithGame:(DDIOSGame *)game store:(DDIOSStore *)store center:(DDGameCenter *)center;
- (void)updateActive:(BOOL)active elapsed:(double)elapsed;
@end
