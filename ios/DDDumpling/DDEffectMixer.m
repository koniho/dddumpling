#import "DDEffectMixer.h"
#import "com/dddumpling/game/Sfx.h"

static const NSUInteger DDVoiceCount = 12;

@interface DDEffectMixer ()
@property(nonatomic) AVAudioEngine *engine;
@property(nonatomic) NSMutableArray<AVAudioPlayerNode *> *voices;
@property(nonatomic) NSMutableArray<AVAudioUnitVarispeed *> *pitches;
@property(nonatomic) NSUInteger nextVoice;
@end

@implementation DDEffectMixer
- (BOOL)prepare {
  if (!_engine) {
    _engine = [AVAudioEngine new];
    _voices = [NSMutableArray new];
    _pitches = [NSMutableArray new];
    AVAudioFormat *format = [[AVAudioFormat alloc] initStandardFormatWithSampleRate:DDSfx_RATE channels:1];
    for (NSUInteger i = 0; i < DDVoiceCount; ++i) {
      AVAudioPlayerNode *voice = [AVAudioPlayerNode new];
      AVAudioUnitVarispeed *pitch = [AVAudioUnitVarispeed new];
      [_engine attachNode:voice]; [_engine attachNode:pitch];
      [_engine connect:voice to:pitch format:format];
      [_engine connect:pitch to:_engine.mainMixerNode format:format];
      [_voices addObject:voice]; [_pitches addObject:pitch];
    }
    [_engine prepare];
  }
  if (!_engine.isRunning) {
    NSError *error = nil;
    if (![_engine startAndReturnError:&error]) return NO;
  }
  return YES;
}

- (void)playBuffer:(AVAudioPCMBuffer *)buffer rate:(float)rate gain:(float)gain {
  if (!buffer.frameLength || ![self prepare]) return;
  AVAudioPlayerNode *voice = _voices[_nextVoice];
  _pitches[_nextVoice].rate = MAX(.5f, MIN(2.f, rate));
  voice.volume = MAX(0.f, MIN(1.f, gain));
  // Interrupt only this reusable voice; no per-effect audio queue is created or disposed.
  [voice scheduleBuffer:buffer atTime:nil options:AVAudioPlayerNodeBufferInterrupts completionHandler:nil];
  if (!voice.isPlaying) [voice play];
  _nextVoice = (_nextVoice + 1) % DDVoiceCount;
}

- (void)pause {
  for (AVAudioPlayerNode *voice in _voices) [voice stop];
  [_engine pause];
  _nextVoice = 0;
}

- (void)dealloc { [_engine stop]; }
@end
