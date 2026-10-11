#import <XCTest/XCTest.h>
#import <GameController/GameController.h>
#import "DDGameView.h"
#import "com/dddumpling/game/IOSGame.h"

// Read-only UIKit event properties supplied at the native host boundary.
@interface DDKeyboardKey : UIKey
@property(nonatomic) UIKeyboardHIDUsage keyCode;
@property(nonatomic) UIKeyModifierFlags modifierFlags;
@end
@implementation DDKeyboardKey
@synthesize keyCode = _testKeyCode, modifierFlags = _testModifiers;
@end
@interface DDKeyboardPress : UIPress
@property(nonatomic, strong) UIKey *key;
@end
@implementation DDKeyboardPress
@synthesize key = _testKey;
@end
@interface DDKeyboardEvent : UIPressesEvent
@property(nonatomic) UIKeyModifierFlags modifierFlags;
@end
@implementation DDKeyboardEvent
@synthesize modifierFlags = _testModifiers;
@end
@interface DDKeyboardNextResponder : UIResponder
@property(nonatomic) NSUInteger forwarded;
@end
@implementation DDKeyboardNextResponder
- (void)pressesBegan:(NSSet<UIPress *> *)presses withEvent:(UIPressesEvent *)event {
    self.forwarded += presses.count;
}
@end
@interface DDKeyboardView : DDGameView
@property(nonatomic, strong) DDKeyboardNextResponder *receiver;
@end
@implementation DDKeyboardView
- (UIResponder *)nextResponder { return self.receiver; }
@end

@interface DDKeyboardTests : XCTestCase
@end
@implementation DDKeyboardTests
- (void)testNativeModifiersRepeatsAndDisconnect {
    DDKeyboardView *view = [[DDKeyboardView alloc] initWithFrame:CGRectMake(0, 0, 393, 852)];
    [[view valueForKey:@"displayLink"] invalidate];
    view.receiver = [DDKeyboardNextResponder new];
    DDIOSGame *game = [[DDIOSGame alloc] initWithDDGameCore_Store:nil withDDGameCore_Sound:nil withLong:9];
    [game layoutWithFloat:393 withFloat:852 withFloat:0 withFloat:59 withFloat:0 withFloat:34];
    [game debugSceneWithNSString:@"keyboard:6"];
    [view setValue:game forKey:@"game"];
    [view setActive:YES];
    DDKeyboardPress *press = [DDKeyboardPress new];
    DDKeyboardKey *key = [DDKeyboardKey new];
    press.key = key;
    key.keyCode = UIKeyboardHIDUsageKeyboardS;
    NSSet *presses = [NSSet setWithObject:press];
    DDKeyboardEvent *event = [DDKeyboardEvent new];

    for (NSNumber *flags in @[@(UIKeyModifierCommand), @(UIKeyModifierControl), @(UIKeyModifierAlternate)]) {
        key.modifierFlags = flags.unsignedIntegerValue;
        [view pressesBegan:presses withEvent:(id)event];
        key.modifierFlags = 0;
        event.modifierFlags = flags.unsignedIntegerValue;
        [view pressesBegan:presses withEvent:(id)event];
        event.modifierFlags = 0;
    }
    XCTAssertTrue([[game debugStatus] containsString:@"keyboardPresses=0;"]);
    XCTAssertEqual(view.receiver.forwarded, 6u);
    [view pressesBegan:presses withEvent:(id)event];
    [view pressesBegan:presses withEvent:(id)event];
    XCTAssertTrue([[game debugStatus] containsString:@"keyboardPresses=1;keyboardGlyph=0"]);
    [view pressesEnded:presses withEvent:(id)event];
    [view pressesBegan:presses withEvent:(id)event];
    XCTAssertTrue([[game debugStatus] containsString:@"keyboardPresses=2;"]);
    [NSNotificationCenter.defaultCenter postNotificationName:GCKeyboardDidDisconnectNotification object:nil];
    [view pressesBegan:presses withEvent:(id)event];
    XCTAssertTrue([[game debugStatus] containsString:@"keyboardPresses=3;"]);
    [view resignFirstResponder];
    [view pressesBegan:presses withEvent:(id)event];
    XCTAssertTrue([[game debugStatus] containsString:@"keyboardPresses=4;"]);
    [view setActive:NO];
}
@end
