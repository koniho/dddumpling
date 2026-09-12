#import <UIKit/UIKit.h>
#import "DDAppDelegate.h"
#import "IOSClass.h"
#import "NSString+JavaString.h"

int main(int argc, char *argv[]) {
    @autoreleasepool {
        // J2ObjC 3.1's string/reflection initializers can deadlock if audio wins their first use.
        // Finish them on one thread before any synthesis queue or game object is created.
        IOSClass_initialize();
        NSString_class_();
        return UIApplicationMain(argc, argv, nil, NSStringFromClass(DDAppDelegate.class));
    }
}
