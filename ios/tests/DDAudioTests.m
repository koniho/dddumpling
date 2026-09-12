#import <XCTest/XCTest.h>
#import <AVFoundation/AVFoundation.h>

#import "DDAudio.h"
#import "com/dddumpling/game/Sfx.h"

@interface DDIOSAudio (TestHooks)
- (NSData *)wavForEffect:(jint)effect rate:(float)rate;
- (void)pausePlayers;
- (void)resumePlayersIfNeeded;
- (void)routeChanged:(NSNotification *)note;
- (void)playEffect:(jint)effect rate:(float)rate gain:(float)gain;
- (AVAudioPlayer *)effectPlayer:(jint)effect rate:(float)rate;
@end

@interface DDCountingPlayer : AVAudioPlayer
@property(nonatomic) NSInteger pauseCount;
@property(nonatomic) NSInteger stopCount;
@property(nonatomic) NSInteger playCount;
@property(nonatomic) NSInteger currentTimeWrites;
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
- (BOOL)play { ++_playCount; return YES; }
- (void)setCurrentTime:(NSTimeInterval)currentTime { (void)currentTime; ++_currentTimeWrites; }
@end

@interface DDAlwaysPlayingPlayer : DDCountingPlayer
@end
@implementation DDAlwaysPlayingPlayer
- (BOOL)isPlaying { return YES; }
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

- (uint32_t)sampleRateInWav:(NSData *)wav {
  uint32_t rate = 0;
  XCTAssertGreaterThanOrEqual(wav.length, (NSUInteger)44);
  [wav getBytes:&rate range:NSMakeRange(24, sizeof(rate))];
  return rate;
}

- (void)testPitchVariantsChangeTheWavSampleRate {
  DDIOSAudio *audio = [DDIOSAudio new];
  NSData *base = [audio wavForEffect:DDSfx_ZAP rate:1.f];
  NSData *raised = [audio wavForEffect:DDSfx_ZAP rate:1.5f];
  XCTAssertEqual([self sampleRateInWav:base], (uint32_t)DDSfx_RATE);
  XCTAssertEqual([self sampleRateInWav:raised], (uint32_t)(DDSfx_RATE * 1.5f));
}

- (void)testPauseStopsTransientPlayersAndSpeech {
  DDIOSAudio *audio = [DDIOSAudio new];
  DDCountingPlayer *music = [DDCountingPlayer new];
  DDCountingPlayer *bubble = [DDCountingPlayer new];
  DDCountingPlayer *effect = [DDCountingPlayer new];
  DDCountingSpeech *speech = [DDCountingSpeech new];
  [audio setValue:music forKey:@"music"];
  [audio setValue:bubble forKey:@"bubble"];
  [audio setValue:[NSMutableSet setWithObject:effect] forKey:@"effects"];
  [audio setValue:speech forKey:@"speech"];
  [audio setValue:@YES forKey:@"narrating"];

  [audio pausePlayers];

  XCTAssertEqual(music.pauseCount, 1);
  XCTAssertEqual(bubble.pauseCount, 1);
  XCTAssertEqual(effect.stopCount, 1);
  XCTAssertEqual(speech.stopCount, 1);
  XCTAssertFalse([[audio valueForKey:@"narrating"] boolValue]);
  XCTAssertEqual([[audio valueForKey:@"effects"] count], 0u);
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

- (void)testEffectPoolResetsPlayheadAndBoundsAbandonedPlayers {
  DDIOSAudio *audio = [DDIOSAudio new];
  DDCountingPlayer *reused = [DDCountingPlayer new];
  [audio setValue:@YES forKey:@"active"];
  [audio setValue:@YES forKey:@"playbackAllowed"];
  [audio setValue:[NSMutableSet setWithObject:reused] forKey:@"effects"];
  [audio setValue:[@{ @"14/22050": [NSMutableArray arrayWithObject:reused] } mutableCopy]
          forKey:@"effectPlayers"];
  [audio playEffect:DDSfx_ZAP rate:1 gain:1];
  XCTAssertEqual(reused.currentTimeWrites, 1);

  NSMutableArray *busy = [NSMutableArray array];
  DDAlwaysPlayingPlayer *victim = nil;
  for (NSUInteger i = 0; i < 6; ++i) {
    DDAlwaysPlayingPlayer *player = [DDAlwaysPlayingPlayer new];
    if (i == 0) victim = player;
    [busy addObject:player];
  }
  [audio setValue:[NSMutableSet setWithObject:victim] forKey:@"effects"];
  [audio setValue:[@{ @"14/22050": busy } mutableCopy] forKey:@"effectPlayers"];
  [audio effectPlayer:DDSfx_ZAP rate:1];
  XCTAssertLessThanOrEqual([(NSDictionary *)[audio valueForKey:@"effectPlayers"][@"14/22050"] count], 6u);
  XCTAssertFalse([[(NSSet *)[audio valueForKey:@"effects"] allObjects] containsObject:victim]);
}

@end
