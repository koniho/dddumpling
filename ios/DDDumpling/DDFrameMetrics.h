#import <Foundation/Foundation.h>

/**
 * Opt-in CPU and display-link cadence measurements for a 60 Hz game host.
 * These figures deliberately do not claim compositor FPS or physical-device
 * performance; they only make a simulator or device profiling session easier
 * to inspect from the Xcode console.
 */
@interface DDFrameMetrics : NSObject

- (instancetype)initWithEnabled:(BOOL)enabled;
- (void)reset;
- (void)recordDisplayLinkTimestamp:(CFTimeInterval)timestamp;
- (void)recordUpdateMilliseconds:(double)milliseconds;
- (void)recordDrawMilliseconds:(double)milliseconds;
- (void)recordTouchMilliseconds:(double)milliseconds;
- (void)recordHapticMilliseconds:(double)milliseconds;
- (BOOL)windowComplete;
- (void)logWindowWithDebugStatus:(nullable NSString *)debugStatus;

@end
