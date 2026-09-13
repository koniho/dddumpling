#import <UIKit/UIKit.h>
#import "com/dddumpling/game/Analytics.h"

@protocol DDAnalyticsBackend <NSObject>
- (void)setCollectionEnabled:(BOOL)enabled;
- (void)recordEvent:(NSString *)name amount:(NSInteger)amount;
@end

@interface DDGameAnalytics : NSObject <DDAnalytics_Sink>
@property(nonatomic, readonly) BOOL available;
@property(nonatomic, readonly) BOOL enabled;
- (instancetype)initWithDefaults:(NSUserDefaults *)defaults backend:(id<DDAnalyticsBackend>)backend;
- (void)setConsent:(BOOL)allowed;
- (BOOL)shouldOfferConsent;
- (void)openPolicy;
@end
