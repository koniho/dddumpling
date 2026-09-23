#import <XCTest/XCTest.h>
#import <AVFoundation/AVFoundation.h>
#import <objc/runtime.h>

#import "DDAudio.h"
#import "DDEffectMixer.h"
#import "com/dddumpling/game/Sfx.h"
#import "com/dddumpling/game/Music.h"
#import "com/dddumpling/game/Narration.h"
#import "IOSPrimitiveArray.h"

@interface DDIOSAudio (TestHooks)
- (AVAudioPCMBuffer *)bufferForEffect:(jint)effect;
- (void)pausePlayers;
- (void)resumePlayersIfNeeded;
- (void)routeChanged:(NSNotification *)note;
- (void)playEffect:(jint)effect rate:(float)rate gain:(float)gain;
- (CFTimeInterval)effectTime;
- (void)warmBuffers;
@end

// Exercise queue routing/deadlines independently of simulator scheduling and warmup load.
@interface DDClockedAudio : DDIOSAudio
@property(atomic) CFTimeInterval now;
@end
@implementation DDClockedAudio
- (CFTimeInterval)effectTime { return self.now; }
- (void)warmBuffers {}
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
@property(nonatomic) NSInteger speakCount;
@property(nonatomic) AVSpeechUtterance *utterance;
@end
@implementation DDCountingSpeech
- (void)speakUtterance:(AVSpeechUtterance *)utterance { ++_speakCount; _utterance = utterance; }
- (BOOL)stopSpeakingAtBoundary:(AVSpeechBoundary)boundary { (void)boundary; ++_stopCount; return YES; }
@end

@interface DDAudioTests : XCTestCase
@end

@implementation DDAudioTests
- (void)testRunNameUsesAnnouncerDeliveryAndRespectsMute {
  DDClockedAudio *audio = [DDClockedAudio new];
  DDCountingSpeech *speech = [DDCountingSpeech new];
  [audio setValue:speech forKey:@"speech"];
  [audio setValue:@YES forKey:@"active"];
  [audio setValue:@0.4f forKey:@"effectsVolume"];
  [audio announceSquishyWithInt:11];
  XCTAssertEqual(speech.speakCount, 1);
  XCTAssertEqualObjects(speech.utterance.speechString, [DDNarration nameWithInt:11]);
  XCTAssertEqualWithAccuracy(speech.utterance.pitchMultiplier, DDNarration_NAME_PITCH, .001);
  XCTAssertEqualWithAccuracy(speech.utterance.rate, AVSpeechUtteranceDefaultSpeechRate * DDNarration_NAME_RATE, .001);
  XCTAssertEqualWithAccuracy(speech.utterance.volume, .4, .001);
  XCTAssertTrue([[audio valueForKey:@"narrating"] boolValue]);
  [audio hush];
  XCTAssertFalse([[audio valueForKey:@"narrating"] boolValue]);
  [audio setValue:@0 forKey:@"effectsVolume"];
  [audio announceSquishyWithInt:4];
  XCTAssertEqual(speech.speakCount, 1);
  [audio setValue:@1 forKey:@"effectsVolume"];
  [audio setValue:@NO forKey:@"active"];
  [audio announceSquishyWithInt:4];
  XCTAssertEqual(speech.speakCount, 1);
}

- (void)testRepeatedMusicSelectionPreservesPlayerAndPausedPosition {
  DDClockedAudio *audio = [DDClockedAudio new];
  [audio selectMusicWithInt:DDMusic_SWING_STYLE];
  DDCountingPlayer *player = [DDCountingPlayer new];
  [audio setValue:player forKey:@"music"];
  NSUInteger generation = [[audio valueForKey:@"musicGeneration"] unsignedIntegerValue];
  [audio selectMusicWithInt:DDMusic_SWING_STYLE];
  [audio pausePlayers];
  [audio selectMusicWithInt:DDMusic_SWING_STYLE];
  XCTAssertEqual([audio valueForKey:@"music"], player);
  XCTAssertEqual(player.stopCount, 0);
  XCTAssertEqual(player.currentTimeWrites, 0);
  XCTAssertEqual(player.playCount, 0);
  XCTAssertEqual([[audio valueForKey:@"musicGeneration"] unsignedIntegerValue], generation);
}

- (void)testDuplicateSelectionsKeepThePendingInitialRender {
  DDClockedAudio *audio = [DDClockedAudio new];
  dispatch_queue_t queue = [audio valueForKey:@"renderQueue"];
  dispatch_semaphore_t gate = dispatch_semaphore_create(0);
  dispatch_async(queue, ^{ dispatch_semaphore_wait(gate, DISPATCH_TIME_FOREVER); });
  [audio selectMusicWithInt:DDMusic_SWING_STYLE];
  NSUInteger generation = [[audio valueForKey:@"musicGeneration"] unsignedIntegerValue];
  [audio selectMusicWithInt:DDMusic_SWING_STYLE];
  XCTAssertGreaterThan(generation, 0u);
  XCTAssertNil([audio valueForKey:@"music"]);
  XCTAssertEqual([[audio valueForKey:@"musicGeneration"] unsignedIntegerValue], generation);
  dispatch_semaphore_signal(gate);
}

- (void)testDifferentStylesAndArrangementsStillReplaceMusic {
  DDClockedAudio *audio = [DDClockedAudio new];
  [audio selectMusicWithInt:DDMusic_SWING_STYLE];
  NSUInteger generation = [[audio valueForKey:@"musicGeneration"] unsignedIntegerValue];
  DDCountingPlayer *player = [DDCountingPlayer new];
  [audio setValue:player forKey:@"music"];
  [audio selectMusicWithInt:DDMusic_DRIFT];
  XCTAssertEqual(player.stopCount, 1);
  XCTAssertEqual([[audio valueForKey:@"musicGeneration"] unsignedIntegerValue], generation + 1);
  generation++;
  [audio frenzyWithBoolean:YES];
  XCTAssertEqual([[audio valueForKey:@"musicGeneration"] unsignedIntegerValue], generation + 1);
  generation++;
  [audio selectMusicWithInt:DDMusic_DRIFT];
  XCTAssertEqual([[audio valueForKey:@"musicGeneration"] unsignedIntegerValue], generation);
  [audio bossMusicWithBoolean:YES];
  XCTAssertEqual([[audio valueForKey:@"musicGeneration"] unsignedIntegerValue], generation + 1);
  generation++;
  [audio frenzyWithBoolean:NO]; // Boss arrangements do not depend on frenzy.
  XCTAssertEqual([[audio valueForKey:@"musicGeneration"] unsignedIntegerValue], generation);
  [audio selectMusicWithInt:DDMusic_DRIFT];
  XCTAssertEqual([[audio valueForKey:@"musicGeneration"] unsignedIntegerValue], generation + 1);
  generation++;
}

- (void)testNormalMusicReturnsAfterBandPlayback {
  DDClockedAudio *audio = [DDClockedAudio new];
  [audio selectMusicWithInt:DDMusic_SWING_STYLE];
  [audio bandStartWithInt:0 withBoolean:NO];
  NSUInteger generation = [[audio valueForKey:@"musicGeneration"] unsignedIntegerValue];
  [audio selectMusicWithInt:DDMusic_SWING_STYLE];
  XCTAssertEqual([[audio valueForKey:@"musicGeneration"] unsignedIntegerValue], generation);
  [audio bandStop];
  [audio selectMusicWithInt:DDMusic_SWING_STYLE];
  XCTAssertGreaterThan([[audio valueForKey:@"musicGeneration"] unsignedIntegerValue], generation);
  XCTAssertTrue([[audio valueForKey:@"musicRequested"] boolValue]);
}

- (void)testUserVolumesSurviveDuckingAndMuteChannelsIndependently {
  DDClockedAudio *audio = [DDClockedAudio new];
  DDCountingPlayer *music = [DDCountingPlayer new];
  DDCountingPlayer *bubble = [DDCountingPlayer new];
  DDCountingMixer *mixer = [DDCountingMixer new];
  [audio setValue:music forKey:@"music"];
  [audio setValue:bubble forKey:@"bubble"];
  [audio setValue:mixer forKey:@"effectMixer"];
  [audio setValue:@.1f forKey:@"bubbleVolume"];
  [audio volumesWithFloat:.4f withFloat:.25f];
  dispatch_queue_t queue = [audio valueForKey:@"effectsQueue"];
  dispatch_sync(queue, ^{});
  XCTAssertEqualWithAccuracy(music.volume, .4f, .001f);
  XCTAssertEqualWithAccuracy(bubble.volume, .025f, .001f);
  XCTAssertEqualWithAccuracy(mixer.volume, .25f, .001f);
  [audio setValue:@YES forKey:@"narrating"];
  [audio volumesWithFloat:.4f withFloat:.25f];
  XCTAssertEqualWithAccuracy(music.volume, .4f * .22f, .001f);
  [audio volumesWithFloat:.4f withFloat:0];
  dispatch_sync(queue, ^{});
  XCTAssertEqualWithAccuracy(mixer.volume, 0, .001f);
  XCTAssertEqualWithAccuracy(bubble.volume, 0, .001f);
  XCTAssertEqualWithAccuracy(music.volume, .4f, .001f);
  [audio volumesWithFloat:0 withFloat:1];
  dispatch_sync(queue, ^{});
  XCTAssertEqualWithAccuracy(music.volume, 0, .001f);
  XCTAssertEqualWithAccuracy(mixer.volume, 1, .001f);
}


- (void)testEverySharedSoundCallbackHasANativeImplementation {
  unsigned int count = 0;
  struct objc_method_description *methods = protocol_copyMethodDescriptionList(
      @protocol(DDGameCore_Sound), YES, YES, &count);
  XCTAssertGreaterThan(count, 0u);
  for (unsigned int i = 0; i < count; ++i)
    XCTAssertTrue([DDIOSAudio instancesRespondToSelector:methods[i].name],
                  @"Missing sound callback %@", NSStringFromSelector(methods[i].name));
  free(methods);
}

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
  DDClockedAudio *audio = [DDClockedAudio new];
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

- (void)testQueuedEffectsRespectTheStaleDeadline {
  for (NSNumber *delay in @[@.099, @.101]) {
    DDClockedAudio *audio = [DDClockedAudio new];
    DDCountingMixer *mixer = [DDCountingMixer new];
    [audio setValue:@YES forKey:@"active"];
    [audio setValue:mixer forKey:@"effectMixer"];
    dispatch_queue_t queue = [audio valueForKey:@"effectsQueue"];
    dispatch_semaphore_t gate = dispatch_semaphore_create(0);
    dispatch_async(queue, ^{ dispatch_semaphore_wait(gate, DISPATCH_TIME_FOREVER); });
    [audio playEffect:DDSfx_ZAP rate:1 gain:1];
    audio.now = delay.doubleValue;
    dispatch_semaphore_signal(gate);
    dispatch_sync(queue, ^{});
    XCTAssertEqual(mixer.playCount, delay.doubleValue < .1 ? 1u : 0u);
  }
}

- (void)testPendingEffectsDoNotBlockInputAndAreCancelledByPause {
  DDClockedAudio *audio = [DDClockedAudio new];
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
