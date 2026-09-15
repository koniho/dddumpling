#import <XCTest/XCTest.h>
#import "DDGameCenter.h"

@interface DDTestPlayer : NSObject <DDGameCenterPlayer>
@property(nonatomic) NSUInteger calls;
@property(nonatomic, copy) DDGameCenterAuthentication callback;
@end
@implementation DDTestPlayer
- (void)authenticate:(DDGameCenterAuthentication)callback { self.calls++; self.callback = callback; }
@end

@interface DDTestPresentation : UIViewController
@property(nonatomic, weak) UIViewController *host;
@end
@implementation DDTestPresentation
- (UIViewController *)presentingViewController { return self.host; }
@end

@interface DDTestPresenter : UIViewController
@property(nonatomic, strong) UIViewController *modal;
@property(nonatomic) NSUInteger presentations;
@end
@implementation DDTestPresenter
- (UIViewController *)presentedViewController { return self.modal; }
- (void)presentViewController:(UIViewController *)controller animated:(BOOL)animated completion:(void (^)(void))completion {
    self.presentations++; self.modal = controller;
    ((DDTestPresentation *)controller).host = self;
    if (completion) completion();
}
@end

@interface DDGameCenterTests : XCTestCase
@end
@implementation DDGameCenterTests
- (void)testDisabledNeverAuthenticates {
    DDTestPlayer *player = [DDTestPlayer new];
    DDGameCenter *service = [[DDGameCenter alloc] initWithPresenter:[UIViewController new] player:player enabled:NO];
    [service refreshActive:YES]; [service refreshActive:NO]; [service refreshActive:YES];
    XCTAssertEqual(player.calls, 0u);
    XCTAssertEqualObjects(service.status, @"Disabled");
}
- (void)testOfflineFailureAndAccountChangesDoNotRepeatAuthentication {
    DDTestPlayer *player = [DDTestPlayer new];
    DDGameCenter *service = [[DDGameCenter alloc] initWithPresenter:[UIViewController new] player:player enabled:YES];
    [service refreshActive:NO]; XCTAssertEqual(player.calls, 0u);
    [service refreshActive:YES]; XCTAssertEqual(player.calls, 1u);
    player.callback(nil, nil, [NSError errorWithDomain:@"test" code:1 userInfo:nil]);
    XCTAssertEqualObjects(service.status, @"Playing locally"); XCTAssertNil(service.playerID);
    [service refreshActive:NO]; [service refreshActive:YES]; XCTAssertEqual(player.calls, 1u);
    player.callback(nil, @"player-a", nil); XCTAssertEqualObjects(service.playerID, @"player-a");
    player.callback(nil, nil, nil); XCTAssertNil(service.playerID);
    player.callback(nil, @"player-b", nil); XCTAssertEqualObjects(service.playerID, @"player-b");
}
- (void)testPresentationWaitsForForegroundAndOtherModalsAndReleasesPause {
    UIWindow *window = [[UIWindow alloc] initWithFrame:CGRectMake(0, 0, 393, 852)];
    DDTestPresenter *host = [DDTestPresenter new]; window.rootViewController = host;
    [window addSubview:host.view];
    DDTestPlayer *player = [DDTestPlayer new];
    DDGameCenter *service = [[DDGameCenter alloc] initWithPresenter:host player:player enabled:YES];
    NSMutableArray *visibility = [NSMutableArray new];
    service.presentationChanged = ^(BOOL visible) { [visibility addObject:@(visible)]; };
    [service refreshActive:YES]; [service refreshActive:NO];
    DDTestPresentation *auth = [DDTestPresentation new];
    player.callback(auth, nil, nil);
    XCTAssertEqual(host.presentations, 0u);
    host.modal = [UIViewController new]; [service refreshActive:YES];
    XCTAssertEqual(host.presentations, 0u);
    host.modal = nil; [service refreshActive:YES];
    XCTAssertEqual(host.presentations, 1u); XCTAssertEqualObjects(visibility, (@[@YES]));
    player.callback(auth, nil, nil); [service refreshActive:YES];
    XCTAssertEqual(host.presentations, 1u);
    player.callback(nil, nil, nil); host.modal = nil; auth.host = nil;
    [service refreshActive:YES]; [service refreshActive:YES];
    XCTAssertEqualObjects(visibility, (@[@YES, @NO]));
}
@end
