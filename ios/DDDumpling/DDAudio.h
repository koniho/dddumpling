#import <Foundation/Foundation.h>
#import "com/dddumpling/game/GameCore.h"

/**
 * iOS implementation of the Java game's Sound seam.
 *
 * The procedural buffers remain in the translated Java Sfx and Music classes.  This
 * object only owns the platform mixer, speech voice, and audio-session lifecycle.
 */
@interface DDIOSAudio : NSObject <DDGameCore_Sound>

/** Enables or suspends output for the containing scene/app lifecycle. */
- (void)setActive:(BOOL)active;

@end
