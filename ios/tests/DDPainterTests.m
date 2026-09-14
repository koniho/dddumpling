#import <XCTest/XCTest.h>
#import "DDPainter.h"

@interface DDIOSPainter (ColorTests)
- (CGColorRef)cachedColor:(jint)color;
@end

@interface DDPainterTests : XCTestCase
@end

@implementation DDPainterTests
- (void)testPaletteReuseAndEvictionPreserveComponents {
  DDIOSPainter *painter = [DDIOSPainter new];
  CGColorRef first = [painter cachedColor:(jint)0x7f123456];
  XCTAssertEqual(first, [painter cachedColor:(jint)0x7f123456]);
  uint32_t key = 42;
  for (NSUInteger i = 0; i < 10000; ++i) {
    key = key * 1664525u + 1013904223u;
    CGColorRef color = [painter cachedColor:(jint)key];
    const CGFloat *rgba = CGColorGetComponents(color);
    XCTAssertEqualWithAccuracy(rgba[0], (key >> 16 & 255) / 255.0, .000001);
    XCTAssertEqualWithAccuracy(rgba[1], (key >> 8 & 255) / 255.0, .000001);
    XCTAssertEqualWithAccuracy(rgba[2], (key & 255) / 255.0, .000001);
    XCTAssertEqualWithAccuracy(rgba[3], (key >> 24) / 255.0, .000001);
  }
  XCTAssertEqualWithAccuracy(CGColorGetAlpha([painter cachedColor:(jint)0x7f123456]), 127 / 255.0, .000001);
}

- (void)testCachedFillAndStrokeMatchOriginalPixels {
  unsigned char expected[64 * 64 * 4] = {0}, actual[64 * 64 * 4] = {0};
  CGColorSpaceRef space = CGColorSpaceCreateDeviceRGB();
  CGContextRef reference = CGBitmapContextCreate(expected, 64, 64, 8, 64 * 4, space, kCGImageAlphaPremultipliedLast);
  CGContextRef cached = CGBitmapContextCreate(actual, 64, 64, 8, 64 * 4, space, kCGImageAlphaPremultipliedLast);
  DDIOSPainter *painter = [DDIOSPainter new];
  painter.context = cached;
  uint32_t key = 42;
  for (NSUInteger i = 0; i < 3000; ++i) {
    key = key * 1664525u + 1013904223u;
    CGFloat r = (key >> 16 & 255) / 255.0, g = (key >> 8 & 255) / 255.0;
    CGFloat b = (key & 255) / 255.0, a = (key >> 24) / 255.0;
    float x = i % 57, y = (i * 13) % 57;
    CGContextSaveGState(reference); [painter save];
    CGContextSetRGBFillColor(reference, r, g, b, a);
    CGContextFillRect(reference, CGRectMake(x, y, 7, 7));
    [painter fillRectWithFloat:x withFloat:y withFloat:x + 7 withFloat:y + 7 withInt:(jint)key];
    CGContextSetRGBStrokeColor(reference, r, g, b, a);
    CGContextSetLineWidth(reference, 1.5);
    CGContextSetLineCap(reference, kCGLineCapRound);
    CGContextSetLineJoin(reference, kCGLineJoinRound);
    CGContextMoveToPoint(reference, x, y); CGContextAddLineToPoint(reference, x + 7, y + 7);
    CGContextStrokePath(reference);
    [painter lineWithFloat:x withFloat:y withFloat:x + 7 withFloat:y + 7 withInt:(jint)key withFloat:1.5];
    CGContextRestoreGState(reference); [painter restore];
  }
  XCTAssertEqualObjects([NSData dataWithBytes:expected length:sizeof(expected)],
                        [NSData dataWithBytes:actual length:sizeof(actual)]);
  painter.context = NULL;
  CGContextRelease(reference); CGContextRelease(cached); CGColorSpaceRelease(space);
}
@end
