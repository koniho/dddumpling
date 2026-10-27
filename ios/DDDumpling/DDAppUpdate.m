#import "DDAppUpdate.h"
#import "com/dddumpling/game/IOSGame.h"
#import <StoreKit/StoreKit.h>
#import <UIKit/UIKit.h>

@implementation DDAppUpdate
+ (NSURL *)listingURL:(NSBundle *)bundle {
    id value=[bundle objectForInfoDictionaryKey:@"DDPublicAppStoreURL"];
    NSURL *url=[value isKindOfClass:NSString.class]?[NSURL URLWithString:value]:nil;
    if(![url.scheme isEqualToString:@"https"] || ![url.host isEqualToString:@"apps.apple.com"]
            || ![url.lastPathComponent hasPrefix:@"id"])return nil;
    return url;
}
+ (BOOL)version:(id)value {
    if(![value isKindOfClass:NSString.class] || [value length]>40)return NO;
    return [value rangeOfString:@"^[0-9]{1,9}(\\.[0-9]{1,9}){0,3}$" options:NSRegularExpressionSearch].location!=NSNotFound;
}
+ (NSComparisonResult)compare:(NSString *)left to:(NSString *)right {
    NSArray *a=[left componentsSeparatedByString:@"."], *b=[right componentsSeparatedByString:@"."];
    for(NSUInteger i=0;i<MAX(a.count,b.count);i++) {
        long long x=i<a.count?[a[i] longLongValue]:0, y=i<b.count?[b[i] longLongValue]:0;
        if(x!=y)return x<y?NSOrderedAscending:NSOrderedDescending;
    }
    return NSOrderedSame;
}
+ (NSString *)newerVersion:(id)json bundleID:(NSString *)bundleID appID:(NSString *)appID
                 installed:(NSString *)installed system:(NSString *)system {
    if(![json isKindOfClass:NSDictionary.class] || ![self version:installed] || ![self version:system])return nil;
    id results=json[@"results"];
    if(![results isKindOfClass:NSArray.class])return nil;
    for(id row in results) {
        if(![row isKindOfClass:NSDictionary.class])continue;
        id track=row[@"trackId"], version=row[@"version"], minimum=row[@"minimumOsVersion"];
        if(![row[@"bundleId"] isEqual:bundleID] || ![track isKindOfClass:NSNumber.class]
                || ![[track stringValue] isEqual:appID] || ![row[@"kind"] isEqual:@"software"]
                || ![self version:version] || ![self version:minimum])continue;
        if([self compare:version to:installed]==NSOrderedDescending
                && [self compare:minimum to:system]!=NSOrderedDescending)return version;
    }
    return nil;
}
- (void)check:(DDIOSGame *)game {
#if DEBUG
    return;
#else
    NSBundle *bundle=NSBundle.mainBundle;
    // TestFlight uses a sandbox receipt; local/ad-hoc installs must not advertise store updates.
    NSURL *receipt=bundle.appStoreReceiptURL;
    if(![receipt.lastPathComponent isEqual:@"receipt"]
            || ![NSFileManager.defaultManager fileExistsAtPath:receipt.path])return;
    NSURL *listing=[DDAppUpdate listingURL:bundle];
    if(!listing)return;
    NSString *country=[DDIOSGame storeCountryWithNSString:SKPaymentQueue.defaultQueue.storefront.countryCode];
    if(!country.length || ![game beginUpdateCheck])return;
    NSString *appID=[listing.lastPathComponent substringFromIndex:2];
    NSURLComponents *lookup=[NSURLComponents componentsWithString:@"https://itunes.apple.com/lookup"];
    lookup.queryItems=@[[NSURLQueryItem queryItemWithName:@"id" value:appID],
                        [NSURLQueryItem queryItemWithName:@"country" value:country],
                        [NSURLQueryItem queryItemWithName:@"entity" value:@"software"]];
    NSMutableURLRequest *request=[NSMutableURLRequest requestWithURL:lookup.URL
            cachePolicy:NSURLRequestReloadIgnoringLocalCacheData timeoutInterval:10];
    NSURLSessionConfiguration *configuration=NSURLSessionConfiguration.ephemeralSessionConfiguration;
    configuration.timeoutIntervalForResource=15;
    NSURLSession *session=[NSURLSession sessionWithConfiguration:configuration];
    NSString *installed=[bundle objectForInfoDictionaryKey:@"CFBundleShortVersionString"];
    NSString *system=UIDevice.currentDevice.systemVersion;
    [[session dataTaskWithRequest:request completionHandler:^(NSData *data,NSURLResponse *response,NSError *error) {
        NSString *version=nil;
        if(!error && [response isKindOfClass:NSHTTPURLResponse.class]
                && ((NSHTTPURLResponse *)response).statusCode==200 && data.length>0 && data.length<1024*1024) {
            id json=[NSJSONSerialization JSONObjectWithData:data options:0 error:nil];
            version=[DDAppUpdate newerVersion:json bundleID:bundle.bundleIdentifier appID:appID installed:installed system:system];
        }
        dispatch_async(dispatch_get_main_queue(), ^{ [game storeUpdateAvailableWithNSString:version?:@""]; });
    }] resume];
    [session finishTasksAndInvalidate];
#endif
}
- (void)openListing {
    NSURL *url=[DDAppUpdate listingURL:NSBundle.mainBundle];
    if(url)[UIApplication.sharedApplication openURL:url options:@{} completionHandler:nil];
}
@end
