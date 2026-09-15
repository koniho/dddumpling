#import <UIKit/UIKit.h>

typedef void (^DDGameCenterAuthentication)(UIViewController *, NSString *, NSError *);

@protocol DDGameCenterPlayer <NSObject>
- (void)authenticate:(DDGameCenterAuthentication)callback;
@end

@interface DDGameCenter : NSObject
@property(nonatomic, readonly) NSString *status;
@property(nonatomic, readonly) NSString *playerID;
@property(nonatomic, copy) void (^presentationChanged)(BOOL visible);
- (instancetype)initWithPresenter:(UIViewController *)presenter;
- (instancetype)initWithPresenter:(UIViewController *)presenter
                           player:(id<DDGameCenterPlayer>)player enabled:(BOOL)enabled;
- (void)refreshActive:(BOOL)active;
@end
