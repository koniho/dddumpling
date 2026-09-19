#import "DDGameView.h"
#import "DDPainter.h"
#import "DDAudio.h"
#import "DDStore.h"
#import "DDFrameMetrics.h"
#import "DDGameCenter.h"
#import "DDGameCloud.h"
#import "DDGameServices.h"
#import "com/dddumpling/game/IOSGame.h"
#import "com/dddumpling/game/IOSTouch.h"
#import "IOSPrimitiveArray.h"
#import "IOSObjectArray.h"
#import "IOSClass.h"
#import <QuartzCore/QuartzCore.h>

@class DDGameView;
@interface DDHost : NSObject <DDIOSGame_Host>
@property(nonatomic, weak) DDGameView *view;
@end

@interface DDGameView ()
@property(nonatomic, strong) DDIOSGame *game;
@property(nonatomic, strong) DDIOSAudio *audio;
@property(nonatomic, strong) DDIOSStore *store;
@property(nonatomic, strong) DDIOSPainter *painter;
@property(nonatomic, strong) CADisplayLink *displayLink;
@property(nonatomic, strong) NSMutableArray<UITouch *> *pointers;
@property(nonatomic, strong) NSMutableArray<NSNumber *> *pointerIDs;
@property(nonatomic, strong) NSMutableArray<NSValue *> *pointerPositions;
@property(nonatomic, strong) NSMutableArray<NSNumber *> *pointerTimes;
@property(nonatomic, strong) UIAccessibilityElement *gameElement;
@property(nonatomic, strong) UIButton *backButton;
@property(nonatomic, strong) UIImpactFeedbackGenerator *haptic;
@property(nonatomic, strong) UIImpactFeedbackGenerator *heavyHaptic;
@property(nonatomic, strong) DDFrameMetrics *frameMetrics;
@property(nonatomic) NSInteger nextID;
@property(nonatomic) CFTimeInterval lastTime;
@property(nonatomic) BOOL active;
@property(nonatomic) BOOL sceneLoaded;
@property(nonatomic) BOOL storeErrorShown;
@property(nonatomic) BOOL storeAlertVisible;
@property(nonatomic) BOOL gameCenterVisible;
@property(nonatomic, strong) DDGameCenter *gameCenter;
@property(nonatomic, strong) DDGameCloud *gameCloud;
#if DEBUG
@property(nonatomic, strong) UIButton *servicesButton;
#endif
@end

@implementation DDHost
- (void)tick {
    CFTimeInterval start = CACurrentMediaTime();
    [self.view.haptic impactOccurred];
    [self.view.frameMetrics recordHapticMilliseconds:(CACurrentMediaTime() - start) * 1000];
}
- (void)impact {
    CFTimeInterval start = CACurrentMediaTime();
    [self.view.heavyHaptic impactOccurred];
    [self.view.frameMetrics recordHapticMilliseconds:(CACurrentMediaTime() - start) * 1000];
}
- (void)openPrivacyWithNSString:(NSString *)url {
    NSURL *address = [NSURL URLWithString:url];
    if (address) [UIApplication.sharedApplication openURL:address options:@{} completionHandler:nil];
}
@end

@implementation DDGameView
- (void)didMoveToWindow {
    [super didMoveToWindow];
#if DEBUG || DDDUMPLING_GAME_CENTER
    if (self.window && !self.gameCenter) {
        self.gameCenter = [[DDGameCenter alloc] initWithPresenter:self.window.rootViewController];
        self.gameCloud = [[DDGameCloud alloc] initWithGame:self.game store:self.store center:self.gameCenter];
        __weak DDGameView *weakSelf = self;
        self.gameCenter.presentationChanged = ^(BOOL visible) {
            DDGameView *view = weakSelf;
            view.gameCenterVisible = visible;
            [view refreshActivity];
        };
    }
#endif
}
- (instancetype)initWithFrame:(CGRect)frame {
    if ((self = [super initWithFrame:frame])) {
        self.multipleTouchEnabled = YES;
        self.opaque = YES;
        // Keep deferred Core Graphics rasterization off the input/update thread.
        self.layer.drawsAsynchronously = YES;
#if DEBUG
        if ([NSProcessInfo.processInfo.environment[@"DDD_SYNC_RASTER"] boolValue])
            self.layer.drawsAsynchronously = NO;
#endif
        self.backgroundColor = UIColor.blackColor;
        self.isAccessibilityElement = NO;
        _gameElement = [[UIAccessibilityElement alloc] initWithAccessibilityContainer:self];
        _gameElement.accessibilityIdentifier = @"game";
        _gameElement.accessibilityLabel = @"DDDUMPLING game";
        _pointers = [NSMutableArray new];
        _pointerIDs = [NSMutableArray new];
        _pointerPositions = [NSMutableArray new];
        _pointerTimes = [NSMutableArray new];
        _store = [DDIOSStore new];
        _audio = [DDIOSAudio new];
        _painter = [DDIOSPainter new];
        jlong seed = (jlong)(CACurrentMediaTime() * 1e9);
#if DEBUG
        if (NSProcessInfo.processInfo.environment[@"DDD_SCENE"].length) seed = 9;
#endif
        _game = [[DDIOSGame alloc] initWithDDGameCore_Store:_store withDDGameCore_Sound:_audio withLong:seed];
        DDHost *host = [DDHost new];
        host.view = self;
        [_game setHostWithDDIOSGame_Host:host];
        _haptic = [[UIImpactFeedbackGenerator alloc] initWithStyle:UIImpactFeedbackStyleLight];
        _heavyHaptic = [[UIImpactFeedbackGenerator alloc] initWithStyle:UIImpactFeedbackStyleHeavy];
        _frameMetrics = [[DDFrameMetrics alloc]
            initWithEnabled:[NSProcessInfo.processInfo.environment[@"DDD_PROFILE"] boolValue]];
        _backButton = [UIButton buttonWithType:UIButtonTypeSystem];
        [_backButton setImage:[UIImage systemImageNamed:@"pause.fill"] forState:UIControlStateNormal];
        _backButton.tintColor = [UIColor colorWithRed:.75 green:.70 blue:.87 alpha:1];
        _backButton.accessibilityIdentifier = @"pause";
        _backButton.accessibilityLabel = @"Pause or go back";
        [_backButton addTarget:self action:@selector(navigateBack) forControlEvents:UIControlEventTouchUpInside];
        [self addSubview:_backButton];
#if DEBUG
        _servicesButton = [UIButton buttonWithType:UIButtonTypeSystem];
        [_servicesButton setTitle:@"DEV · Game Center" forState:UIControlStateNormal];
        _servicesButton.tintColor = _backButton.tintColor;
        _servicesButton.titleLabel.font = [UIFont systemFontOfSize:14 weight:UIFontWeightSemibold];
        _servicesButton.accessibilityIdentifier = @"gameServices";
        [_servicesButton addTarget:self action:@selector(showGameServices) forControlEvents:UIControlEventTouchUpInside];
        [self addSubview:_servicesButton];
#endif
        _displayLink = [CADisplayLink displayLinkWithTarget:self selector:@selector(frame:)];
        _displayLink.preferredFrameRateRange = CAFrameRateRangeMake(60, 60, 60);
        [_displayLink addToRunLoop:NSRunLoop.mainRunLoop forMode:NSRunLoopCommonModes];
        _displayLink.paused = YES;
    }
    return self;
}
- (void)layoutSubviews {
    [super layoutSubviews];
    UIEdgeInsets p = self.safeAreaInsets;
    // A native navigation strip keeps the pause target clear of the shared score/lives HUD.
    [self.game layoutWithFloat:self.bounds.size.width withFloat:self.bounds.size.height
                   withFloat:p.left withFloat:p.top + 44 withFloat:p.right withFloat:p.bottom];
    self.backButton.frame = CGRectMake(self.bounds.size.width - p.right - 46, p.top, 44, 44);
    self.gameElement.accessibilityFrameInContainerSpace = self.bounds;
#if DEBUG
    self.servicesButton.frame = CGRectMake(p.left + 8, p.top, MIN(200, self.bounds.size.width - p.left - p.right - 60), 44);
    if (!self.sceneLoaded && self.bounds.size.width > 0 && self.bounds.size.height > 0) {
        NSString *scene = NSProcessInfo.processInfo.environment[@"DDD_SCENE"];
        if (scene.length) [self.game debugSceneWithNSString:scene];
        self.sceneLoaded = YES;
    }
#endif
    [self refreshNavigation];
}
- (void)safeAreaInsetsDidChange { [super safeAreaInsetsDidChange]; [self setNeedsLayout]; }
- (void)refreshNavigation {
    self.backButton.hidden = ![self.game showsBackButton];
    self.backButton.accessibilityLabel = [self.game paused] ? @"Resume or go back" : @"Pause or go back";
    self.accessibilityElements = self.backButton.hidden ? @[self.gameElement] : @[self.gameElement, self.backButton];
#if DEBUG
    self.servicesButton.hidden = ![self.game developerServicesVisible];
    if (!self.servicesButton.hidden)
        self.accessibilityElements = [self.accessibilityElements arrayByAddingObject:self.servicesButton];
    self.gameElement.accessibilityValue = [self.game debugStatus];
#endif
}
#if DEBUG
- (void)showGameServices {
    UIViewController *controller = self.window.rootViewController;
    if (!self.active || !controller || controller.presentedViewController) return;
    NSString *message = [NSString stringWithFormat:@"Game Center: %@\nCloud save: %@\n%@\n\n%@",
        self.gameCenter.status ?: @"Waiting", self.gameCloud.status ?: @"Waiting",
        self.gameCenter.lastError ?: @"", NSBundle.mainBundle.bundleIdentifier];
    UIAlertController *alert = [UIAlertController alertControllerWithTitle:@"DDD Dev services"
        message:message preferredStyle:UIAlertControllerStyleAlert];
    self.gameCenterVisible = YES;
    [self refreshActivity];
    __weak DDGameView *weakSelf = self;
    [alert addAction:[UIAlertAction actionWithTitle:@"Done" style:UIAlertActionStyleDefault handler:^(UIAlertAction *action) {
        weakSelf.gameCenterVisible = NO;
        [weakSelf refreshActivity];
    }]];
    [controller presentViewController:alert animated:YES completion:nil];
}
#endif
- (void)clearPointers {
    [self.pointers removeAllObjects];
    [self.pointerIDs removeAllObjects];
    [self.pointerPositions removeAllObjects];
    [self.pointerTimes removeAllObjects];
    self.nextID = 0;
}
- (void)showStoreErrorIfNeeded {
    if (self.storeErrorShown || !self.store.error.length || !self.window || !self.active) return;
    UIViewController *controller = self.window.rootViewController;
    if (!controller || controller.presentedViewController) return;
    self.storeErrorShown = YES;
    self.storeAlertVisible = YES;
    [self.game backgroundWithBoolean:YES];
    [self clearPointers];
    [self.audio setActive:NO];
    UIAlertController *alert = [UIAlertController alertControllerWithTitle:@"Progress storage problem"
        message:self.store.error preferredStyle:UIAlertControllerStyleAlert];
    __weak DDGameView *weakSelf = self;
    [alert addAction:[UIAlertAction actionWithTitle:@"OK" style:UIAlertActionStyleDefault handler:^(UIAlertAction *action) {
        (void)action;
        DDGameView *view = weakSelf;
        if (!view) return;
        view.storeAlertVisible = NO;
        [view refreshActivity];
        view.lastTime = 0;
        [view refreshNavigation];
    }]];
    [controller presentViewController:alert animated:YES completion:nil];
}
- (void)navigateBack {
    [self.game back];
    [self clearPointers];
    self.lastTime = 0;
    [self.frameMetrics reset];
    [self refreshNavigation];
    [self setNeedsDisplay];
}
- (void)setActive:(BOOL)active {
    if (_active == active) return;
    _active = active;
    [self refreshActivity];
#if DDDUMPLING_GAME_CENTER
    [self.gameCenter refreshActive:active && !self.storeAlertVisible];
    [self.gameCloud updateActive:active elapsed:0];
#endif
}
- (void)refreshActivity {
    self.lastTime = 0;
    [self.frameMetrics reset];
    BOOL playable = self.active && !self.storeAlertVisible && !self.gameCenterVisible;
    [self.game backgroundWithBoolean:!playable];
    [self clearPointers];
    [self.audio setActive:playable];
    self.displayLink.paused = !self.active;
    [self refreshNavigation];
    [self setNeedsDisplay];
}
- (void)frame:(CADisplayLink *)link {
    if (!self.active || self.bounds.size.width <= 0) return;
    CFTimeInterval now = link.timestamp;
    [self.frameMetrics recordDisplayLinkTimestamp:now];
    float elapsed = self.lastTime > 0 ? (float)(now - self.lastTime) : 0;
    self.lastTime = now;
#if DDDUMPLING_GAME_CENTER
    [self.gameCenter refreshActive:!self.storeAlertVisible];
    [self.gameCloud updateActive:YES elapsed:elapsed];
#endif
    CFTimeInterval start = CACurrentMediaTime();
    [self.game updateWithFloat:elapsed];
    [self.frameMetrics recordUpdateMilliseconds:(CACurrentMediaTime() - start) * 1000];
    [self showStoreErrorIfNeeded];
    [self refreshNavigation];
    [self setNeedsDisplay];
}
- (void)drawRect:(CGRect)rect {
    CFTimeInterval start = CACurrentMediaTime();
    self.painter.context = UIGraphicsGetCurrentContext();
    [self.game drawWithDDPainter:self.painter];
    self.painter.context = NULL;
    [self.frameMetrics recordDrawMilliseconds:(CACurrentMediaTime() - start) * 1000];
    if ([self.frameMetrics windowComplete]) {
#if DEBUG
        [self.frameMetrics logWindowWithDebugStatus:[self.game debugStatus]];
#else
        [self.frameMetrics logWindowWithDebugStatus:nil];
#endif
    }
}
- (void)packet:(jint)action index:(NSUInteger)index event:(UIEvent *)event {
    NSUInteger n = self.pointers.count;
    if (!n) return;
    CFTimeInterval start = CACurrentMediaTime();
    IOSIntArray *ids = [IOSIntArray arrayWithLength:(jint)n];
    IOSFloatArray *xs = [IOSFloatArray arrayWithLength:(jint)n];
    IOSFloatArray *ys = [IOSFloatArray arrayWithLength:(jint)n];
    NSMutableArray<NSArray<UITouch *> *> *histories = [NSMutableArray new];
    NSMutableSet<NSNumber *> *timestamps = [NSMutableSet new];
    // Align samples by time, retaining every pointer's previous position until its next sample.
    for (NSUInteger i = 0; i < n; i++) {
        NSArray<UITouch *> *samples = action == 2 ? [event coalescedTouchesForTouch:self.pointers[i]] : nil;
        samples = samples ?: @[];
        [histories addObject:samples];
        for (UITouch *sample in samples) {
            if (sample.timestamp > self.pointerTimes[i].doubleValue && sample.timestamp < event.timestamp)
                [timestamps addObject:@(sample.timestamp)];
        }
    }
    NSArray<NSNumber *> *times = [timestamps.allObjects sortedArrayUsingSelector:@selector(compare:)];
    IOSObjectArray *historyX = [IOSObjectArray arrayWithLength:times.count type:IOSClass_floatArray(1)];
    IOSObjectArray *historyY = [IOSObjectArray arrayWithLength:times.count type:IOSClass_floatArray(1)];
    for (NSUInteger h = 0; h < times.count; h++) {
        IOSFloatArray *rowX = [IOSFloatArray arrayWithLength:(jint)n];
        IOSFloatArray *rowY = [IOSFloatArray arrayWithLength:(jint)n];
        for (NSUInteger i = 0; i < n; i++) {
            CGPoint point = self.pointerPositions[i].CGPointValue;
            for (UITouch *sample in histories[i]) {
                if (sample.timestamp > times[h].doubleValue) break;
                if (sample.timestamp >= self.pointerTimes[i].doubleValue) point = [sample locationInView:self];
            }
            rowX->buffer_[i] = point.x; rowY->buffer_[i] = point.y;
        }
        IOSObjectArray_Set(historyX, h, rowX);
        IOSObjectArray_Set(historyY, h, rowY);
    }
    for (NSUInteger i = 0; i < n; i++) {
        CGPoint point = [self.pointers[i] locationInView:self];
        ids->buffer_[i] = self.pointerIDs[i].intValue;
        xs->buffer_[i] = point.x;
        ys->buffer_[i] = point.y;
        self.pointerPositions[i] = [NSValue valueWithCGPoint:point];
        self.pointerTimes[i] = @(self.pointers[i].timestamp);
    }
    DDIOSGame *game = self.game;
    [game touchWithDDIOSTouch:[[DDIOSTouch alloc] initWithInt:action withInt:(jint)index
        withIntArray:ids withFloatArray:xs withFloatArray:ys withFloatArray2:historyX withFloatArray2:historyY]];
    [self refreshNavigation];
    [self.frameMetrics recordTouchMilliseconds:(CACurrentMediaTime() - start) * 1000];
}
- (void)touchesBegan:(NSSet<UITouch *> *)touches withEvent:(UIEvent *)event {
    if (!self.active) return;
    for (UITouch *touch in touches) {
        [self.pointers addObject:touch];
        [self.pointerIDs addObject:@(self.nextID++)];
        [self.pointerPositions addObject:[NSValue valueWithCGPoint:[touch locationInView:self]]];
        [self.pointerTimes addObject:@(touch.timestamp)];
        [self packet:self.pointers.count == 1 ? 0 : 5 index:self.pointers.count - 1 event:event];
    }
}
- (void)touchesMoved:(NSSet<UITouch *> *)touches withEvent:(UIEvent *)event {
    if (self.active) [self packet:2 index:0 event:event];
}
- (void)touchesEnded:(NSSet<UITouch *> *)touches withEvent:(UIEvent *)event {
    for (UITouch *touch in touches) {
        NSUInteger i = [self.pointers indexOfObjectIdenticalTo:touch];
        if (i == NSNotFound) continue;
        [self packet:self.pointers.count == 1 ? 1 : 6 index:i event:event];
        [self.pointers removeObjectAtIndex:i];
        [self.pointerIDs removeObjectAtIndex:i];
        [self.pointerPositions removeObjectAtIndex:i];
        [self.pointerTimes removeObjectAtIndex:i];
    }
    if (!self.pointers.count) self.nextID = 0;
}
- (void)touchesCancelled:(NSSet<UITouch *> *)touches withEvent:(UIEvent *)event {
    [self packet:3 index:0 event:event];
    [self clearPointers];
}
@end
