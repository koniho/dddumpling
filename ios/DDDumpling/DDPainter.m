#import "DDPainter.h"

#import <CoreText/CoreText.h>
#import <UIKit/UIKit.h>
#import <math.h>

#import "com/dddumpling/game/MushroomForest.h"
#import "com/dddumpling/game/SlimeBasinArt.h"
#import "IOSObjectArray.h"
#import "IOSPrimitiveArray.h"

static const CGFloat DDDegreesToRadians = (CGFloat)M_PI / 180.0;

static inline void DDSetStrokeColor(CGContextRef context, CGColorRef color,
                                    jfloat width) {
  CGContextSetStrokeColorWithColor(context, color);
  CGContextSetLineWidth(context, width);
  CGContextSetLineJoin(context, kCGLineJoinRound);
  CGContextSetLineCap(context, kCGLineCapRound);
}

static inline BOOL DDHasPolygon(IOSFloatArray *points) {
  return points != nil && points->size_ >= 6;
}

static void DDAppendPolygon(CGContextRef context, IOSFloatArray *points) {
  const jfloat *values = points->buffer_;
  const jint count = points->size_ & ~1;
  CGContextMoveToPoint(context, values[0], values[1]);
  for (jint i = 2; i < count; i += 2) {
    CGContextAddLineToPoint(context, values[i], values[i + 1]);
  }
  CGContextClosePath(context);
}

@interface DDIOSPainter () {
  CGImageRef _forest[6];
  CGImageRef _basin[5];
  CGImageRef _forestTinted[16];
  uint64_t _forestKeys[16];
  NSUInteger _forestNext;
  BOOL _drawingForest;
  CGColorSpaceRef _colorSpace;
  CGColorRef _colors[1024];
  uint32_t _colorKeys[1024];
}
@end

@implementation DDIOSPainter

// The frame wrapper supplies the selected skin; the native canvas starts with Default.
- (jint)skin { return 0; }

// Immutable forest artwork is cached; unsupported stamp families retain vectors.
- (id)stamps { return _drawingForest ? nil : self; }
- (jboolean)stampWithInt:(jint)art withFloat:(jfloat)x withFloat:(jfloat)y
              withFloat:(jfloat)w withFloat:(jfloat)h withFloat:(jfloat)angle
                withInt:(jint)tint withFloat:(jfloat)amount { return NO; }
- (jboolean)greenStampWithInt:(jint)art withFloat:(jfloat)x withFloat:(jfloat)y
                   withFloat:(jfloat)w withFloat:(jfloat)h withFloat:(jfloat)angle
                     withInt:(jint)tint withFloat:(jfloat)amount withFloat:(jfloat)phase { return NO; }

- (CGImageRef)forestImage:(jint)art color:(jint)color {
  const size_t side=256, rowBytes=side*4;
  if (!_forest[art]) {
    CGContextRef bitmap=CGBitmapContextCreate(NULL,side,side,8,rowBytes,_colorSpace,
        kCGBitmapByteOrder32Big|kCGImageAlphaPremultipliedLast);
    if (!bitmap) return NULL;
    CGContextTranslateCTM(bitmap,side*.5,side*.5);
    CGContextRef prior=self.context; self.context=bitmap; _drawingForest=YES;
    [DDMushroomForest stampArtWithDDPainter:self withInt:art withFloat:side/5.0f];
    _drawingForest=NO; self.context=prior;
    _forest[art]=CGBitmapContextCreateImage(bitmap); CGContextRelease(bitmap);
  }
  if (!color || color==(jint)0xFF71917A || !_forest[art]) return _forest[art];
  uint64_t key=((uint64_t)(uint32_t)color<<8)|(uint64_t)art;
  for (NSUInteger i=0;i<16;i++)if (_forestTinted[i] && _forestKeys[i]==key)return _forestTinted[i];
  CGContextRef bitmap=CGBitmapContextCreate(NULL,side,side,8,rowBytes,_colorSpace,
      kCGBitmapByteOrder32Big|kCGImageAlphaPremultipliedLast);
  if (!bitmap) return _forest[art];
  CGContextDrawImage(bitmap,CGRectMake(0,0,side,side),_forest[art]);
  uint8_t *pixels=CGBitmapContextGetData(bitmap);
  float red=((color>>16)&255)/113.0f,green=((color>>8)&255)/145.0f,blue=(color&255)/122.0f;
  for (size_t i=0;i<side*side;i++) {
    uint8_t *pixel=pixels+i*4,alpha=pixel[3];
    pixel[0]=(uint8_t)MIN(alpha,lroundf(pixel[0]*red));
    pixel[1]=(uint8_t)MIN(alpha,lroundf(pixel[1]*green));
    pixel[2]=(uint8_t)MIN(alpha,lroundf(pixel[2]*blue));
  }
  NSUInteger slot=_forestNext++%16;
  CGImageRelease(_forestTinted[slot]);_forestKeys[slot]=key;
  _forestTinted[slot]=CGBitmapContextCreateImage(bitmap);CGContextRelease(bitmap);
  return _forestTinted[slot]?:_forest[art];
}

- (jboolean)forestWithInt:(jint)art withFloat:(jfloat)x withFloat:(jfloat)y
               withFloat:(jfloat)radius withFloat:(jfloat)lean withInt:(jint)color withInt:(jint)alpha {
  CGContextRef context=self.context;
  if (!context || _drawingForest || art<0 || art>=6 || radius<=0)return NO;
  CGImageRef image=[self forestImage:art color:color];if (!image)return NO;
  CGContextSaveGState(context);CGContextTranslateCTM(context,x,y);
  if (art<3) {
    CGContextScaleCTM(context,lean<0?-1:1,1);
    CGContextRotateCTM(context,(fabsf(lean)-1)*29*DDDegreesToRadians);
  } else CGContextRotateCTM(context,lean-[DDMushroomForest litterAngleWithInt:art]);
  CGContextSetAlpha(context,MAX(0,MIN(255,alpha))/255.0);
  CGContextSetInterpolationQuality(context,kCGInterpolationMedium);
  CGContextDrawImage(context,CGRectMake(-radius*2.5,-radius*2.5,radius*5,radius*5),image);
  CGContextRestoreGState(context);return YES;
}

- (CGImageRef)basinImage:(jint)art {
  if (art<0 || art>=5)return NULL;
  if (!_basin[art]) {
    size_t w=[DDSlimeBasinArt widthWithInt:art],h=[DDSlimeBasinArt heightWithInt:art];
    CGContextRef bitmap=CGBitmapContextCreate(NULL,w,h,8,w*4,_colorSpace,
        kCGBitmapByteOrder32Big|kCGImageAlphaPremultipliedLast);
    if (!bitmap)return NULL;
    CGContextRef prior=self.context;self.context=bitmap;_drawingForest=YES;
    [DDSlimeBasinArt paintWithDDPainter:self withInt:art withFloat:w withFloat:h];
    _drawingForest=NO;self.context=prior;
    _basin[art]=CGBitmapContextCreateImage(bitmap);CGContextRelease(bitmap);
  }
  return _basin[art];
}

- (jboolean)basinWithInt:(jint)art withFloat:(jfloat)x withFloat:(jfloat)y
              withFloat:(jfloat)w withFloat:(jfloat)h withInt:(jint)alpha {
  if (!self.context || _drawingForest || w<=0 || h<=0)return NO;
  CGImageRef image=[self basinImage:art];if (!image)return NO;
  CGContextSaveGState(self.context);
  CGContextSetAlpha(self.context,MAX(0,MIN(255,alpha))/255.0);
  CGContextSetInterpolationQuality(self.context,kCGInterpolationMedium);
  CGContextDrawImage(self.context,CGRectMake(x,y,w,h),image);
  CGContextRestoreGState(self.context);return YES;
}

- (jboolean)basinMeshWithInt:(jint)art withFloat:(jfloat)x withFloat:(jfloat)y
                  withFloat:(jfloat)w withFloat:(jfloat)h withFloatArray:(IOSFloatArray *)offsets
                    withInt:(jint)alpha {
  if (!self.context || _drawingForest || (art!=3 && art!=4) || !offsets
      || offsets->size_<2 || offsets->size_>17 || w<=0 || h<=0)return NO;
  CGImageRef image=[self basinImage:art];if (!image)return NO;
  CGContextRef ctx=self.context;int rows=offsets->size_-1;BOOL left=art==3;
  CGContextSaveGState(ctx);
  CGContextSetAlpha(ctx,MAX(0,MIN(255,alpha))/255.0);
  CGContextSetInterpolationQuality(ctx,kCGInterpolationMedium);
  // Adjacent clipped triangles share edges. Texture filtering supplies the visible silhouette AA.
  CGContextSetShouldAntialias(ctx,NO);
  for (int row=0;row<rows;row++) {
    CGFloat v=row/(CGFloat)rows,y0=y+h*v,y1=y+h*(row+1)/(CGFloat)rows;
    CGFloat l0=x-(left?0:offsets->buffer_[row]),r0=x+w+(left?offsets->buffer_[row]:0);
    CGFloat l1=x-(left?0:offsets->buffer_[row+1]),r1=x+w+(left?offsets->buffer_[row+1]:0);
    for (int triangle=0;triangle<2;triangle++) {
      CGContextSaveGState(ctx);CGContextBeginPath(ctx);
      CGContextMoveToPoint(ctx,r0,y0);CGContextAddLineToPoint(ctx,l1,y1);
      CGContextAddLineToPoint(ctx,triangle?r1:l0,triangle?y1:y0);CGContextClosePath(ctx);
      CGContextClip(ctx);
      CGFloat a=triangle?r1-l1:r0-l0,c=(triangle?r1-r0:l1-l0)*rows;
      CGFloat tx=(triangle?r0-a:l0)-c*v;
      CGContextConcatCTM(ctx,CGAffineTransformMake(a,0,c,h,tx,y));
      CGContextDrawImage(ctx,CGRectMake(0,0,1,1),image);CGContextRestoreGState(ctx);
    }
  }
  CGContextRestoreGState(ctx);return YES;
}

- (instancetype)init {
  if ((self = [super init])) _colorSpace = CGColorSpaceCreateDeviceRGB();
  return self;
}

- (void)dealloc {
  for (NSUInteger i=0;i<5;i++) CGImageRelease(_basin[i]);
  for (NSUInteger i=0;i<6;i++) CGImageRelease(_forest[i]);
  for (NSUInteger i=0;i<16;i++) CGImageRelease(_forestTinted[i]);
  for (NSUInteger i = 0; i < 1024; ++i) CGColorRelease(_colors[i]);
  CGColorSpaceRelease(_colorSpace);
}

- (CGColorRef)cachedColor:(jint)color {
  uint32_t key = (uint32_t)color;
  NSUInteger slot = ((key ^ (key >> 16)) * 2654435761u) >> 22;
  if (!_colors[slot] || _colorKeys[slot] != key) {
    CGFloat components[] = {(key >> 16 & 255) / 255.0, (key >> 8 & 255) / 255.0,
                            (key & 255) / 255.0, (key >> 24) / 255.0};
    // Fixed capacity: rainbow and fading effects must not grow a palette indefinitely.
    CGColorRelease(_colors[slot]);
    _colors[slot] = CGColorCreate(_colorSpace, components);
    _colorKeys[slot] = key;
  }
  return _colors[slot];
}

- (void)fillPolyWithFloatArray:(IOSFloatArray *)points withInt:(jint)color {
  CGContextRef context = self.context;
  if (context == NULL || !DDHasPolygon(points)) return;
  CGContextBeginPath(context);
  DDAppendPolygon(context, points);
  CGContextSetFillColorWithColor(context, [self cachedColor:color]);
  CGContextFillPath(context);
}

- (void)fillContoursWithFloatArray2:(IOSObjectArray *)contours
                             withInt:(jint)color {
  CGContextRef context = self.context;
  if (context == NULL || contours == nil) return;

  CGContextBeginPath(context);
  for (jint i = 0; i < contours->size_; i++) {
    IOSFloatArray *points = (IOSFloatArray *)[contours objectAtIndex:i];
    if (DDHasPolygon(points)) DDAppendPolygon(context, points);
  }
  CGContextSetFillColorWithColor(context, [self cachedColor:color]);
  CGContextEOFillPath(context);
}

- (void)strokePolyWithFloatArray:(IOSFloatArray *)points
                         withInt:(jint)color
                       withFloat:(jfloat)width {
  CGContextRef context = self.context;
  if (context == NULL || !DDHasPolygon(points)) return;
  CGContextBeginPath(context);
  DDAppendPolygon(context, points);
  DDSetStrokeColor(context, [self cachedColor:color], width);
  CGContextStrokePath(context);
}

- (void)fillCircleWithFloat:(jfloat)cx
                   withFloat:(jfloat)cy
                   withFloat:(jfloat)radius
                     withInt:(jint)color {
  CGContextRef context = self.context;
  if (context == NULL || radius <= 0) return;
  CGContextSetFillColorWithColor(context, [self cachedColor:color]);
  CGContextFillEllipseInRect(context, CGRectMake(cx - radius, cy - radius,
                                                  radius * 2, radius * 2));
}

- (void)strokeCircleWithFloat:(jfloat)cx
                     withFloat:(jfloat)cy
                     withFloat:(jfloat)radius
                       withInt:(jint)color
                     withFloat:(jfloat)width {
  CGContextRef context = self.context;
  if (context == NULL || radius <= 0) return;
  DDSetStrokeColor(context, [self cachedColor:color], width);
  CGContextStrokeEllipseInRect(context, CGRectMake(cx - radius, cy - radius,
                                                    radius * 2, radius * 2));
}

- (void)arcWithFloat:(jfloat)cx
            withFloat:(jfloat)cy
            withFloat:(jfloat)rx
            withFloat:(jfloat)ry
            withFloat:(jfloat)start
            withFloat:(jfloat)sweep
              withInt:(jint)color
            withFloat:(jfloat)width {
  CGContextRef context = self.context;
  if (context == NULL || rx <= 0 || ry <= 0 || sweep == 0) return;

  // Canvas measures increasing screen-space angles clockwise. Build the
  // ellipse explicitly so that convention survives UIKit's flipped CTM.
  const jint segments = MAX(1, (jint)ceil(fabs(sweep) / 90.0));
  const CGFloat startRadians = start * DDDegreesToRadians;
  const CGFloat sweepRadians = sweep * DDDegreesToRadians;
  CGFloat angle = startRadians;
  CGContextBeginPath(context);
  CGContextMoveToPoint(context, cx + rx * cos(angle), cy + ry * sin(angle));
  for (jint i = 0; i < segments; i++) {
    const CGFloat next = startRadians + sweepRadians * (i + 1) / segments;
    const CGFloat delta = next - angle;
    const CGFloat handle = 4.0 / 3.0 * tan(delta / 4.0);
    const CGFloat x0 = cx + rx * cos(angle);
    const CGFloat y0 = cy + ry * sin(angle);
    const CGFloat x1 = cx + rx * cos(next);
    const CGFloat y1 = cy + ry * sin(next);
    CGContextAddCurveToPoint(context,
                             x0 - handle * rx * sin(angle),
                             y0 + handle * ry * cos(angle),
                             x1 + handle * rx * sin(next),
                             y1 - handle * ry * cos(next), x1, y1);
    angle = next;
  }
  DDSetStrokeColor(context, [self cachedColor:color], width);
  CGContextStrokePath(context);
}

- (void)fillEllipseWithFloat:(jfloat)cx
                    withFloat:(jfloat)cy
                    withFloat:(jfloat)rx
                    withFloat:(jfloat)ry
                      withInt:(jint)color {
  CGContextRef context = self.context;
  if (context == NULL || rx <= 0 || ry <= 0) return;
  CGContextSetFillColorWithColor(context, [self cachedColor:color]);
  CGContextFillEllipseInRect(context, CGRectMake(cx - rx, cy - ry, rx * 2,
                                                  ry * 2));
}

- (void)polylineWithFloatArray:(IOSFloatArray *)points
                       withInt:(jint)color
                     withFloat:(jfloat)width {
  CGContextRef context = self.context;
  if (context == NULL || points == nil || points->size_ < 4) return;
  const jfloat *values = points->buffer_;
  const jint count = points->size_ & ~1;
  CGContextBeginPath(context);
  CGContextMoveToPoint(context, values[0], values[1]);
  for (jint i = 2; i < count; i += 2) {
    CGContextAddLineToPoint(context, values[i], values[i + 1]);
  }
  DDSetStrokeColor(context, [self cachedColor:color], width);
  CGContextStrokePath(context);
}

- (void)fillRectWithFloat:(jfloat)left
                 withFloat:(jfloat)top
                 withFloat:(jfloat)right
                 withFloat:(jfloat)bottom
                   withInt:(jint)color {
  CGContextRef context = self.context;
  if (context == NULL) return;
  CGContextSetFillColorWithColor(context, [self cachedColor:color]);
  CGContextFillRect(context, CGRectMake(left, top, right - left, bottom - top));
}

- (void)lineWithFloat:(jfloat)x1
             withFloat:(jfloat)y1
             withFloat:(jfloat)x2
             withFloat:(jfloat)y2
               withInt:(jint)color
             withFloat:(jfloat)width {
  CGContextRef context = self.context;
  if (context == NULL) return;
  CGContextBeginPath(context);
  CGContextMoveToPoint(context, x1, y1);
  CGContextAddLineToPoint(context, x2, y2);
  DDSetStrokeColor(context, [self cachedColor:color], width);
  CGContextStrokePath(context);
}

- (void)textWithNSString:(NSString *)string
                withFloat:(jfloat)x
                withFloat:(jfloat)y
                withFloat:(jfloat)size
                  withInt:(jint)color
                  withInt:(jint)align
              withBoolean:(jboolean)bold {
  CGContextRef context = self.context;
  if (context == NULL || string == nil || string.length == 0 || size <= 0) return;

  // Bungee has its own heavy strokes, so both Java text styles use the same
  // bundled face. The different system fallbacks mirror CanvasPainter.
  UIFont *font = [UIFont fontWithName:@"Bungee-Regular" size:size];
  if (font == nil) {
    font = [UIFont systemFontOfSize:size
                              weight:bold ? UIFontWeightBold : UIFontWeightMedium];
  }
  UIColor *ink = [UIColor colorWithRed:((uint32_t)color >> 16 & 0xff) / 255.0
                                  green:((uint32_t)color >> 8 & 0xff) / 255.0
                                   blue:((uint32_t)color & 0xff) / 255.0
                                  alpha:((uint32_t)color >> 24) / 255.0];
  NSDictionary *attributes = @{
      (__bridge id)kCTFontAttributeName : font,
      (__bridge id)kCTForegroundColorAttributeName : (__bridge id)ink.CGColor,
  };
  NSAttributedString *attributed =
      [[NSAttributedString alloc] initWithString:string attributes:attributes];
  CTLineRef line = CTLineCreateWithAttributedString((__bridge CFAttributedStringRef)attributed);
  CGFloat advance = CTLineGetTypographicBounds(line, NULL, NULL, NULL);
  if (align == -1) {
    // LEFT: x is already the leading edge.
  } else if (align == 1) {
    x -= advance;
  } else {
    x -= advance / 2.0;
  }

  // Core Text's glyph coordinates are y-up, while a UIView's graphics
  // context is y-down. Flipping around the requested baseline preserves the
  // Java contract that y is the text baseline, not its top edge.
  CGContextSaveGState(context);
  CGContextSetTextMatrix(context, CGAffineTransformIdentity);
  CGContextTranslateCTM(context, 0, y * 2.0);
  CGContextScaleCTM(context, 1, -1);
  CGContextSetTextPosition(context, x, y);
  CTLineDraw(line, context);
  CGContextRestoreGState(context);
  CFRelease(line);
}

- (void)clipRectWithFloat:(jfloat)left
                 withFloat:(jfloat)top
                 withFloat:(jfloat)right
                 withFloat:(jfloat)bottom {
  CGContextRef context = self.context;
  if (context == NULL) return;
  CGContextClipToRect(context, CGRectMake(left, top, right - left, bottom - top));
}

- (void)clipOutCircleWithFloat:(jfloat)x withFloat:(jfloat)y withFloat:(jfloat)radius {
  CGContextRef context = self.context;
  if (context == NULL) return;
  CGRect bounds = CGContextGetClipBoundingBox(context);
  CGContextBeginPath(context);
  CGContextAddRect(context, bounds);
  CGContextAddEllipseInRect(context, CGRectMake(x-radius, y-radius, radius*2, radius*2));
  CGContextEOClip(context);
}

- (void)save {
  if (self.context != NULL) CGContextSaveGState(self.context);
}

- (void)restore {
  if (self.context != NULL) CGContextRestoreGState(self.context);
}

- (void)translateWithFloat:(jfloat)dx withFloat:(jfloat)dy {
  if (self.context != NULL) CGContextTranslateCTM(self.context, dx, dy);
}

@end
