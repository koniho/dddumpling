#import "DDAudio.h"
#import "DDEffectMixer.h"

#import <AVFoundation/AVFoundation.h>
#import <UIKit/UIKit.h>
#import <math.h>

#import "com/dddumpling/game/Sfx.h"
#import "com/dddumpling/game/Music.h"
#import "com/dddumpling/game/Narration.h"
#import "IOSPrimitiveArray.h"

static const double DDBoltPopInterval = 0.165;
static const jint DDStyleSwing = DDMusic_SWING_STYLE;
static const jint DDStyleOff = DDMusic_OFF;
static const jint DDStyleCustom = DDMusic_CUSTOM;

@interface DDIOSAudio () <AVAudioPlayerDelegate, AVSpeechSynthesizerDelegate>
@property(nonatomic) NSCache<NSNumber *, AVAudioPCMBuffer *> *effectBuffers;
@property(nonatomic) DDEffectMixer *effectMixer;
@property(nonatomic) AVAudioPlayer *music;
@property(nonatomic) AVAudioPlayer *bubble;
@property(nonatomic) AVAudioEngine *rocketEngine;
@property(nonatomic) AVAudioPlayerNode *rocketNode;
@property(nonatomic) AVAudioUnitVarispeed *rocketPitch;
@property(nonatomic) AVSpeechSynthesizer *speech;
@property(nonatomic) dispatch_queue_t renderQueue;
@property(nonatomic) dispatch_queue_t effectsQueue;
@property(nonatomic) dispatch_semaphore_t effectSlots;
@property(atomic) NSUInteger effectGeneration;
#if DEBUG
@property(nonatomic) BOOL profileMuteEffects;
#endif
@property(nonatomic) BOOL active;
@property(nonatomic) BOOL interrupted;
/** A route/privacy interruption can only be cleared by iOS permission or app reactivation. */
@property(nonatomic) BOOL playbackAllowed;
@property(nonatomic) BOOL needsResume;
@property(nonatomic) BOOL boss;
@property(nonatomic) BOOL frenzy;
@property(nonatomic) BOOL rocketOn;
@property(nonatomic) BOOL bubbleOn;
@property(nonatomic) BOOL narrating;
@property(nonatomic) NSInteger selectedStyle;
@property(nonatomic) NSUInteger musicGeneration;
@property(nonatomic) float bubbleVolume;
@property(nonatomic) CFTimeInterval boltPopUntil;
@end

@implementation DDIOSAudio

- (instancetype)init {
  if ((self = [super init])) {
    _effectBuffers = [[NSCache alloc] init];
    _effectMixer = [DDEffectMixer new];
    _speech = [[AVSpeechSynthesizer alloc] init];
    _speech.delegate = self;
    _renderQueue = dispatch_queue_create("com.dddumpling.audio.render", DISPATCH_QUEUE_SERIAL);
    _effectsQueue = dispatch_queue_create("com.dddumpling.audio.effects", DISPATCH_QUEUE_SERIAL);
    _effectSlots = dispatch_semaphore_create(12);
#if DEBUG
    _profileMuteEffects = [NSProcessInfo.processInfo.environment[@"DDD_PROFILE_MUTE_EFFECTS"] boolValue];
#endif
    _selectedStyle = DDStyleSwing;
    _playbackAllowed = YES;
    [self observeAudioSession];
    [self warmBuffers];
  }
  return self;
}

- (void)dealloc {
  [[NSNotificationCenter defaultCenter] removeObserver:self];
  [_music stop]; [_rocketNode stop]; [_rocketEngine stop]; [_bubble stop];
  [_speech stopSpeakingAtBoundary:AVSpeechBoundaryImmediate];
}

- (void)observeAudioSession {
  NSNotificationCenter *center = NSNotificationCenter.defaultCenter;
  [center addObserver:self selector:@selector(interruption:)
                name:AVAudioSessionInterruptionNotification object:AVAudioSession.sharedInstance];
  [center addObserver:self selector:@selector(routeChanged:)
                name:AVAudioSessionRouteChangeNotification object:AVAudioSession.sharedInstance];
  [center addObserver:self selector:@selector(enteredBackground:)
                name:UIApplicationDidEnterBackgroundNotification object:nil];
}

- (void)warmBuffers {
  dispatch_async(_renderQueue, ^{
    @autoreleasepool {
      for (jint i = 0; i < DDSfx_COUNT; ++i) {
        [self bufferForEffect:i];
      }
      [DDSfx rocket]; [DDSfx bubble];
      [DDMusic preRenderWithInt:DDStyleSwing];
    }
  });
}

- (void)setActive:(BOOL)active {
  _active = active;
  if (!active) {
    _needsResume = _music.isPlaying || _rocketNode.isPlaying || _bubble.isPlaying;
    [self pausePlayers];
    [AVAudioSession.sharedInstance setActive:NO
                         withOptions:AVAudioSessionSetActiveOptionNotifyOthersOnDeactivation error:nil];
    return;
  }
  // A foreground activation is an explicit player action after an unplugged route.
  _playbackAllowed = YES;
  [self activateSession];
  if (!_interrupted) [self resumePlayersIfNeeded];
}

- (void)activateSession {
  AVAudioSession *session = AVAudioSession.sharedInstance;
  [session setCategory:AVAudioSessionCategoryAmbient
                  mode:AVAudioSessionModeDefault
               options:AVAudioSessionCategoryOptionMixWithOthers error:nil];
  [session setActive:YES error:nil];
}

- (void)enteredBackground:(NSNotification *)note {
  (void)note;
  if (_active) {
    _needsResume = _music.isPlaying || _rocketNode.isPlaying || _bubble.isPlaying;
    [self pausePlayers];
  }
}

- (void)interruption:(NSNotification *)note {
  NSDictionary *info = note.userInfo;
  AVAudioSessionInterruptionType type = [info[AVAudioSessionInterruptionTypeKey] unsignedIntegerValue];
  if (type == AVAudioSessionInterruptionTypeBegan) {
    _interrupted = YES;
    _needsResume = _music.isPlaying || _rocketNode.isPlaying || _bubble.isPlaying;
    [self pausePlayers];
    return;
  }
  _interrupted = NO;
  AVAudioSessionInterruptionOptions options = [info[AVAudioSessionInterruptionOptionKey] unsignedIntegerValue];
  if (_active && (options & AVAudioSessionInterruptionOptionShouldResume)) {
    _playbackAllowed = YES;
    [self activateSession];
    [self resumePlayersIfNeeded];
  } else {
    _playbackAllowed = NO;
  }
}

- (void)routeChanged:(NSNotification *)note {
  AVAudioSessionRouteChangeReason reason = [note.userInfo[AVAudioSessionRouteChangeReasonKey] unsignedIntegerValue];
  // Removing headphones is a deliberate route change; do not unexpectedly resume on speaker.
  if (reason == AVAudioSessionRouteChangeReasonOldDeviceUnavailable) {
    _playbackAllowed = NO;
    _needsResume = NO;
    [self pausePlayers];
  }
}

- (void)pausePlayers {
  [_music pause]; [_rocketNode pause]; [_rocketEngine pause]; [_bubble pause];
  self.effectGeneration += 1;
  dispatch_async(_effectsQueue, ^{
    [self.effectMixer pause];
  });
  [_speech stopSpeakingAtBoundary:AVSpeechBoundaryImmediate];
  _narrating = NO;
  [self applyMusicMix];
}

- (void)resumePlayersIfNeeded {
  if (!_active || _interrupted || !_playbackAllowed) return;
  NSUInteger generation = self.effectGeneration;
  dispatch_async(_effectsQueue, ^{
    if (generation == self.effectGeneration) [self.effectMixer prepare];
  });
  if (_music && !_music.isPlaying && _selectedStyle != DDStyleOff) [_music play];
  if (_rocketOn && _rocketNode && !_rocketNode.isPlaying) {
    NSError *error = nil;
    [_rocketEngine startAndReturnError:&error];
    if (!error) [_rocketNode play];
  }
  if (_bubbleOn && _bubble && !_bubble.isPlaying) [_bubble play];
  _needsResume = NO;
}

- (NSData *)wavData:(IOSShortArray *)pcm sampleRate:(uint32_t)sampleRate {
  if (!pcm || pcm->size_ == 0) return nil;
  uint32_t frames = (uint32_t)pcm->size_;
  uint32_t bytes = frames * sizeof(int16_t);
  uint32_t riffSize = 36 + bytes;
  NSMutableData *out = [NSMutableData dataWithCapacity:44 + bytes];
  const char riff[] = "RIFF", wave[] = "WAVE", fmt[] = "fmt ", data[] = "data";
  uint32_t fmtSize = 16, rate = sampleRate, byteRate = sampleRate * 2, dataSize = bytes;
  uint16_t pcmTag = 1, channels = 1, bits = 16, block = 2;
  [out appendBytes:riff length:4]; [out appendBytes:&riffSize length:4];
  [out appendBytes:wave length:4]; [out appendBytes:fmt length:4]; [out appendBytes:&fmtSize length:4];
  [out appendBytes:&pcmTag length:2]; [out appendBytes:&channels length:2];
  [out appendBytes:&rate length:4]; [out appendBytes:&byteRate length:4];
  [out appendBytes:&block length:2]; [out appendBytes:&bits length:2];
  [out appendBytes:data length:4]; [out appendBytes:&dataSize length:4];
  [out appendBytes:pcm->buffer_ length:bytes];
  return out;
}

- (AVAudioPlayer *)playerForData:(NSData *)data loop:(BOOL)loop {
  if (!data) return nil;
  AVAudioPlayer *player = [[AVAudioPlayer alloc] initWithData:data error:nil];
  player.delegate = self;
  player.enableRate = YES;
  player.numberOfLoops = loop ? -1 : 0;
  [player prepareToPlay];
  return player;
}

- (AVAudioPCMBuffer *)bufferForEffect:(jint)effect {
  AVAudioPCMBuffer *buffer = [_effectBuffers objectForKey:@(effect)];
  if (buffer) return buffer;
  IOSShortArray *pcm = [DDSfx buildWithInt:effect];
  if (!pcm || !pcm->size_) return nil;
  AVAudioFormat *format = [[AVAudioFormat alloc] initStandardFormatWithSampleRate:DDSfx_RATE channels:1];
  buffer = [[AVAudioPCMBuffer alloc] initWithPCMFormat:format frameCapacity:(AVAudioFrameCount)pcm->size_];
  buffer.frameLength = (AVAudioFrameCount)pcm->size_;
  for (jint i = 0; i < pcm->size_; ++i) buffer.floatChannelData[0][i] = pcm->buffer_[i] / 32768.f;
  [_effectBuffers setObject:buffer forKey:@(effect)];
  return buffer;
}

- (AVAudioPlayer *)playerForPCM:(IOSShortArray *)pcm loop:(BOOL)loop {
  return [self playerForData:[self wavData:pcm sampleRate:DDSfx_RATE] loop:loop];
}

- (void)playEffect:(jint)effect rate:(float)rate gain:(float)gain {
#if DEBUG
  if (_profileMuteEffects) return;
#endif
  if (!_active || _interrupted || !_playbackAllowed) return;
  if (dispatch_semaphore_wait(_effectSlots, DISPATCH_TIME_NOW) != 0) return;
  NSUInteger generation = self.effectGeneration;
  CFTimeInterval requested = CACurrentMediaTime();
  dispatch_async(_effectsQueue, ^{
    @try {
      // Drop stale impacts rather than replaying a backlog after a pause or slow audio call.
      if (generation != self.effectGeneration || CACurrentMediaTime() - requested > .1) return;
      AVAudioPCMBuffer *buffer = [self bufferForEffect:effect];
      if (!buffer || ![self.effectMixer prepare]) return;
      if (generation != self.effectGeneration || CACurrentMediaTime() - requested > .1) return;
      [self.effectMixer playBuffer:buffer rate:rate gain:gain];
    } @catch (NSException *exception) {
      // Audio is optional. A translated synthesis error must not affect gameplay.
    } @finally {
      dispatch_semaphore_signal(self.effectSlots);
    }
  });
}

- (void)applyMusicMix {
  float gain = _boss ? DDMusic_BOSS_GAIN : (_selectedStyle == DDStyleCustom ? 0.55f : 1.f);
  if (_rocketOn) gain *= 0.68f;
  if (_narrating) gain *= 0.22f;
  _music.volume = gain;
}

- (NSURL *)customMusicURL {
  for (NSString *extension in @[@"m4a", @"mp3", @"wav", @"aiff", @"caf"]) {
    NSURL *url = [NSBundle.mainBundle URLForResource:@"bgm" withExtension:extension];
    if (url) return url;
  }
  return nil;
}

- (void)rebuildMusic {
  ++_musicGeneration;
  NSUInteger generation = _musicGeneration;
  [_music stop]; _music = nil;
  if (_selectedStyle == DDStyleOff) return;
  BOOL boss = _boss, frenzy = _frenzy;
  NSInteger style = _selectedStyle;
  NSURL *customURL = style == DDStyleCustom && !boss && !frenzy ? [self customMusicURL] : nil;
  if (customURL) {
    AVAudioPlayer *player = [[AVAudioPlayer alloc] initWithContentsOfURL:customURL error:nil];
    if (player && generation == _musicGeneration) {
      player.numberOfLoops = -1; player.enableRate = YES; player.delegate = self;
      _music = player; [self applyMusicMix];
      if (_active && !_interrupted && _playbackAllowed) [player play];
    }
    return;
  }
  dispatch_async(_renderQueue, ^{
    IOSShortArray *pcm = nil;
    @try {
      jint synthStyle = style >= 0 && style < DDStyleOff ? (jint)style : DDStyleSwing;
      pcm = boss ? [DDMusic bossLoopWithInt:synthStyle]
                 : [DDMusic loopWithInt:synthStyle withBoolean:frenzy];
    } @catch (NSException *exception) { return; }
    dispatch_async(dispatch_get_main_queue(), ^{
      if (generation != self.musicGeneration || self.selectedStyle == DDStyleOff) return;
      AVAudioPlayer *player = [self playerForPCM:pcm loop:YES];
      if (!player) return;
      self.music = player; [self applyMusicMix];
      if (self.active && !self.interrupted && self.playbackAllowed) [player play];
    });
  });
}

- (void)ensureRocket {
  if (_rocketNode) return;
  @try {
    IOSShortArray *pcm = [DDSfx rocket];
    AVAudioFormat *format = [[AVAudioFormat alloc] initWithCommonFormat:AVAudioPCMFormatFloat32
                                                               sampleRate:DDSfx_RATE channels:1 interleaved:NO];
    AVAudioPCMBuffer *buffer = [[AVAudioPCMBuffer alloc] initWithPCMFormat:format
                                                               frameCapacity:(AVAudioFrameCount)pcm->size_];
    buffer.frameLength = (AVAudioFrameCount)pcm->size_;
    float *samples = buffer.floatChannelData[0];
    for (jint i = 0; i < pcm->size_; ++i) samples[i] = pcm->buffer_[i] / 32768.f;
    _rocketEngine = [[AVAudioEngine alloc] init];
    _rocketNode = [[AVAudioPlayerNode alloc] init];
    _rocketPitch = [[AVAudioUnitVarispeed alloc] init];
    [_rocketEngine attachNode:_rocketNode];
    [_rocketEngine attachNode:_rocketPitch];
    [_rocketEngine connect:_rocketNode to:_rocketPitch format:format];
    [_rocketEngine connect:_rocketPitch to:_rocketEngine.mainMixerNode format:format];
    [_rocketNode scheduleBuffer:buffer atTime:nil options:AVAudioPlayerNodeBufferLoops completionHandler:nil];
    [_rocketEngine prepare];
  } @catch (NSException *exception) { _rocketNode = nil; _rocketEngine = nil; _rocketPitch = nil; }
}

- (void)ensureBubble {
  if (_bubble) return;
  @try { _bubble = [self playerForPCM:[DDSfx bubble] loop:YES]; } @catch (NSException *exception) { }
}

// GameCore.Sound -----------------------------------------------------------

- (void)squishWithInt:(jint)glyph withInt:(jint)depth {
  [self playEffect:DDSfx_SQUISH_0 + MAX(0, MIN(5, glyph))
               rate:powf(.92f, MAX(0, depth - 1)) gain:1.f];
}
- (void)clearWord { [self playEffect:DDSfx_CLEAR rate:1 gain:1]; }
- (void)shuffleBlip { [self playEffect:DDSfx_SHUFFLE_BLIP rate:1 gain:.3f]; }
- (void)slimeCoverWithBoolean:(jboolean)release { [self playEffect:release ? DDSfx_SLIME_RELEASE : DDSfx_SLIME_COVER rate:1 gain:.65f]; }
- (void)debuffDown { [self playEffect:DDSfx_DEBUFF_DOWN rate:1 gain:.8f]; }
- (void)linkedThud { [self playEffect:DDSfx_LINKED_THUD rate:1 gain:.85f]; }
- (void)wrong { [self playEffect:DDSfx_WRONG rate:1 gain:1]; }
- (void)damage { [self playEffect:DDSfx_DRIP rate:1 gain:1]; }
- (void)achievement { [self playEffect:DDSfx_ACHIEVEMENT rate:1 gain:1]; }
- (void)bossLaugh { [self playEffect:DDSfx_BOSS_LAUGH rate:1 gain:1]; }
- (void)bossDamage { [self playEffect:DDSfx_BOSS_DAMAGE rate:.92f gain:.72f]; }
- (void)slimeDamage { [self playEffect:DDSfx_SLIME_DAMAGE rate:1 gain:.76f]; }
- (void)bossSplit { [self playEffect:DDSfx_BOSS_SPLIT rate:1.08f gain:.68f]; }
- (void)divideDamage { [self playEffect:DDSfx_DIVIDE_DAMAGE rate:1 gain:.76f]; }
- (void)divideSplit { [self playEffect:DDSfx_DIVIDE_SPLIT rate:1 gain:.78f]; }
- (void)divideDeactivate { [self playEffect:DDSfx_DIVIDE_DEACTIVATE rate:1 gain:.82f]; }
- (void)divideBoingWithFloat:(jfloat)weight {
  float w = MAX(0.f, MIN(1.f, weight));
  jint sound = w >= .67f ? DDSfx_DIVIDE_BOING_HEAVY : w >= .34f ? DDSfx_DIVIDE_BOING_MEDIUM : DDSfx_DIVIDE_BOING_LIGHT;
  [self playEffect:sound rate:1 gain:.58f + .16f * w];
}
- (void)boltPop {
  CFTimeInterval now = CACurrentMediaTime();
  if (now < _boltPopUntil) return;
  _boltPopUntil = now + DDBoltPopInterval;
  [self playEffect:DDSfx_BOLT_POP rate:1 gain:.68f];
}
- (void)boltDeath { [self playEffect:DDSfx_BOLT_DEATH rate:1 gain:.82f]; }
- (void)shieldBounce { [self playEffect:DDSfx_SHIELD_BOUNCE rate:1 gain:.72f]; }
- (void)octoCue { [self playEffect:DDSfx_OCTO_CUE rate:1 gain:.74f]; }
- (void)octoLock { [self playEffect:DDSfx_OCTO_LOCK rate:1 gain:.70f]; }
- (void)mushroomShake { [self playEffect:DDSfx_MUSHROOM_SHAKE rate:1 gain:.78f]; }
- (void)mushroomSpore { [self playEffect:DDSfx_MUSHROOM_SPORE rate:1 gain:.72f]; }
- (void)chop { [self playEffect:DDSfx_CHOP rate:1 gain:1]; }
- (void)zapWithInt:(jint)hop { [self playEffect:DDSfx_ZAP rate:1 + .055f * MAX(0, MIN(8, hop - 1)) gain:1]; }
- (void)collectWithInt:(jint)nth { [self playEffect:DDSfx_COLLECT rate:1 + .05f * MAX(0, MIN(7, nth)) gain:1]; }
- (void)starWithInt:(jint)nth { [self playEffect:DDSfx_STAR rate:1 + .032f * MAX(0, MIN(16, nth - 1)) gain:1]; }
- (void)courseStart { [self playEffect:DDSfx_COURSE rate:1 gain:1]; }
- (void)tallyWithInt:(jint)nth { [self playEffect:DDSfx_TALLY rate:.94f + .018f * MAX(0, MIN(20, nth)) gain:1]; }
- (void)paradeJoin { [self playEffect:DDSfx_JOIN rate:1 gain:1]; }
- (void)rosterJoin { [self playEffect:DDSfx_ROSTER_JOIN rate:1 gain:1]; }
- (void)gameOver { [self playEffect:DDSfx_OVER rate:1 gain:1]; }
- (void)bossTauntWithInt:(jint)kind { if (kind >= 0 && kind < 4) [self playEffect:DDSfx_BOSS_TAUNT_0 + kind rate:1 gain:.78f]; }
- (void)gameStart { [self playEffect:DDSfx_START rate:1 gain:.72f]; }
- (void)stageClear { [self playEffect:DDSfx_STAGE_CLEAR rate:1 gain:1]; }
- (void)powerClear { [self playEffect:DDSfx_POWER_CLEAR rate:1 gain:1]; }

- (void)rocketWithFloat:(jfloat)thrust {
  if (thrust <= 0) { _rocketOn = NO; [_rocketNode pause]; [self applyMusicMix]; return; }
  [self ensureRocket];
  float p = MAX(0.f, MIN(1.f, thrust));
  _rocketNode.volume = .16f + .28f * p; _rocketPitch.rate = .82f + .43f * p; _rocketOn = YES;
  [self applyMusicMix];
  if (_active && !_interrupted && _playbackAllowed && !_rocketNode.isPlaying) {
    NSError *error = nil;
    [_rocketEngine startAndReturnError:&error];
    if (!error) [_rocketNode play];
  }
}

- (void)bossChargeWithFloat:(jfloat)charge {
  if (charge <= 0) {
    _bubbleVolume = 0;
    _bubble.volume = 0;
    [_bubble stop];
    _bubbleOn = NO;
    return;
  }
  [self ensureBubble];
  float target = MAX(0.f, MIN(1.f, charge)) * .10f;
  _bubbleVolume += (target - _bubbleVolume) * .18f;
  _bubble.volume = _bubbleVolume; _bubble.rate = 1; _bubbleOn = YES;
  if (_active && !_interrupted && _playbackAllowed && !_bubble.isPlaying) [_bubble play];
}

- (void)selectMusicWithInt:(jint)style { _selectedStyle = style; _boss = NO; [self rebuildMusic]; }
- (void)bossMusicWithBoolean:(jboolean)active { if (_boss == active) return; _boss = active; [self rebuildMusic]; }
- (void)frenzyWithBoolean:(jboolean)on { if (_frenzy == on) return; _frenzy = on; [self rebuildMusic]; }

- (void)narrateWithInt:(jint)entry {
  if (!_active || _interrupted || !_playbackAllowed) return;
  @try {
    IOSObjectArray *lines = [DDNarration linesWithInt:entry];
    if (!lines || lines->size_ == 0) return;
    [_speech stopSpeakingAtBoundary:AVSpeechBoundaryImmediate];
    _narrating = YES; [self applyMusicMix];
    for (jint i = 0; i < lines->size_; ++i) {
      NSString *text = [lines objectAtIndex:i];
      AVSpeechUtterance *utterance = [AVSpeechUtterance speechUtteranceWithString:text ?: @""];
      utterance.voice = [AVSpeechSynthesisVoice voiceWithLanguage:@"en-US"];
      utterance.pitchMultiplier = 1.9f;
      utterance.rate = i == 0 ? AVSpeechUtteranceDefaultSpeechRate * .90f : AVSpeechUtteranceDefaultSpeechRate;
      utterance.postUtteranceDelay = i == 0 ? .28 : i == 1 ? .20 : 0;
      [_speech speakUtterance:utterance];
    }
  } @catch (NSException *exception) { [self hush]; }
}

- (void)hush {
  [_speech stopSpeakingAtBoundary:AVSpeechBoundaryImmediate];
  _narrating = NO; [self applyMusicMix];
}

- (void)speechSynthesizer:(AVSpeechSynthesizer *)synthesizer didFinishSpeechUtterance:(AVSpeechUtterance *)utterance {
  (void)synthesizer; (void)utterance;
  if (!_speech.isSpeaking) { _narrating = NO; [self applyMusicMix]; }
}
- (void)speechSynthesizer:(AVSpeechSynthesizer *)synthesizer didCancelSpeechUtterance:(AVSpeechUtterance *)utterance {
  (void)synthesizer; (void)utterance; _narrating = NO; [self applyMusicMix];
}

@end
