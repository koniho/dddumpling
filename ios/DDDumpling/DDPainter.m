#import "DDPainter.h"

#import <CoreText/CoreText.h>
#import <UIKit/UIKit.h>
#import <math.h>

#import "IOSObjectArray.h"
#import "IOSPrimitiveArray.h"

static const CGFloat DDDegreesToRadians = (CGFloat)M_PI / 180.0;

static inline void DDSetFillColor(CGContextRef context, jint color) {
  const CGFloat alpha = ((uint32_t)color >> 24) / 255.0;
  const CGFloat red = ((uint32_t)color >> 16 & 0xff) / 255.0;
  const CGFloat green = ((uint32_t)color >> 8 & 0xff) / 255.0;
  const CGFloat blue = ((uint32_t)color & 0xff) / 255.0;
  CGContextSetRGBFillColor(context, red, green, blue, alpha);
}

static inline void DDSetStrokeColor(CGContextRef context, jint color,
                                    jfloat width) {
  const CGFloat alpha = ((uint32_t)color >> 24) / 255.0;
  const CGFloat red = ((uint32_t)color >> 16 & 0xff) / 255.0;
  const CGFloat green = ((uint32_t)color >> 8 & 0xff) / 255.0;
  const CGFloat blue = ((uint32_t)color & 0xff) / 255.0;
  CGContextSetRGBStrokeColor(context, red, green, blue, alpha);
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

@implementation DDIOSPainter

- (void)fillPolyWithFloatArray:(IOSFloatArray *)points withInt:(jint)color {
  CGContextRef context = self.context;
  if (context == NULL || !DDHasPolygon(points)) return;
  CGContextBeginPath(context);
  DDAppendPolygon(context, points);
  DDSetFillColor(context, color);
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
  DDSetFillColor(context, color);
  CGContextEOFillPath(context);
}

- (void)strokePolyWithFloatArray:(IOSFloatArray *)points
                         withInt:(jint)color
                       withFloat:(jfloat)width {
  CGContextRef context = self.context;
  if (context == NULL || !DDHasPolygon(points)) return;
  CGContextBeginPath(context);
  DDAppendPolygon(context, points);
  DDSetStrokeColor(context, color, width);
  CGContextStrokePath(context);
}

- (void)fillCircleWithFloat:(jfloat)cx
                   withFloat:(jfloat)cy
                   withFloat:(jfloat)radius
                     withInt:(jint)color {
  CGContextRef context = self.context;
  if (context == NULL || radius <= 0) return;
  DDSetFillColor(context, color);
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
  DDSetStrokeColor(context, color, width);
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
  DDSetStrokeColor(context, color, width);
  CGContextStrokePath(context);
}

- (void)fillEllipseWithFloat:(jfloat)cx
                    withFloat:(jfloat)cy
                    withFloat:(jfloat)rx
                    withFloat:(jfloat)ry
                      withInt:(jint)color {
  CGContextRef context = self.context;
  if (context == NULL || rx <= 0 || ry <= 0) return;
  DDSetFillColor(context, color);
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
  DDSetStrokeColor(context, color, width);
  CGContextStrokePath(context);
}

- (void)fillRectWithFloat:(jfloat)left
                 withFloat:(jfloat)top
                 withFloat:(jfloat)right
                 withFloat:(jfloat)bottom
                   withInt:(jint)color {
  CGContextRef context = self.context;
  if (context == NULL) return;
  DDSetFillColor(context, color);
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
  DDSetStrokeColor(context, color, width);
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
