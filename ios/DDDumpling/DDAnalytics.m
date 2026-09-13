#import "DDAnalytics.h"
#import <FirebaseCore/FirebaseCore.h>
#import <FirebaseAnalytics/FirebaseAnalytics.h>

static NSString *const DDConsentKey = @"ddd.analytics.consent.v1";
static NSString *const DDPrivacyURL = @"https://koniho.github.io/dddumpling-privacy/";

@interface DDFirebaseAnalytics : NSObject <DDAnalyticsBackend>
@property(nonatomic, strong) FIROptions *options;
@end
@implementation DDFirebaseAnalytics
- (void)setCollectionEnabled:(BOOL)enabled {
    if (enabled && !FIRApp.defaultApp) [FIRApp configureWithOptions:self.options];
    if (!FIRApp.defaultApp) return;
    if (!enabled) [FIRAnalytics setAnalyticsCollectionEnabled:NO];
    [FIRAnalytics setConsent:@{
        FIRConsentTypeAnalyticsStorage: enabled ? FIRConsentStatusGranted : FIRConsentStatusDenied,
        FIRConsentTypeAdStorage: FIRConsentStatusDenied,
        FIRConsentTypeAdUserData: FIRConsentStatusDenied,
        FIRConsentTypeAdPersonalization: FIRConsentStatusDenied
    }];
    if (enabled) [FIRAnalytics setAnalyticsCollectionEnabled:YES];
    else [FIRAnalytics resetAnalyticsData];
}
- (void)recordEvent:(NSString *)name amount:(NSInteger)amount {
    [FIRAnalytics logEventWithName:name parameters:@{@"amount": @(amount)}];
}
@end

#if DEBUG
// UI tests exercise the real consent UI without a Firebase project or network collection.
@interface DDAnalyticsPreviewBackend : NSObject <DDAnalyticsBackend>
@end
@implementation DDAnalyticsPreviewBackend
- (void)setCollectionEnabled:(BOOL)enabled {}
- (void)recordEvent:(NSString *)name amount:(NSInteger)amount {}
@end
#endif

@interface DDGameAnalytics ()
@property(nonatomic, strong) NSUserDefaults *defaults;
@property(nonatomic, strong) id<DDAnalyticsBackend> backend;
@property(nonatomic) BOOL enabled;
@property(nonatomic) BOOL prompted;
@end
@implementation DDGameAnalytics
- (instancetype)init {
#if DEBUG
    if ([NSProcessInfo.processInfo.environment[@"DDD_ANALYTICS_PREVIEW"] isEqualToString:@"1"]) {
        NSUserDefaults *defaults = [[NSUserDefaults alloc] initWithSuiteName:@"ddd.analytics.ui-tests"];
        if ([NSProcessInfo.processInfo.environment[@"DDD_ANALYTICS_RESET"] isEqualToString:@"1"])
            [defaults removePersistentDomainForName:@"ddd.analytics.ui-tests"];
        return [self initWithDefaults:defaults backend:[DDAnalyticsPreviewBackend new]];
    }
#endif
    DDFirebaseAnalytics *backend = nil;
#if !DEBUG
    NSString *path = [NSBundle.mainBundle pathForResource:@"GoogleService-Info" ofType:@"plist"];
    FIROptions *options = path ? [[FIROptions alloc] initWithContentsOfFile:path] : nil;
    if (options && [options.bundleID isEqualToString:NSBundle.mainBundle.bundleIdentifier]) {
        backend = [DDFirebaseAnalytics new];
        backend.options = options;
    }
#endif
    return [self initWithDefaults:NSUserDefaults.standardUserDefaults backend:backend];
}
- (instancetype)initWithDefaults:(NSUserDefaults *)defaults backend:(id<DDAnalyticsBackend>)backend {
    if ((self = [super init])) {
        _defaults = defaults;
        _backend = backend;
        _enabled = backend && [defaults boolForKey:DDConsentKey];
        // Do not initialize Firebase for people who have never opted in.
        if (_enabled) [_backend setCollectionEnabled:YES];
    }
    return self;
}
- (BOOL)available { return self.backend != nil; }
- (void)setConsent:(BOOL)allowed {
    if (!self.available) return;
    self.enabled = allowed;
    [self.defaults setBool:allowed forKey:DDConsentKey];
    [self.backend setCollectionEnabled:allowed];
}
- (void)eventWithNSString:(NSString *)name withInt:(jint)amount {
    if (!self.enabled || amount <= 0 || !name.length || name.length > 40) return;
    [self.backend recordEvent:name amount:amount];
}
- (void)openPolicy {
    [UIApplication.sharedApplication openURL:[NSURL URLWithString:DDPrivacyURL]
                                    options:@{} completionHandler:nil];
}
- (BOOL)shouldOfferConsent {
    if (!self.available || self.prompted || [self.defaults objectForKey:DDConsentKey]) return NO;
    self.prompted = YES;
    return YES;
}
@end
