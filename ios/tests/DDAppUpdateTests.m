#import <XCTest/XCTest.h>
#import "DDAppUpdate.h"
#import "DDStore.h"
#import "com/dddumpling/game/IOSGame.h"

@interface DDAppUpdateTests : XCTestCase
@end
@implementation DDAppUpdateTests
- (NSMutableDictionary *)listing {
    return [@{@"kind":@"software", @"trackId":@6811478003LL, @"bundleId":@"com.dddumpling.game.ios",
              @"version":@"0.1.10", @"minimumOsVersion":@"15.0"} mutableCopy];
}
- (NSString *)parse:(id)json installed:(NSString *)installed {
    return [DDAppUpdate newerVersion:json bundleID:@"com.dddumpling.game.ios" appID:@"6811478003"
                          installed:installed system:@"16.2"];
}
- (void)testPublishedVersionAndCompatibility {
    NSMutableDictionary *row=[self listing];
    NSDictionary *json=@{@"results":@[row]};
    XCTAssertEqualObjects([self parse:json installed:@"0.1.9"], @"0.1.10");
    XCTAssertNil([self parse:json installed:@"0.1.10"]);
    XCTAssertNil([self parse:json installed:@"0.1.10.0"]);
    XCTAssertNil([self parse:json installed:@"0.1.11"]);
    row[@"minimumOsVersion"]=@"17.0";
    XCTAssertNil([self parse:json installed:@"0.1.9"]);
    row[@"minimumOsVersion"]=@"15.0";
    row[@"bundleId"]=@"another.app";
    XCTAssertNil([self parse:json installed:@"0.1.9"]);
    row[@"bundleId"]=@"com.dddumpling.game.ios";row[@"trackId"]=@12;
    XCTAssertNil([self parse:json installed:@"0.1.9"]);
}
- (void)testMalformedAndMissingListingsStayQuiet {
    for(id json in @[[NSNull null], @[], @{}, @{@"results":@"bad"}, @{@"results":@[[NSNull null],@"bad",@{}]}])
        XCTAssertNil([self parse:json installed:@"0.1.9"]);
    NSMutableDictionary *row=[self listing];
    for(id value in @[@"",@"0.2-beta",@"1..2",@"new",@[],[NSNull null]]) {
        row[@"version"]=value;
        XCTAssertNil([self parse:@{@"results":@[row]} installed:@"0.1.9"]);
    }
}
- (void)testStorefrontMappingUsesStoreCountry {
    XCTAssertEqualObjects([DDIOSGame storeCountryWithNSString:@"USA"], @"US");
    XCTAssertEqualObjects([DDIOSGame storeCountryWithNSString:@"GBR"], @"GB");
    XCTAssertEqualObjects([DDIOSGame storeCountryWithNSString:@"JPN"], @"JP");
    XCTAssertEqualObjects([DDIOSGame storeCountryWithNSString:@"unknown"], @"");
}
- (void)testReminderStatePersistsSeparately {
    NSURL *url=[NSURL fileURLWithPath:[NSTemporaryDirectory() stringByAppendingPathComponent:NSUUID.UUID.UUIDString]];
    DDIOSStore *store=[[DDIOSStore alloc] initWithURL:url];
    [store saveStoreUpdateWithNSString:@"1\n0.1.40\n1800000000000\n1799999999999"];
    DDIOSStore *reopened=[[DDIOSStore alloc] initWithURL:url];
    XCTAssertEqualObjects(reopened.loadStoreUpdate,store.loadStoreUpdate);
    XCTAssertEqualObjects(reopened.loadUpdateState,@"");
    XCTAssertGreaterThan(reopened.updateTimeMillis,0);
    [NSFileManager.defaultManager removeItemAtURL:url error:nil];
}
@end
