#import <XCTest/XCTest.h>
#import "DDGameView.h"
#import "DDViewport.h"
#import "com/dddumpling/game/IOSTouch.h"
#import "com/dddumpling/game/IOSGame.h"
#import "com/dddumpling/game/GameCore.h"
#import "com/dddumpling/game/Showcase.h"
#import "com/dddumpling/game/Layout.h"

@interface DDTestTouch : UITouch
@property(nonatomic) CGPoint point;
@property(nonatomic) NSTimeInterval time;
@end
@implementation DDTestTouch
- (CGPoint)locationInView:(UIView *)view { return self.point; }
- (NSTimeInterval)timestamp { return self.time; }
@end

@interface DDTestEvent : UIEvent
@property(nonatomic) NSTimeInterval time;
@property(nonatomic, strong) UITouch *owner;
@property(nonatomic, strong) NSArray<UITouch *> *samples;
@end
@implementation DDTestEvent
- (NSTimeInterval)timestamp { return self.time; }
- (NSArray<UITouch *> *)coalescedTouchesForTouch:(UITouch *)touch {
    return touch == self.owner ? self.samples : @[];
}
@end

// Observe the native boundary without reimplementing Java gesture routing.
@interface DDTouchRecorder : NSObject
@property(nonatomic, strong) NSMutableArray<DDIOSTouch *> *packets;
@end
@implementation DDTouchRecorder
- (BOOL)touchWithDDIOSTouch:(DDIOSTouch *)packet { [self.packets addObject:packet]; return YES; }
- (BOOL)handlesBack { return NO; }
- (BOOL)paused { return NO; }
- (NSString *)debugStatus { return @""; }
- (void)layoutWithFloat:(float)width withFloat:(float)height withFloat:(float)left
             withFloat:(float)top withFloat:(float)right withFloat:(float)bottom {}
@end

@interface DDTabletTestView : DDGameView
@end
@implementation DDTabletTestView
- (UITraitCollection *)traitCollection { return [UITraitCollection traitCollectionWithUserInterfaceIdiom:UIUserInterfaceIdiomPad]; }
- (UIEdgeInsets)safeAreaInsets { return UIEdgeInsetsMake(24, 0, 20, 0); }
@end

@interface DDTouchViewportTests : XCTestCase
@end
@implementation DDTouchViewportTests
- (void)testDisconnectedSceneReleasesItsDisplayLinkAndView {
    __weak DDGameView *released;
    @autoreleasepool {
        DDGameView *view = [[DDGameView alloc] initWithFrame:CGRectMake(0, 0, 390, 844)];
        released = view;
        [view setActive:YES];
        [view disconnect];
        XCTAssertNil([view valueForKey:@"displayLink"]);
        XCTAssertFalse([[view valueForKey:@"active"] boolValue]);
    }
    XCTAssertNil(released);
}
- (DDTestTouch *)touchAt:(CGPoint)point time:(NSTimeInterval)time viewport:(DDViewport)viewport {
    DDTestTouch *touch = [DDTestTouch new];
    touch.point = CGPointMake(viewport.frame.origin.x + point.x * viewport.scale,
                             viewport.frame.origin.y + point.y * viewport.scale);
    touch.time = time;
    return touch;
}
- (void)tap:(CGPoint)point inView:(DDTabletTestView *)view {
    DDTestTouch *touch = [self touchAt:point time:1 viewport:DDViewportMake(view.bounds, view.safeAreaInsets, YES)];
    DDTestEvent *event = [DDTestEvent new]; event.time = 1;
    [view touchesBegan:[NSSet setWithObject:touch] withEvent:event];
    [view touchesEnded:[NSSet setWithObject:touch] withEvent:event];
}
- (void)testCollectionStoryTouchAlignmentSurvivesResize {
    DDTabletTestView *view = [[DDTabletTestView alloc] initWithFrame:CGRectMake(0, 0, 1133, 744)];
    [view layoutSubviews];
    [view setActive:YES];
    DDIOSGame *game = [view valueForKey:@"game"];
    DDGameCore *core = [game core];
    DDLayout *layout = [game geometry];
    // In-memory test fixture only; no save or production scene is modified.
    core->collected_ = 1;
    core->caseIndex_ = 0;
    [self tap:CGPointMake([DDShowcase iconCxWithDDLayout:layout withFloat:core->clock_],
                          [DDShowcase iconCyWithDDLayout:layout withFloat:core->clock_]) inView:view];
    XCTAssertTrue(core->caseOpen_);
    for (int frame = 0; frame < 30; frame++) [game updateWithFloat:.05];
    [self tap:CGPointMake(195, [DDShowcase focusCyWithDDLayout:layout]) inView:view];
    XCTAssertTrue([core storyOpen]);
    for (int frame = 0; frame < 30; frame++) [game updateWithFloat:.05];
    UIGraphicsImageRenderer *renderer = [[UIGraphicsImageRenderer alloc] initWithSize:view.bounds.size];
    UIImage *image = [renderer imageWithActions:^(UIGraphicsImageRendererContext *context) {
        [view drawRect:view.bounds];
    }];
    XCTAttachment *shot = [XCTAttachment attachmentWithImage:image];
    shot.name = @"ipad-story-test-fixture";
    shot.lifetime = XCTAttachmentLifetimeKeepAlways;
    [self addAttachment:shot];
    view.frame = CGRectMake(0, 0, 320, 700);
    [view layoutSubviews];
    XCTAssertTrue([core storyOpen]);
    [self tap:CGPointMake(195, 400) inView:view];
    XCTAssertFalse([core storyOpen]);
    XCTAssertTrue(core->caseOpen_);
    [view disconnect];
}
- (void)testAllPacketCoordinatesAndResizeCancellationUseTheViewport {
    DDTabletTestView *view = [[DDTabletTestView alloc] initWithFrame:CGRectMake(0, 0, 1133, 744)];
    [view layoutSubviews];
    [view setActive:YES];
    // Stop the display link before substituting the packet recorder.
    [[view valueForKey:@"displayLink"] invalidate];
    DDTouchRecorder *recorder = [DDTouchRecorder new];
    recorder.packets = [NSMutableArray new];
    [view setValue:recorder forKey:@"game"];
    DDViewport viewport = DDViewportMake(view.bounds, view.safeAreaInsets, YES);
    DDTestTouch *left = [self touchAt:CGPointMake(80, 700) time:1 viewport:viewport];
    DDTestTouch *right = [self touchAt:CGPointMake(310, 700) time:1 viewport:viewport];
    DDTestEvent *event = [DDTestEvent new]; event.time = 1;
    [view touchesBegan:[NSSet setWithObject:left] withEvent:event];
    [view touchesBegan:[NSSet setWithObject:right] withEvent:event];
    DDIOSTouch *packet = recorder.packets.lastObject;
    XCTAssertEqual(packet.getActionMasked, 5);
    XCTAssertEqual(packet.getActionIndex, 1);
    XCTAssertEqual(packet.getPointerCount, 2);
    XCTAssertEqualWithAccuracy([packet getXWithInt:0], 80, .001);
    XCTAssertEqualWithAccuracy([packet getXWithInt:1], 310, .001);
    event.owner = left;
    event.samples = @[[self touchAt:CGPointMake(100, 640) time:1.1 viewport:viewport],
                      [self touchAt:CGPointMake(120, 600) time:1.2 viewport:viewport]];
    left.point = [self touchAt:CGPointMake(140, 580) time:1.3 viewport:viewport].point;
    left.time = event.time = 1.3;
    [view touchesMoved:[NSSet setWithObject:left] withEvent:event];
    packet = recorder.packets.lastObject;
    XCTAssertEqual(packet.getHistorySize, 2);
    XCTAssertEqualWithAccuracy([packet getHistoricalXWithInt:0 withInt:0], 100, .001);
    XCTAssertEqualWithAccuracy([packet getHistoricalYWithInt:0 withInt:1], 600, .001);
    XCTAssertEqualWithAccuracy([packet getHistoricalXWithInt:1 withInt:1], 310, .001);
    XCTAssertEqualWithAccuracy([packet getHistoricalYWithInt:1 withInt:0], 700, .001);
    XCTAssertEqualWithAccuracy([packet getXWithInt:0], 140, .001);
    [view touchesEnded:[NSSet setWithObject:right] withEvent:event];
    packet = recorder.packets.lastObject;
    XCTAssertEqual(packet.getActionMasked, 6);
    XCTAssertEqual([packet getPointerIdWithInt:1], 1);
    XCTAssertEqualWithAccuracy([packet getYWithInt:1], 700, .001);
    view.frame = CGRectMake(0, 0, 320, 700);
    [view layoutSubviews];
    XCTAssertEqual(recorder.packets.lastObject.getActionMasked, 3);
    XCTAssertEqualWithAccuracy(recorder.packets.lastObject.getX, 140, .001);
    NSUInteger count = recorder.packets.count;
    [view touchesMoved:[NSSet setWithObject:left] withEvent:event];
    [view touchesEnded:[NSSet setWithObject:left] withEvent:event];
    XCTAssertEqual(recorder.packets.count, count);
    viewport = DDViewportMake(view.bounds, view.safeAreaInsets, YES);
    DDTestTouch *fresh = [self touchAt:CGPointMake(195, 400) time:2 viewport:viewport];
    [view touchesBegan:[NSSet setWithObject:fresh] withEvent:event];
    packet = recorder.packets.lastObject;
    XCTAssertEqual(packet.getActionMasked, 0);
    XCTAssertEqualWithAccuracy(packet.getX, 195, .001);
    [view touchesCancelled:[NSSet setWithObject:fresh] withEvent:event];
    XCTAssertEqual(recorder.packets.lastObject.getActionMasked, 3);
    count = recorder.packets.count;
    DDTestTouch *outside = [DDTestTouch new]; outside.point = CGPointMake(0, 0);
    [view touchesBegan:[NSSet setWithObject:outside] withEvent:event];
    XCTAssertEqual(recorder.packets.count, count);
}
@end
