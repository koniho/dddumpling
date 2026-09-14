#import <AVFoundation/AVFoundation.h>

/** A persistent graph for short effects. All methods run on the effects queue. */
@interface DDEffectMixer : NSObject
- (BOOL)prepare;
- (void)playBuffer:(AVAudioPCMBuffer *)buffer rate:(float)rate gain:(float)gain;
- (void)pause;
@end
