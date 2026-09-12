#import <CoreGraphics/CoreGraphics.h>
#import <Foundation/Foundation.h>

#import "com/dddumpling/game/Painter.h"

/**
 * Native implementation of the J2ObjC-translated {@code Painter} protocol.
 *
 * The generated protocol is named DDPainter, so this class deliberately uses
 * DDIOSPainter to keep the protocol and its implementation distinct. The view
 * that owns a frame assigns its current CGContext before calling Renderer.
 */
@interface DDIOSPainter : NSObject <DDPainter>

@property(nonatomic, assign) CGContextRef context;

@end
