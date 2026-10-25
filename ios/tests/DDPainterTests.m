#import <XCTest/XCTest.h>
#import "DDPainter.h"
#import "IOSPrimitiveArray.h"

@interface DDIOSPainter (ColorTests)
- (CGColorRef)cachedColor:(jint)color;
- (CGImageRef)basinImage:(jint)art;
- (CGImageRef)forestImage:(jint)art color:(jint)color;
@end

@interface DDPainterTests : XCTestCase
@end

@implementation DDPainterTests
- (void)testCanvasStartsWithDefaultSkin {
  XCTAssertEqual([[DDIOSPainter new] skin], 0);
}

- (void)testBasinCacheIsBoundedAndReusesImages {
  DDIOSPainter *painter=[DDIOSPainter new];
  for (jint art=0;art<5;art++) {
    CGImageRef image=[painter basinImage:art];
    XCTAssertNotEqual(image,NULL);XCTAssertEqual(image,[painter basinImage:art]);
    XCTAssertEqual(CGImageGetWidth(image),art==0?480u:art>=3?336u:256u);
    XCTAssertEqual(CGImageGetHeight(image),art==0 || art>=3?960u:256u);
  }
  XCTAssertEqual([painter basinImage:-1],NULL);XCTAssertEqual([painter basinImage:5],NULL);
}

- (void)testBasinDrawRestoresTransformAndAlpha {
  uint8_t pixels[256*256*4]={0};
  CGColorSpaceRef space=CGColorSpaceCreateDeviceRGB();
  CGContextRef context=CGBitmapContextCreate(pixels,256,256,8,256*4,space,
      kCGBitmapByteOrder32Big|kCGImageAlphaPremultipliedLast);
  DDIOSPainter *painter=[DDIOSPainter new];painter.context=context;
  CGAffineTransform transform=CGContextGetCTM(context);
  XCTAssertTrue([painter basinWithInt:0 withFloat:0 withFloat:0 withFloat:256 withFloat:256 withInt:80]);
  XCTAssertTrue(CGAffineTransformEqualToTransform(transform,CGContextGetCTM(context)));
  XCTAssertEqual(pixels[3],80);
  [painter fillRectWithFloat:0 withFloat:0 withFloat:256 withFloat:256 withInt:(jint)0xFFFFFFFF];
  XCTAssertEqual(pixels[3],255);
  painter.context=NULL;CGContextRelease(context);CGColorSpaceRelease(space);
}

- (void)testBasinMeshDrawRestoresContextAndKeepsAlphaBounded {
  uint8_t pixels[256*256*4]={0};
  CGColorSpaceRef space=CGColorSpaceCreateDeviceRGB();
  CGContextRef context=CGBitmapContextCreate(pixels,256,256,8,256*4,space,
      kCGBitmapByteOrder32Big|kCGImageAlphaPremultipliedLast);
  DDIOSPainter *painter=[DDIOSPainter new];painter.context=context;
  IOSFloatArray *offsets=[IOSFloatArray arrayWithLength:9];
  offsets->buffer_[3]=8;offsets->buffer_[4]=-12;offsets->buffer_[5]=5;
  CGAffineTransform transform=CGContextGetCTM(context);
  XCTAssertTrue([painter basinMeshWithInt:3 withFloat:0 withFloat:0 withFloat:100
                   withFloat:256 withFloatArray:offsets withInt:80]);
  XCTAssertTrue(CGAffineTransformEqualToTransform(transform,CGContextGetCTM(context)));
  NSUInteger painted=0;uint8_t maxAlpha=0;
  for (NSUInteger i=0;i<256*256;i++){painted+=pixels[i*4+3]>0;maxAlpha=MAX(maxAlpha,pixels[i*4+3]);}
  XCTAssertGreaterThan(painted,100u);XCTAssertLessThanOrEqual(maxAlpha,80);
  [painter fillRectWithFloat:0 withFloat:0 withFloat:256 withFloat:256 withInt:(jint)0xFFFFFFFF];
  XCTAssertEqual(pixels[3],255);
  painter.context=NULL;CGContextRelease(context);CGColorSpaceRelease(space);
}

- (void)testForestCacheReusesArtworkAndTintVariants {
  DDIOSPainter *painter=[DDIOSPainter new];
  XCTAssertEqual([painter stamps],painter);
  for (jint art=0;art<6;art++) {
    CGImageRef base=[painter forestImage:art color:0];
    XCTAssertNotEqual(base,NULL);
    XCTAssertEqual(CGImageGetWidth(base),256u);
    XCTAssertEqual(base,[painter forestImage:art color:0]);
    CGImageRef tinted=[painter forestImage:art color:(jint)0xFF91A486];
    XCTAssertNotEqual(tinted,NULL);
    XCTAssertEqual(tinted,[painter forestImage:art color:(jint)0xFF91A486]);
  }
}

- (void)testForestDrawRestoresTransformAndAlpha {
  uint8_t pixels[256*256*4]={0};
  CGColorSpaceRef space=CGColorSpaceCreateDeviceRGB();
  CGContextRef context=CGBitmapContextCreate(pixels,256,256,8,256*4,space,
      kCGBitmapByteOrder32Big|kCGImageAlphaPremultipliedLast);
  DDIOSPainter *painter=[DDIOSPainter new];painter.context=context;
  CGAffineTransform transform=CGContextGetCTM(context);
  XCTAssertTrue([painter forestWithInt:1 withFloat:128 withFloat:180 withFloat:55
                               withFloat:1 withInt:0 withInt:80]);
  XCTAssertTrue(CGAffineTransformEqualToTransform(transform,CGContextGetCTM(context)));
  NSUInteger painted=0;uint8_t maxAlpha=0;
  for (NSUInteger i=0;i<256*256;i++){painted+=pixels[i*4+3]>0;maxAlpha=MAX(maxAlpha,pixels[i*4+3]);}
  XCTAssertGreaterThan(painted,100u);XCTAssertLessThanOrEqual(maxAlpha,80);
  // Cover the bitmap so this alpha check is independent of its row orientation.
  [painter fillRectWithFloat:0 withFloat:0 withFloat:256 withFloat:256 withInt:(jint)0xFFFFFFFF];
  XCTAssertEqual(pixels[3],255);
  painter.context=NULL;CGContextRelease(context);CGColorSpaceRelease(space);
}

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
