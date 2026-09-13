#import <XCTest/XCTest.h>
#import "DDAnalytics.h"

@interface DDAnalyticsRecorder : NSObject <DDAnalyticsBackend>
@property(nonatomic, strong) NSMutableArray *choices;
@property(nonatomic, strong) NSMutableArray *events;
@end
@implementation DDAnalyticsRecorder
- (instancetype)init {
    if ((self = [super init])) { _choices = [NSMutableArray new]; _events = [NSMutableArray new]; }
    return self;
}
- (void)setCollectionEnabled:(BOOL)enabled { [self.choices addObject:@(enabled)]; }
- (void)recordEvent:(NSString *)name amount:(NSInteger)amount {
    [self.events addObject:@{@"name": name, @"amount": @(amount)}];
}
@end

@interface DDAnalyticsTests : XCTestCase
@property(nonatomic, strong) NSUserDefaults *defaults;
@property(nonatomic, strong) NSString *suite;
@end
@implementation DDAnalyticsTests
- (void)setUp {
    self.suite = [@"ddd.analytics.test." stringByAppendingString:NSUUID.UUID.UUIDString];
    self.defaults = [[NSUserDefaults alloc] initWithSuiteName:self.suite];
}
- (void)tearDown { [self.defaults removePersistentDomainForName:self.suite]; }
- (void)testNoSDKCallsBeforeConsentAndNoReplayAfterOptIn {
    DDAnalyticsRecorder *backend = [DDAnalyticsRecorder new];
    DDGameAnalytics *analytics = [[DDGameAnalytics alloc] initWithDefaults:self.defaults backend:backend];
    XCTAssertTrue(analytics.available);
    XCTAssertFalse(analytics.enabled);
    [analytics eventWithNSString:@"runs_started" withInt:1];
    XCTAssertEqual(backend.choices.count, 0u);
    XCTAssertEqual(backend.events.count, 0u);
    [analytics setConsent:YES];
    XCTAssertEqualObjects(backend.choices, (@[@YES]));
    XCTAssertEqual(backend.events.count, 0u);
    [analytics eventWithNSString:@"boss_slime_first_hit_ms_total" withInt:12345];
    XCTAssertEqualObjects(backend.events.firstObject, (@{@"name": @"boss_slime_first_hit_ms_total", @"amount": @12345}));
}
- (void)testWithdrawalAndChoiceSurviveRestart {
    DDAnalyticsRecorder *backend = [DDAnalyticsRecorder new];
    DDGameAnalytics *analytics = [[DDGameAnalytics alloc] initWithDefaults:self.defaults backend:backend];
    [analytics setConsent:YES];
    DDGameAnalytics *restarted = [[DDGameAnalytics alloc] initWithDefaults:self.defaults backend:backend];
    XCTAssertTrue(restarted.enabled);
    [restarted setConsent:NO];
    [restarted eventWithNSString:@"runs_finished" withInt:1];
    XCTAssertEqualObjects(backend.choices.lastObject, @NO);
    XCTAssertEqual(backend.events.count, 0u);
    NSUInteger calls = backend.choices.count;
    restarted = [[DDGameAnalytics alloc] initWithDefaults:self.defaults backend:backend];
    XCTAssertFalse(restarted.enabled);
    XCTAssertEqual(backend.choices.count, calls);
}
- (void)testMissingConfigurationNeverEnablesCollection {
    DDGameAnalytics *analytics = [[DDGameAnalytics alloc] initWithDefaults:self.defaults backend:nil];
    [analytics setConsent:YES];
    XCTAssertFalse(analytics.available);
    XCTAssertFalse(analytics.enabled);
    XCTAssertNil([self.defaults objectForKey:@"ddd.analytics.consent.v1"]);
    XCTAssertFalse([DDGameAnalytics new].available); // Test target runs the unconfigured Debug app.
}
- (void)testInvalidAmountsAndLongNamesAreDropped {
    DDAnalyticsRecorder *backend = [DDAnalyticsRecorder new];
    DDGameAnalytics *analytics = [[DDGameAnalytics alloc] initWithDefaults:self.defaults backend:backend];
    [analytics setConsent:YES];
    [analytics eventWithNSString:@"runs_started" withInt:0];
    [analytics eventWithNSString:@"runs_started" withInt:-1];
    [analytics eventWithNSString:@"" withInt:1];
    [analytics eventWithNSString:[@"a" stringByPaddingToLength:41 withString:@"a" startingAtIndex:0] withInt:1];
    XCTAssertEqual(backend.events.count, 0u);
}
@end
