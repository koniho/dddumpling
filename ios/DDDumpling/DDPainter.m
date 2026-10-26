#import "DDPainter.h"

#import <CoreText/CoreText.h>
#import <UIKit/UIKit.h>
#import <math.h>

#import "com/dddumpling/game/MushroomForest.h"
#import "com/dddumpling/game/SlimeBasinArt.h"
#import "com/dddumpling/game/CrystalField.h"
#import "com/dddumpling/game/CrystalArena.h"
#import "com/dddumpling/game/Underwater.h"
#import "com/dddumpling/game/UnderwaterVista.h"
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

// Grayscale copies are bounded by the corresponding live artwork slots.
static CGImageRef DDGrayImage(CGImageRef image,CGColorSpaceRef space) {
  size_t w=CGImageGetWidth(image),h=CGImageGetHeight(image);
  CGContextRef bitmap=CGBitmapContextCreate(NULL,w,h,8,w*4,space,
      kCGBitmapByteOrder32Big|kCGImageAlphaPremultipliedLast);
  if(!bitmap)return NULL;
  CGContextDrawImage(bitmap,CGRectMake(0,0,w,h),image);
  uint8_t *p=CGBitmapContextGetData(bitmap);
  for(size_t i=0;i<w*h;i++,p+=4) {
    uint8_t gray=(p[0]*54+p[1]*183+p[2]*19)/256;
    p[0]=p[1]=p[2]=gray;
  }
  CGImageRef result=CGBitmapContextCreateImage(bitmap);CGContextRelease(bitmap);return result;
}

static void DDDrawCachedImage(CGContextRef ctx,CGRect rect,CGImageRef color,CGImageRef gray,float mono) {
  if(mono<=0){CGContextDrawImage(ctx,rect,color);return;}
  if(mono>=1){CGContextDrawImage(ctx,rect,gray);return;}
  // Add premultiplied endpoints inside a local layer so the fade preserves image alpha.
  CGContextSaveGState(ctx);
  CGContextBeginTransparencyLayerWithRect(ctx,rect,NULL);
  CGContextSetBlendMode(ctx,kCGBlendModeCopy);CGContextSetAlpha(ctx,1-mono);
  CGContextDrawImage(ctx,rect,color);
  CGContextSetBlendMode(ctx,kCGBlendModePlusLighter);CGContextSetAlpha(ctx,mono);
  CGContextDrawImage(ctx,rect,gray);
  CGContextEndTransparencyLayer(ctx);CGContextRestoreGState(ctx);
}

@interface DDIOSPainter () {
  CGImageRef _forest[6];
  CGImageRef _reefVista[DDUnderwaterVista_COUNT],_reefVistaGray[DDUnderwaterVista_COUNT];
  CGImageRef _reef[DDUnderwater_ROCK_COUNT],_reefGray[DDUnderwater_ROCK_COUNT];
  CGImageRef _waterBand,_waterGlow,_waterBandGray,_waterGlowGray;
  CGImageRef _crystal[DDCrystalField_LAYERS],_crystalArena[DDCrystalField_LAYERS];
  CGImageRef _crystalGray[DDCrystalField_LAYERS],_crystalArenaGray[DDCrystalField_LAYERS];
  jint _crystalArenaArt[DDCrystalField_LAYERS];
  CGImageRef _basin[DDSlimeBasinArt_COUNT];
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

- (CGImageRef)waterImage:(BOOL)band {
  CGImageRef cached=band?_waterBand:_waterGlow;if(cached)return cached;
  size_t width=band?2:DDUnderwater_WATER_TEXTURE,height=DDUnderwater_WATER_TEXTURE;
  CGContextRef bitmap=CGBitmapContextCreate(NULL,width,height,8,width*4,_colorSpace,
      kCGBitmapByteOrder32Big|kCGImageAlphaPremultipliedLast);
  if(!bitmap)return NULL;
  uint8_t *pixels=CGBitmapContextGetData(bitmap);
  for(size_t y=0;y<height;y++)for(size_t x=0;x<width;x++) {
    uint32_t color=(uint32_t)[DDUnderwater waterPixelWithInt:(jint)x withInt:(jint)y withBoolean:band];
    uint32_t alpha=color>>24;uint8_t *pixel=pixels+(y*width+x)*4;
    pixel[0]=((color>>16)&255)*alpha/255;pixel[1]=((color>>8)&255)*alpha/255;
    pixel[2]=(color&255)*alpha/255;pixel[3]=alpha;
  }
  cached=CGBitmapContextCreateImage(bitmap);CGContextRelease(bitmap);
  if(band)_waterBand=cached;else _waterGlow=cached;return cached;
}

- (jboolean)waterBandWithFloatArray:(IOSFloatArray *)xy withFloat:(jfloat)radius withInt:(jint)alpha {
  return [self waterBandWithFloatArray:xy withFloat:radius withInt:alpha withFloat:0];
}

- (jboolean)waterBandWithFloatArray:(IOSFloatArray *)xy withFloat:(jfloat)radius withInt:(jint)alpha withFloat:(jfloat)mono {
  if(!self.context || !xy || xy->size_<4 || (xy->size_&1) || radius<=0)return NO;
  const jfloat *v=xy->buffer_;
  for(jint i=0;i<xy->size_-2;i+=2)if(v[i+2]<=v[i])return NO;
  CGImageRef image=[self waterImage:YES];if(!image)return NO;
  if(mono>0 && !_waterBandGray)_waterBandGray=DDGrayImage(image,_colorSpace);
  if(mono>0 && !_waterBandGray)return NO;
  CGContextRef ctx=self.context;CGContextSaveGState(ctx);
  CGContextSetAlpha(ctx,MAX(0,MIN(255,alpha))/255.0);
  CGContextSetInterpolationQuality(ctx,kCGInterpolationMedium);
  for(jint i=0;i<xy->size_-2;i+=2) {
    CGFloat width=v[i+2]-v[i],slope=(v[i+3]-v[i+1])/width;
    CGContextSaveGState(ctx);
    CGContextConcatCTM(ctx,CGAffineTransformMake(1,slope,0,1,v[i],v[i+1]-radius));
    DDDrawCachedImage(ctx,CGRectMake(0,0,width,radius*2),image,_waterBandGray,mono);
    CGContextRestoreGState(ctx);
  }
  CGContextRestoreGState(ctx);return YES;
}

- (jboolean)waterGlowWithFloat:(jfloat)x withFloat:(jfloat)y withFloat:(jfloat)rx
                    withFloat:(jfloat)ry withInt:(jint)alpha {
  return [self waterGlowWithFloat:x withFloat:y withFloat:rx withFloat:ry withInt:alpha withFloat:0];
}

- (jboolean)waterGlowWithFloat:(jfloat)x withFloat:(jfloat)y withFloat:(jfloat)rx
                    withFloat:(jfloat)ry withInt:(jint)alpha withFloat:(jfloat)mono {
  if(!self.context || rx<=0 || ry<=0)return NO;
  CGImageRef image=[self waterImage:NO];if(!image)return NO;
  if(mono>0 && !_waterGlowGray)_waterGlowGray=DDGrayImage(image,_colorSpace);
  if(mono>0 && !_waterGlowGray)return NO;
  CGContextSaveGState(self.context);
  CGContextSetAlpha(self.context,MAX(0,MIN(255,alpha))/255.0);
  CGContextSetInterpolationQuality(self.context,kCGInterpolationMedium);
  DDDrawCachedImage(self.context,CGRectMake(x-rx,y-ry,rx*2,ry*2),image,_waterGlowGray,mono);
  CGContextRestoreGState(self.context);return YES;
}

- (jboolean)reefVistaWithInt:(jint)art withFloat:(jfloat)x withFloat:(jfloat)y
                  withFloat:(jfloat)w withFloat:(jfloat)h withInt:(jint)alpha {
  return [self reefVistaWithInt:art withFloat:x withFloat:y withFloat:w withFloat:h withInt:alpha withFloat:0];
}

- (jboolean)reefVistaWithInt:(jint)art withFloat:(jfloat)x withFloat:(jfloat)y
                  withFloat:(jfloat)w withFloat:(jfloat)h withInt:(jint)alpha withFloat:(jfloat)mono {
  return [self reefVistaShadedWithInt:art withFloat:x withFloat:y withFloat:w withFloat:h withInt:alpha withFloat:1 withFloat:mono];
}

- (jboolean)reefVistaShadedWithInt:(jint)art withFloat:(jfloat)x withFloat:(jfloat)y
                  withFloat:(jfloat)w withFloat:(jfloat)h withInt:(jint)alpha withFloat:(jfloat)shade withFloat:(jfloat)mono {
  if(!self.context || _drawingForest || art<0 || art>=DDUnderwaterVista_COUNT || w<=0 || h<=0)return NO;
  if(!_reefVista[art]) {
    size_t width=[DDUnderwaterVista widthWithInt:art],height=[DDUnderwaterVista heightWithInt:art];
    CGContextRef bitmap=CGBitmapContextCreate(NULL,width,height,8,width*4,_colorSpace,
        kCGBitmapByteOrder32Big|kCGImageAlphaPremultipliedLast);
    if(!bitmap)return NO;
    CGContextRef prior=self.context;self.context=bitmap;_drawingForest=YES;
    [DDUnderwaterVista paintWithDDPainter:self withInt:art];
    _drawingForest=NO;self.context=prior;
    _reefVista[art]=CGBitmapContextCreateImage(bitmap);CGContextRelease(bitmap);
    if(!_reefVista[art])return NO;
  }
  if(mono>0 && !_reefVistaGray[art])_reefVistaGray[art]=DDGrayImage(_reefVista[art],_colorSpace);
  if(mono>0 && !_reefVistaGray[art])return NO;
  CGContextSaveGState(self.context);
  CGContextSetAlpha(self.context,MAX(0,MIN(255,alpha))/255.0);
  CGContextSetInterpolationQuality(self.context,kCGInterpolationMedium);
  CGRect rect=CGRectMake(x,y,w,h);
  shade=MAX(0,MIN(1,shade));
  if(shade<1)CGContextBeginTransparencyLayerWithRect(self.context,CGRectIntersection(rect,CGContextGetClipBoundingBox(self.context)),NULL);
  DDDrawCachedImage(self.context,rect,_reefVista[art],_reefVistaGray[art],mono);
  if(shade<1) {
    // Tint only the image's RGB, keeping its alpha and the horizon fade intact.
    CGContextSetBlendMode(self.context,kCGBlendModeSourceAtop);
    jint water=[DDUnderwaterVista waterColorWithFloat:mono];
    CGContextSetRGBFillColor(self.context,((water>>16)&255)/255.0,
        ((water>>8)&255)/255.0,(water&255)/255.0,1-shade);
    CGContextFillRect(self.context,rect);
    CGContextEndTransparencyLayer(self.context);
  }
  CGContextRestoreGState(self.context);return YES;
}

- (jboolean)reefRockWithInt:(jint)art withFloat:(jfloat)x withFloat:(jfloat)y
                 withFloat:(jfloat)w withFloat:(jfloat)h withInt:(jint)alpha {
  return [self reefRockWithInt:art withFloat:x withFloat:y withFloat:w withFloat:h withInt:alpha withFloat:0];
}

- (jboolean)reefRockWithInt:(jint)art withFloat:(jfloat)x withFloat:(jfloat)y
                 withFloat:(jfloat)w withFloat:(jfloat)h withInt:(jint)alpha withFloat:(jfloat)mono {
  if (!self.context || _drawingForest || art<0 || art>=DDUnderwater_ROCK_COUNT || w<=0 || h<=0)return NO;
  if (!_reef[art]) {
    size_t width=DDUnderwater_ROCK_WIDTH,height=DDUnderwater_ROCK_HEIGHT;
    CGContextRef bitmap=CGBitmapContextCreate(NULL,width,height,8,width*4,_colorSpace,
        kCGBitmapByteOrder32Big|kCGImageAlphaPremultipliedLast);
    if (!bitmap)return NO;
    CGContextRef prior=self.context;self.context=bitmap;_drawingForest=YES;
    [DDUnderwater paintRockWithDDPainter:self withInt:art];
    _drawingForest=NO;self.context=prior;
    _reef[art]=CGBitmapContextCreateImage(bitmap);CGContextRelease(bitmap);
    if (!_reef[art])return NO;
  }
  if(mono>0 && !_reefGray[art])_reefGray[art]=DDGrayImage(_reef[art],_colorSpace);
  if(mono>0 && !_reefGray[art])return NO;
  CGContextSaveGState(self.context);
  CGContextSetAlpha(self.context,MAX(0,MIN(255,alpha))/255.0);
  CGContextSetInterpolationQuality(self.context,kCGInterpolationMedium);
  DDDrawCachedImage(self.context,CGRectMake(x-w*1.3,y-h*1.35,w*3.1,h*2.95),_reef[art],_reefGray[art],mono);
  CGContextRestoreGState(self.context);return YES;
}

- (jboolean)crystalWithInt:(jint)layer withFloat:(jfloat)x withFloat:(jfloat)y
                withFloat:(jfloat)w withFloat:(jfloat)h withInt:(jint)alpha {
  return [self crystalWithInt:layer withFloat:x withFloat:y withFloat:w withFloat:h withInt:alpha withFloat:0];
}

- (jboolean)crystalWithInt:(jint)layer withFloat:(jfloat)x withFloat:(jfloat)y
                withFloat:(jfloat)w withFloat:(jfloat)h withInt:(jint)alpha withFloat:(jfloat)mono {
  if (!self.context || _drawingForest || ![DDCrystalArena validArtWithInt:layer] || w<=0 || h<=0)return NO;
  jint slot=layer%DDCrystalField_LAYERS;
  CGImageRef *image=layer<DDCrystalField_LAYERS?&_crystal[slot]:&_crystalArena[slot];
  CGImageRef *gray=layer<DDCrystalField_LAYERS?&_crystalGray[slot]:&_crystalArenaGray[slot];
  if (layer>=DDCrystalField_LAYERS && _crystalArenaArt[slot]!=layer) {
    CGImageRelease(*image);*image=NULL;CGImageRelease(*gray);*gray=NULL;_crystalArenaArt[slot]=layer;
  }
  if (!*image) {
    size_t width=DDCrystalField_STAMP_WIDTH,height=DDCrystalField_STAMP_HEIGHT;
    CGContextRef bitmap=CGBitmapContextCreate(NULL,width,height,8,width*4,_colorSpace,
        kCGBitmapByteOrder32Big|kCGImageAlphaPremultipliedLast);
    if (!bitmap)return NO;
    CGContextRef prior=self.context;self.context=bitmap;_drawingForest=YES;
    [DDCrystalField paintLayerWithDDPainter:self withInt:layer];
    _drawingForest=NO;self.context=prior;
    *image=CGBitmapContextCreateImage(bitmap);CGContextRelease(bitmap);
    if (!*image)return NO;
  }
  if(mono>0 && !*gray)*gray=DDGrayImage(*image,_colorSpace);
  if(mono>0 && !*gray)return NO;
  CGContextSaveGState(self.context);
  CGContextSetAlpha(self.context,MAX(0,MIN(255,alpha))/255.0);
  CGContextSetInterpolationQuality(self.context,kCGInterpolationMedium);
  DDDrawCachedImage(self.context,CGRectMake(x,y,w,h),*image,*gray,mono);
  CGContextRestoreGState(self.context);return YES;
}

- (CGImageRef)basinImage:(jint)art {
  if (art<0 || art>=DDSlimeBasinArt_COUNT)return NULL;
  if (!_basin[art]) {
    size_t w=[DDSlimeBasinArt widthWithInt:art],h=[DDSlimeBasinArt heightWithInt:art];
    CGContextRef bitmap=CGBitmapContextCreate(NULL,w,h,8,w*4,_colorSpace,
        kCGBitmapByteOrder32Big|kCGImageAlphaPremultipliedLast);
    if (!bitmap)return NULL;
    CGContextRef prior=self.context;self.context=bitmap;_drawingForest=YES;
    [DDSlimeBasinArt paintWithDDPainter:self withInt:art withFloat:w withFloat:h];
    _drawingForest=NO;self.context=prior;
    _basin[art]=CGBitmapContextCreateImage(bitmap);CGContextRelease(bitmap);
    // Warm the matching surface highlights with the scene, before the first combat impact.
    if (art==DDSlimeBasinArt_SCENE)[self basinImage:DDSlimeBasinArt_REAR_LIT];
    else if (art==DDSlimeBasinArt_LEFT_BANK)[self basinImage:DDSlimeBasinArt_LEFT_LIT];
    else if (art==DDSlimeBasinArt_RIGHT_BANK)[self basinImage:DDSlimeBasinArt_RIGHT_LIT];
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
  if (!self.context || _drawingForest || [DDSlimeBasinArt sideWithInt:art]<0 || !offsets
      || offsets->size_<2 || offsets->size_>17 || w<=0 || h<=0)return NO;
  CGImageRef image=[self basinImage:art];if (!image)return NO;
  CGContextRef ctx=self.context;int rows=offsets->size_-1;BOOL left=[DDSlimeBasinArt sideWithInt:art]==0;
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
  CGImageRelease(_waterBand);CGImageRelease(_waterGlow);
  CGImageRelease(_waterBandGray);CGImageRelease(_waterGlowGray);
  for(NSUInteger i=0;i<DDUnderwaterVista_COUNT;i++){CGImageRelease(_reefVista[i]);CGImageRelease(_reefVistaGray[i]);}
  for (NSUInteger i=0;i<DDUnderwater_ROCK_COUNT;i++){CGImageRelease(_reef[i]);CGImageRelease(_reefGray[i]);}
  for (NSUInteger i=0;i<DDCrystalField_LAYERS;i++){CGImageRelease(_crystal[i]);CGImageRelease(_crystalArena[i]);CGImageRelease(_crystalGray[i]);CGImageRelease(_crystalArenaGray[i]);}
  for (NSUInteger i=0;i<DDSlimeBasinArt_COUNT;i++) CGImageRelease(_basin[i]);
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
