#import "DDRenderCheck.h"

#if DEBUG

#import <UIKit/UIKit.h>

#import "DDPainter.h"
#import "com/dddumpling/game/IOSGame.h"

static const CGFloat DDRenderCheckWidth = 640;
static const CGFloat DDRenderCheckHeight = 1400;
static const NSUInteger DDRenderCheckFrames = 30;
static const float DDRenderCheckStep = 1.0f / 60.0f;

static NSArray<NSString *> *DDRenderCheckScenes(void) {
  return @[
      @"title", @"play", @"stage:5", @"stage:10", @"stage:15", @"stage:20",
      @"case", @"stars", @"steamer",
  ];
}

static UIImage *DDRenderCheckImage(NSString *scene) {
  // Every scene owns a fresh core so the captured result cannot inherit RNG or
  // animation state from an earlier scene.
  DDIOSGame *game = [[DDIOSGame alloc] initWithDDGameCore_Store:nil
                                           withDDGameCore_Sound:nil
                                                       withLong:42];
  [game layoutWithFloat:DDRenderCheckWidth withFloat:DDRenderCheckHeight
              withFloat:0 withFloat:0 withFloat:0 withFloat:0];
  [game debugSceneWithNSString:scene];
  for (NSUInteger i = 0; i < DDRenderCheckFrames; i++) {
    [game updateWithFloat:DDRenderCheckStep];
  }

  UIGraphicsImageRendererFormat *format = [UIGraphicsImageRendererFormat defaultFormat];
  format.scale = 1;
  format.opaque = YES;
  UIGraphicsImageRenderer *renderer = [[UIGraphicsImageRenderer alloc]
      initWithSize:CGSizeMake(DDRenderCheckWidth, DDRenderCheckHeight) format:format];
  DDIOSPainter *painter = [[DDIOSPainter alloc] init];
  return [renderer imageWithActions:^(UIGraphicsImageRendererContext *rendererContext) {
    painter.context = rendererContext.CGContext;
    [game drawWithDDPainter:painter];
    painter.context = NULL;
  }];
}

void DDRunRenderChecks(void) {
  NSArray<NSURL *> *documents = [[NSFileManager defaultManager]
      URLsForDirectory:NSDocumentDirectory inDomains:NSUserDomainMask];
  NSURL *directory = [[documents firstObject] URLByAppendingPathComponent:@"render-check"
                                                                isDirectory:YES];
  NSError *error = nil;
  if (!directory || ![[NSFileManager defaultManager] createDirectoryAtURL:directory
                                                withIntermediateDirectories:YES
                                                                 attributes:nil error:&error]) {
    NSLog(@"DDD render checks could not create output directory: %@", error);
    return;
  }

  NSMutableString *manifest = [NSMutableString stringWithString:
      @"DDDUMPLING iOS renderer reference\n"
       "size=640x1400 points\n"
       "scale=1\n"
       "seed=42\n"
       "updates=30@1/60\n"
       "font=Bungee-Regular (native Core Text; Java raster reference uses its bitmap approximation)\n"
       "scenes=\n"];
  for (NSString *scene in DDRenderCheckScenes()) {
    @autoreleasepool {
      UIImage *image = DDRenderCheckImage(scene);
      NSData *png = UIImagePNGRepresentation(image);
      NSURL *file = [directory URLByAppendingPathComponent:[scene stringByAppendingString:@".png"]];
      if (png.length == 0 || ![png writeToURL:file options:NSDataWritingAtomic error:&error]) {
        NSLog(@"DDD render check %@ failed: %@", scene, error);
        continue;
      }
      [manifest appendFormat:@"%@.png bytes=%lu\n", scene, (unsigned long)png.length];
    }
  }
  NSURL *manifestURL = [directory URLByAppendingPathComponent:@"manifest.txt"];
  NSData *manifestData = [manifest dataUsingEncoding:NSUTF8StringEncoding];
  if (manifestData == nil || ![manifestData writeToURL:manifestURL
                                                options:NSDataWritingAtomic error:&error]) {
    NSLog(@"DDD render-check manifest failed: %@", error);
  } else {
    NSLog(@"DDD render checks wrote %@", directory.path);
  }
}
#endif
