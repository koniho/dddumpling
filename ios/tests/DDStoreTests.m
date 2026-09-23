#import <XCTest/XCTest.h>
#import <CommonCrypto/CommonDigest.h>

#import "DDStore.h"
#import "IOSPrimitiveArray.h"
#import "com/dddumpling/game/Collect.h"

@interface DDStoreTests : XCTestCase
@end

@implementation DDStoreTests

- (void)testEmptyScoreResetKeepsTheStarterRoster {
  NSURL *url = [self temporaryFile];
  DDIOSStore *store = [[DDIOSStore alloc] initWithURL:url];
  XCTAssertEqual([store loadRosterState], 0);
  XCTAssertTrue([store resetHighScoresWithByteArray:nil]);
  DDIOSStore *reopened = [[DDIOSStore alloc] initWithURL:url];
  XCTAssertEqual([reopened loadRosterState], 0);
  [[NSFileManager defaultManager] removeItemAtURL:url error:nil];
}

- (void)testScoreResetPersistsWithoutChangingOtherProgress {
  NSURL *url = [self temporaryFile];
  DDIOSStore *store = [[DDIOSStore alloc] initWithURL:url];
  [store saveBestWithInt:9000];
  [store saveLandBestWithInt:1 withInt:12000];
  [store saveHighScoresWithNSString:@"old records"];
  [store saveCollectedWithLong:3];
  [store saveCaseIndexWithInt:1];
  [store saveLandStateWithInt:7];
  [store savePlayerSettingsWithInt:42];
  IOSByteArray *progress = [IOSByteArray arrayWithLength:3];
  progress->buffer_[0] = 11;
  XCTAssertTrue([store resetHighScoresWithByteArray:progress]);
  DDIOSStore *reopened = [[DDIOSStore alloc] initWithURL:url];
  XCTAssertEqual([reopened loadBest], 0);
  XCTAssertEqual([reopened loadLandBestWithInt:1], 0);
  XCTAssertEqualObjects([reopened loadHighScores], @"");
  XCTAssertEqual([reopened loadProgress]->buffer_[0], 11);
  XCTAssertEqual([reopened loadCollected], 3);
  XCTAssertEqual([reopened loadCaseIndex], 1);
  XCTAssertEqual([reopened loadLandState], 7);
  XCTAssertEqual([reopened loadPlayerSettings], 42);
  [[NSFileManager defaultManager] removeItemAtURL:url error:nil];
}

- (void)testCloudOwnerPersistsAndRejectsEitherAccountChanging {
  NSURL *url = [self temporaryFile];
  NSData *identity = [@"icloud-a" dataUsingEncoding:NSUTF8StringEncoding];
  DDIOSStore *store = [[DDIOSStore alloc] initWithURL:url];
  XCTAssertFalse([store bindCloudPlayer:@"" identity:identity]);
  XCTAssertTrue([store bindCloudPlayer:@"game-a" identity:identity]);
  NSData *before = [NSData dataWithContentsOfURL:url];
  DDIOSStore *reopened = [[DDIOSStore alloc] initWithURL:url];
  XCTAssertTrue([reopened bindCloudPlayer:@"game-a" identity:identity]);
  XCTAssertFalse([reopened bindCloudPlayer:@"game-b" identity:identity]);
  XCTAssertFalse([reopened bindCloudPlayer:@"game-a" identity:[@"icloud-b" dataUsingEncoding:NSUTF8StringEncoding]]);
  XCTAssertEqualObjects(before, [NSData dataWithContentsOfURL:url]);
  [[NSFileManager defaultManager] removeItemAtURL:url error:nil];
}

- (NSURL *)temporaryFile {
  NSURL *directory = [NSURL fileURLWithPath:NSTemporaryDirectory() isDirectory:YES];
  return [directory URLByAppendingPathComponent:[NSString stringWithFormat:@"dddumpling-store-%@.plist", NSUUID.UUID.UUIDString]];
}

- (NSData *)envelopeWithVersion:(NSString *)version checksum:(NSData *)checksum {
  NSData *payload = [NSPropertyListSerialization dataWithPropertyList:@{}
                                                                 format:NSPropertyListBinaryFormat_v1_0
                                                                options:0 error:nil];
  if (!checksum) {
    unsigned char digest[CC_SHA256_DIGEST_LENGTH];
    CC_SHA256(payload.bytes, (CC_LONG)payload.length, digest);
    checksum = [NSData dataWithBytes:digest length:sizeof(digest)];
  }
  NSDictionary *envelope = @{ @"magic": @"dddumpling-store", @"version": version,
                               @"payload": payload, @"checksum": checksum };
  return [NSPropertyListSerialization dataWithPropertyList:envelope
                                                     format:NSPropertyListBinaryFormat_v1_0
                                                    options:0 error:nil];
}

- (void)assertReadOnlyForData:(NSData *)data atURL:(NSURL *)url {
  [data writeToURL:url atomically:YES];
  DDIOSStore *store = [[DDIOSStore alloc] initWithURL:url];
  XCTAssertNotNil(store.error);
  NSData *original = [NSData dataWithContentsOfURL:url];
  [store saveBestWithInt:999];
  XCTAssertFalse([store resetHighScoresWithByteArray:nil]);
  XCTAssertEqualObjects([NSData dataWithContentsOfURL:url], original);
}

- (void)testRoundTripsEveryStoreFieldAndProgress {
  NSURL *url = [self temporaryFile];
  DDIOSStore *saved = [[DDIOSStore alloc] initWithURL:url];
  XCTAssertEqual(saved.loadCaveChoice, -1);
  [saved saveCaveChoiceWithInt:4];
  [saved saveCaseIndexWithInt:17];
  [saved saveBestWithInt:812];
  [saved saveLandStateWithInt:0x52];
  [saved saveReleaseSeenWithNSString:@"test-build"];
  [saved saveHighScoresWithNSString:@"3:1;1,100,2,1,0,0,0,0,4,1,3,4,100,0,0,5,17"];
  [saved saveLandBestWithInt:2 withInt:900];
  [saved savePlayerSettingsWithInt:98329];
  [saved saveCollectedWithLong:0x12345];
  IOSIntArray *counts = [IOSIntArray arrayWithLength:DDCollect_COUNT];
  counts->buffer_[0] = 2; counts->buffer_[DDCollect_COUNT - 1] = 4;
  [saved saveCollectionCountsWithIntArray:counts];
  [saved saveCollectTotalWithInt:27];
  [saved saveSteamerOpensWithInt:8];
  [saved saveStarWinsWithInt:3];
  [saved saveMineCartsWithInt:4];
  [saved saveRosterStateWithInt:5];
  IOSByteArray *progress = [IOSByteArray arrayWithLength:4];
  progress->buffer_[0] = 9; progress->buffer_[3] = 6;
  [saved saveProgressWithByteArray:progress];
  NSString *writer = saved.progressReplica;

  DDIOSStore *loaded = [[DDIOSStore alloc] initWithURL:url];
  XCTAssertNil(loaded.error);
  XCTAssertEqual(loaded.loadCaveChoice, 4);
  XCTAssertEqual(loaded.loadCaseIndex, 17);
  XCTAssertEqual(loaded.loadBest, 812);
  XCTAssertEqual(loaded.loadLandState, 0x52);
  XCTAssertEqualObjects(loaded.loadReleaseSeen, @"test-build");
  XCTAssertEqualObjects(loaded.loadHighScores, @"3:1;1,100,2,1,0,0,0,0,4,1,3,4,100,0,0,5,17");
  XCTAssertEqual([loaded loadLandBestWithInt:2], 900);
  XCTAssertEqual(loaded.loadPlayerSettings, 98329);
  XCTAssertEqual(loaded.loadCollected, 0x12345);
  XCTAssertEqual([loaded loadCollectionCounts]->buffer_[0], 2);
  XCTAssertEqual([loaded loadCollectionCounts]->buffer_[DDCollect_COUNT - 1], 4);
  XCTAssertEqual(loaded.loadCollectTotal, 27);
  XCTAssertEqual(loaded.loadSteamerOpens, 8);
  XCTAssertEqual(loaded.loadStarWins, 3);
  XCTAssertEqual(loaded.loadMineCarts, 4);
  XCTAssertEqual(loaded.loadRosterState, 5);
  XCTAssertEqualObjects(loaded.progressReplica, writer);
  XCTAssertEqual([loaded loadProgress]->buffer_[0], 9);
  XCTAssertEqual([loaded loadProgress]->buffer_[3], 6);
  [[NSFileManager defaultManager] removeItemAtURL:url error:nil];
}

- (void)testCorruptionNeverReplacesTheOriginalSave {
  NSURL *url = [self temporaryFile];
  NSData *corrupt = [@"not a plist" dataUsingEncoding:NSUTF8StringEncoding];
  [self assertReadOnlyForData:corrupt atURL:url];
  [[NSFileManager defaultManager] removeItemAtURL:url error:nil];
}

- (void)testEmptyUnknownVersionAndChecksumMismatchNeverOverwrite {
  NSURL *url = [self temporaryFile];
  [self assertReadOnlyForData:[NSData data] atURL:url];
  [self assertReadOnlyForData:[self envelopeWithVersion:@"v99" checksum:nil] atURL:url];
  NSData *wrongChecksum = [NSMutableData dataWithLength:CC_SHA256_DIGEST_LENGTH];
  [self assertReadOnlyForData:[self envelopeWithVersion:@"v1" checksum:wrongChecksum] atURL:url];
  [[NSFileManager defaultManager] removeItemAtURL:url error:nil];
}

- (void)testFailedFirstWriteReportsError {
  NSURL *base = [NSURL fileURLWithPath:NSTemporaryDirectory() isDirectory:YES];
  NSURL *missingDirectory = [base URLByAppendingPathComponent:NSUUID.UUID.UUIDString isDirectory:YES];
  NSURL *url = [missingDirectory URLByAppendingPathComponent:@"store.plist"];
  DDIOSStore *store = [[DDIOSStore alloc] initWithURL:url];
  XCTAssertNotNil(store.error);
  XCTAssertFalse([[NSFileManager defaultManager] fileExistsAtPath:url.path]);
}

@end
