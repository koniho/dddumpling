#import <XCTest/XCTest.h>
#import <AVFoundation/AVFoundation.h>

#import "DDAudio.h"
#import "DDEffectMixer.h"
#import "com/dddumpling/game/Sfx.h"
#import "IOSPrimitiveArray.h"

@interface DDIOSAudio (TestHooks)
- (AVAudioPCMBuffer *)bufferForEffect:(jint)effect;
- (void)pausePlayers;
- (void)resumePlayersIfNeeded;
- (void)routeChanged:(NSNotification *)note;
- (void)playEffect:(jint)effect rate:(float)rate gain:(float)gain;
@end

@interface DDCountingPlayer : AVAudioPlayer
@property(nonatomic) NSInteger pauseCount;
@property(nonatomic) NSInteger stopCount;
@property(nonatomic) NSInteger playCount;
@property(nonatomic) NSInteger currentTimeWrites;
@property(nonatomic) BOOL playedOnMainThread;
@end
@implementation DDCountingPlayer
- (instancetype)init {
  uint8_t wav[] = {
      'R','I','F','F', 38,0,0,0, 'W','A','V','E', 'f','m','t',' ', 16,0,0,0,
      1,0, 1,0, 34,86,0,0, 68,172,0,0, 2,0, 16,0, 'd','a','t','a', 2,0,0,0,
      0,0 };
  return [super initWithData:[NSData dataWithBytes:wav length:sizeof(wav)] error:nil];
}
- (void)pause { ++_pauseCount; }
- (void)stop { ++_stopCount; }
- (BOOL)play { ++_playCount; _playedOnMainThread = NSThread.isMainThread; return YES; }
- (void)setCurrentTime:(NSTimeInterval)currentTime { (void)currentTime; ++_currentTimeWrites; }
@end

@interface DDCountingMixer : DDEffectMixer
@property(nonatomic) NSUInteger playCount;
@property(nonatomic) NSUInteger pauseCount;
@property(nonatomic) BOOL playedOnMainThread;
@property(nonatomic) float rate;
@property(nonatomic) float gain;
@end
@implementation DDCountingMixer
- (BOOL)prepare { return YES; }
- (void)pause { ++_pauseCount; }
- (void)playBuffer:(AVAudioPCMBuffer *)buffer rate:(float)rate gain:(float)gain {
  ++_playCount; _rate = rate; _gain = gain; _playedOnMainThread = NSThread.isMainThread;
}
@end

@interface DDCountingPlayerNode : AVAudioPlayerNode
@property(nonatomic) NSInteger playCount;
@property(nonatomic) NSInteger pauseCount;
@end
@implementation DDCountingPlayerNode
- (void)play { ++_playCount; }
- (void)pause { ++_pauseCount; }
@end

@interface DDCountingSpeech : AVSpeechSynthesizer
@property(nonatomic) NSInteger stopCount;
@end
@implementation DDCountingSpeech
- (BOOL)stopSpeakingAtBoundary:(AVSpeechBoundary)boundary { (void)boundary; ++_stopCount; return YES; }
@end

@interface DDAudioTests : XCTestCase
@end

@implementation DDAudioTests

- (void)testEffectBufferPreservesSynthesizedPCM {
  DDIOSAudio *audio = [DDIOSAudio new];
  AVAudioPCMBuffer *buffer = [audio bufferForEffect:DDSfx_ZAP];
  IOSShortArray *pcm = [DDSfx buildWithInt:DDSfx_ZAP];
  XCTAssertEqual(buffer.format.sampleRate, DDSfx_RATE);
  XCTAssertEqual(buffer.frameLength, pcm->size_);
  for (jint i = 0; i < pcm->size_; ++i)
    XCTAssertEqual(buffer.floatChannelData[0][i], pcm->buffer_[i] / 32768.f);
}

- (void)testPauseStopsTransientPlayersAndSpeech {
  DDIOSAudio *audio = [DDIOSAudio new];
  DDCountingPlayer *music = [DDCountingPlayer new];
  DDCountingPlayer *bubble = [DDCountingPlayer new];
  DDCountingMixer *effect = [DDCountingMixer new];
  DDCountingSpeech *speech = [DDCountingSpeech new];
  [audio setValue:music forKey:@"music"];
  [audio setValue:bubble forKey:@"bubble"];
  [audio setValue:effect forKey:@"effectMixer"];
  [audio setValue:speech forKey:@"speech"];
  [audio setValue:@YES forKey:@"narrating"];

  [audio pausePlayers];
  dispatch_sync([audio valueForKey:@"effectsQueue"], ^{});

  XCTAssertEqual(music.pauseCount, 1);
  XCTAssertEqual(bubble.pauseCount, 1);
  XCTAssertEqual(effect.pauseCount, 1);
  XCTAssertEqual(speech.stopCount, 1);
  XCTAssertFalse([[audio valueForKey:@"narrating"] boolValue]);
}

- (void)testRouteRemovalBlocksLaterLoopRestartsUntilActivation {
  DDIOSAudio *audio = [DDIOSAudio new];
  DDCountingPlayer *music = [DDCountingPlayer new];
  DDCountingPlayer *bubble = [DDCountingPlayer new];
  DDCountingPlayerNode *rocket = [DDCountingPlayerNode new];
  [audio setValue:@YES forKey:@"active"];
  [audio setValue:@YES forKey:@"playbackAllowed"];
  [audio setValue:music forKey:@"music"];
  [audio setValue:bubble forKey:@"bubble"];
  [audio setValue:rocket forKey:@"rocketNode"];
  [audio setValue:@YES forKey:@"rocketOn"];
  [audio setValue:@YES forKey:@"bubbleOn"];

  [audio routeChanged:[NSNotification notificationWithName:AVAudioSessionRouteChangeNotification
      object:AVAudioSession.sharedInstance userInfo:@{
          AVAudioSessionRouteChangeReasonKey: @(AVAudioSessionRouteChangeReasonOldDeviceUnavailable)}]];
  [audio resumePlayersIfNeeded];
  [audio rocketWithFloat:1];
  [audio bossChargeWithFloat:1];

  XCTAssertFalse([[audio valueForKey:@"playbackAllowed"] boolValue]);
  XCTAssertEqual(music.playCount, 0);
  XCTAssertEqual(bubble.playCount, 0);
  XCTAssertEqual(rocket.playCount, 0);
}

- (void)testZeroBossChargeImmediatelyStopsAndDisarmsBubble {
  DDIOSAudio *audio = [DDIOSAudio new];
  DDCountingPlayer *bubble = [DDCountingPlayer new];
  [audio setValue:bubble forKey:@"bubble"];
  [audio setValue:@YES forKey:@"bubbleOn"];
  [audio setValue:@(.08f) forKey:@"bubbleVolume"];

  [audio bossChargeWithFloat:0];

  XCTAssertEqual(bubble.stopCount, 1);
  XCTAssertFalse([[audio valueForKey:@"bubbleOn"] boolValue]);
  XCTAssertEqualWithAccuracy([[audio valueForKey:@"bubbleVolume"] floatValue], 0, .0001f);
}

- (void)testRocketUsesAStartedFloatVarispeedEngine {
  DDIOSAudio *audio = [DDIOSAudio new];
  [audio setActive:YES];
  [audio rocketWithFloat:.5f];
  AVAudioEngine *engine = [audio valueForKey:@"rocketEngine"];
  XCTAssertNotNil(engine);
  XCTAssertNotNil([audio valueForKey:@"rocketPitch"]);
  XCTAssertTrue(engine.isRunning);
  [audio setActive:NO];
}

- (void)testEffectWorkerPreservesPitchAndGain {
  DDIOSAudio *audio = [DDIOSAudio new];
  // Cold synthesis on a loaded simulator can consume the 100 ms stale-impact window.
  dispatch_sync([audio valueForKey:@"renderQueue"], ^{});
  DDCountingMixer *mixer = [DDCountingMixer new];
  [audio setValue:@YES forKey:@"active"];
  [audio setValue:@YES forKey:@"playbackAllowed"];
  [audio setValue:mixer forKey:@"effectMixer"];
  [audio playEffect:DDSfx_ZAP rate:1.5 gain:.7];
  dispatch_sync([audio valueForKey:@"effectsQueue"], ^{});
  XCTAssertEqual(mixer.playCount, 1u);
  XCTAssertEqualWithAccuracy(mixer.rate, 1.5f, .0001f);
  XCTAssertEqualWithAccuracy(mixer.gain, .7f, .0001f);
  XCTAssertFalse(mixer.playedOnMainThread);
}

- (void)testPendingEffectsDoNotBlockInputAndAreCancelledByPause {
  DDIOSAudio *audio = [DDIOSAudio new];
  dispatch_sync([audio valueForKey:@"renderQueue"], ^{});
  DDCountingMixer *player = [DDCountingMixer new];
  [audio setValue:@YES forKey:@"active"];
  [audio setValue:player forKey:@"effectMixer"];
  dispatch_queue_t queue = [audio valueForKey:@"effectsQueue"];
  dispatch_semaphore_t gate = dispatch_semaphore_create(0);
  dispatch_async(queue, ^{ dispatch_semaphore_wait(gate, DISPATCH_TIME_FOREVER); });
  // A blocked worker must not hold up input, even after all pending slots fill.
  for (NSUInteger i = 0; i < 100; ++i) [audio playEffect:DDSfx_ZAP rate:1 gain:1];
  [audio pausePlayers];
  dispatch_semaphore_signal(gate);
  dispatch_sync(queue, ^{});
  XCTAssertEqual(player.playCount, 0);
  [audio playEffect:DDSfx_ZAP rate:1 gain:1];
  dispatch_sync(queue, ^{});
  XCTAssertEqual(player.playCount, 1);
  XCTAssertFalse(player.playedOnMainThread);
}

@end
