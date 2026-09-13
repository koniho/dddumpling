#import <XCTest/XCTest.h>
#import "DDViewport.h"

@interface DDViewportTests : XCTestCase
@end

@implementation DDViewportTests
- (void)testPortraitLandscapeAndNarrowWindowsKeepOneCanvasInsideSafeArea {
    for (NSValue *value in @[[NSValue valueWithCGSize:CGSizeMake(744, 1133)],
                            [NSValue valueWithCGSize:CGSizeMake(1133, 744)],
                            [NSValue valueWithCGSize:CGSizeMake(1032, 1376)],
                            [NSValue valueWithCGSize:CGSizeMake(1376, 1032)],
                            [NSValue valueWithCGSize:CGSizeMake(320, 700)],
                            [NSValue valueWithCGSize:CGSizeMake(600, 350)]]) {
        CGRect bounds = (CGRect){CGPointZero, value.CGSizeValue};
        UIEdgeInsets safe = UIEdgeInsetsMake(40, 12, 20, 12);
        DDViewport viewport = DDViewportMake(bounds, safe, YES);
        CGRect available = UIEdgeInsetsInsetRect(bounds, safe);
        XCTAssertTrue(CGRectContainsRect(available, viewport.frame));
        XCTAssertTrue(CGRectContainsRect(available, viewport.navigation));
        XCTAssertEqualWithAccuracy(viewport.frame.size.width / viewport.frame.size.height, 390.0 / 800, .00001);
        XCTAssertEqualWithAccuracy(CGRectGetMidX(viewport.frame), CGRectGetMidX(available), .00001);
        XCTAssertEqual(viewport.navigation.size.width, 44);
        XCTAssertLessThanOrEqual(CGRectGetMaxY(viewport.navigation), CGRectGetMinY(viewport.frame));
        // Current, retained and historical samples all use this same inverse transform.
        for (NSValue *sample in @[[NSValue valueWithCGPoint:CGPointMake(0, 0)],
                                  [NSValue valueWithCGPoint:CGPointMake(390, 800)],
                                  [NSValue valueWithCGPoint:CGPointMake(111, 617)],
                                  [NSValue valueWithCGPoint:CGPointMake(-40, 350)]]) {
            CGPoint logical = sample.CGPointValue;
            CGPoint window = CGPointMake(viewport.frame.origin.x + logical.x * viewport.scale,
                                         viewport.frame.origin.y + logical.y * viewport.scale);
            CGPoint mapped = DDViewportPoint(viewport, window);
            XCTAssertEqualWithAccuracy(mapped.x, logical.x, .00001);
            XCTAssertEqualWithAccuracy(mapped.y, logical.y, .00001);
        }
    }
}
- (void)testIPhoneGeometryAndTouchCoordinatesStayUnchanged {
    DDViewport viewport = DDViewportMake(CGRectMake(0, 0, 393, 852), UIEdgeInsetsMake(59, 0, 34, 0), NO);
    XCTAssertTrue(CGRectEqualToRect(viewport.frame, CGRectMake(0, 0, 393, 852)));
    XCTAssertTrue(UIEdgeInsetsEqualToEdgeInsets(viewport.insets, UIEdgeInsetsMake(103, 0, 34, 0)));
    XCTAssertTrue(CGRectEqualToRect(viewport.navigation, CGRectMake(347, 59, 44, 44)));
    XCTAssertTrue(CGPointEqualToPoint(DDViewportPoint(viewport, CGPointMake(120, 700)), CGPointMake(120, 700)));
}
@end
