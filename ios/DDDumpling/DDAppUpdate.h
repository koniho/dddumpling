#import <Foundation/Foundation.h>
@class DDIOSGame;

@interface DDAppUpdate : NSObject
- (void)check:(DDIOSGame *)game;
- (void)openListing;
+ (NSURL *)listingURL:(NSBundle *)bundle;
+ (NSString *)newerVersion:(id)json bundleID:(NSString *)bundleID appID:(NSString *)appID
                 installed:(NSString *)installed system:(NSString *)system;
@end
