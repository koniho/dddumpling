#import <XCTest/XCTest.h>
#import "DDEffectMixer.h"
#import "com/dddumpling/game/Sfx.h"

@interface DDEffectMixerTests : XCTestCase
@end

@implementation DDEffectMixerTests
- (void)testBurstAndPauseReuseOneBoundedGraph {
  DDEffectMixer *mixer = [DDEffectMixer new];
  XCTAssertTrue([mixer prepare]);
  AVAudioEngine *engine = [mixer valueForKey:@"engine"];
  NSArray<AVAudioPlayerNode *> *voices = [[mixer valueForKey:@"voices"] copy];
  NSArray<AVAudioUnitVarispeed *> *pitches = [mixer valueForKey:@"pitches"];
  XCTAssertEqual(voices.count, 12u);
  NSSet *nodes = [engine.attachedNodes copy];
  AVAudioFormat *format = [[AVAudioFormat alloc] initStandardFormatWithSampleRate:DDSfx_RATE channels:1];
  AVAudioPCMBuffer *buffer = [[AVAudioPCMBuffer alloc] initWithPCMFormat:format frameCapacity:2205];
  buffer.frameLength = 2205;
  for (AVAudioFrameCount i = 0; i < buffer.frameLength; ++i)
    buffer.floatChannelData[0][i] = .01f;
  for (NSUInteger i = 0; i < 120; ++i) [mixer playBuffer:buffer rate:1.5 gain:.7];
  XCTAssertEqualObjects(engine.attachedNodes, nodes);
  XCTAssertEqualObjects([mixer valueForKey:@"voices"], voices);
  for (NSUInteger i = 0; i < voices.count; ++i) {
    XCTAssertTrue(voices[i].isPlaying);
    XCTAssertEqualWithAccuracy(pitches[i].rate, 1.5, .0001);
    XCTAssertEqualWithAccuracy(voices[i].volume, .7, .0001);
  }
  [mixer pause];
  XCTAssertFalse(engine.isRunning);
  for (AVAudioPlayerNode *voice in voices) XCTAssertFalse(voice.isPlaying);
  [mixer playBuffer:buffer rate:.75 gain:.4];
  XCTAssertTrue(engine.isRunning);
  XCTAssertEqual([mixer valueForKey:@"engine"], engine);
  XCTAssertEqualObjects(engine.attachedNodes, nodes);
  XCTAssertTrue(voices[0].isPlaying);
  XCTAssertEqualWithAccuracy(pitches[0].rate, .75, .0001);
  [mixer pause];
}
@end
