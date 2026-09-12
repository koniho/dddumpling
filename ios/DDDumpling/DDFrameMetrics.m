#import "DDFrameMetrics.h"

#import <mach/mach.h>
#import <math.h>
#import <stdlib.h>

static const NSUInteger DDFrameMetricsWindowSize = 600;
static const double DDFrameMetricsTargetMilliseconds = 1000.0 / 60.0;

static int DDCompareDoubles(const void *left, const void *right) {
  const double a = *(const double *)left;
  const double b = *(const double *)right;
  return (a > b) - (a < b);
}

static NSString *DDStats(const double *values, NSUInteger count) {
  if (count == 0) return @"n=0";
  double sorted[DDFrameMetricsWindowSize];
  double sum = 0;
  double maximum = values[0];
  for (NSUInteger i = 0; i < count; i++) {
    sorted[i] = values[i];
    sum += values[i];
    maximum = MAX(maximum, values[i]);
  }
  qsort(sorted, count, sizeof(double), DDCompareDoubles);
  NSUInteger p95Index = (NSUInteger)ceil((double)count * 0.95) - 1;
  return [NSString stringWithFormat:@"mean=%.2f p95=%.2f max=%.2fms n=%lu",
          sum / count, sorted[p95Index], maximum, (unsigned long)count];
}

static uint64_t DDResidentBytes(void) {
  mach_task_basic_info_data_t info;
  mach_msg_type_number_t count = MACH_TASK_BASIC_INFO_COUNT;
  kern_return_t result = task_info(mach_task_self(), MACH_TASK_BASIC_INFO,
                                   (task_info_t)&info, &count);
  return result == KERN_SUCCESS ? (uint64_t)info.resident_size : 0;
}

@interface DDFrameMetrics () {
  BOOL _enabled;
  CFTimeInterval _lastDisplayLinkTimestamp;
  double _updateSamples[600];
  double _drawSamples[600];
  double _intervalSamples[600];
  NSUInteger _updateCount;
  NSUInteger _drawCount;
  NSUInteger _intervalCount;
  NSUInteger _missedDisplayLinkIntervals;
}
@end

@implementation DDFrameMetrics

- (instancetype)initWithEnabled:(BOOL)enabled {
  if ((self = [super init])) {
    _enabled = enabled;
    [self reset];
  }
  return self;
}

- (void)reset {
  _lastDisplayLinkTimestamp = 0;
  _updateCount = 0;
  _drawCount = 0;
  _intervalCount = 0;
  _missedDisplayLinkIntervals = 0;
}

- (void)recordDisplayLinkTimestamp:(CFTimeInterval)timestamp {
  if (!_enabled) return;
  if (_lastDisplayLinkTimestamp > 0) {
    const double milliseconds = (timestamp - _lastDisplayLinkTimestamp) * 1000.0;
    if (_intervalCount < DDFrameMetricsWindowSize) {
      _intervalSamples[_intervalCount++] = milliseconds;
    }
    // A cadence of roughly 1.5 or more target intervals indicates at least
    // one missed 60 Hz callback. This is host cadence, not presented frames.
    const NSInteger intervals = (NSInteger)llround(milliseconds / DDFrameMetricsTargetMilliseconds);
    if (intervals > 1) _missedDisplayLinkIntervals += (NSUInteger)(intervals - 1);
  }
  _lastDisplayLinkTimestamp = timestamp;
}

- (void)recordUpdateMilliseconds:(double)milliseconds {
  if (_enabled && _updateCount < DDFrameMetricsWindowSize) {
    _updateSamples[_updateCount++] = milliseconds;
  }
}

- (void)recordDrawMilliseconds:(double)milliseconds {
  if (_enabled && _drawCount < DDFrameMetricsWindowSize) {
    _drawSamples[_drawCount++] = milliseconds;
  }
}

- (BOOL)windowComplete {
  return _enabled && _drawCount == DDFrameMetricsWindowSize;
}

- (void)logWindowWithDebugStatus:(NSString *)debugStatus {
  if (![self windowComplete]) return;
  const uint64_t residentBytes = DDResidentBytes();
  NSString *status = debugStatus.length ? [NSString stringWithFormat:@" status=%@", debugStatus] : @"";
  NSLog(@"DDD profile cpu(update %@) cpu(draw-submit %@; deferred raster excluded) display-link(%@ missed=%lu target=60Hz; not compositor FPS) resident=%.1fMiB%@",
        DDStats(_updateSamples, _updateCount), DDStats(_drawSamples, _drawCount),
        DDStats(_intervalSamples, _intervalCount),
        (unsigned long)_missedDisplayLinkIntervals,
        residentBytes / (1024.0 * 1024.0), status);
  [self reset];
}

@end
