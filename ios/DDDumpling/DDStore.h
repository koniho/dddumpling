#import <Foundation/Foundation.h>
#import "com/dddumpling/game/GameCore.h"

/** A durable, local implementation of GameCore.Store for the iOS app. */
@interface DDIOSStore : NSObject <DDGameCore_Store>

/** Set when a save could not be trusted or written; a healthy store is writable. */
@property(nonatomic, readonly, nullable) NSString *error;

/** Test seam for an isolated store file. The production host uses -init. */
- (instancetype _Nonnull)initWithURL:(NSURL * _Nonnull)fileURL;
- (BOOL)bindCloudPlayer:(NSString * _Nonnull)player identity:(NSData * _Nonnull)identity;
@end
