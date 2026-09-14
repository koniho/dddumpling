#import <UIKit/UIKit.h>

// The iPad canvas stays in one coordinate system while its window rotates or resizes.
typedef struct {
    CGRect frame;
    CGSize size;
    UIEdgeInsets insets;
    CGRect navigation;
    CGFloat scale;
} DDViewport;

FOUNDATION_EXPORT DDViewport DDViewportMake(CGRect bounds, UIEdgeInsets safeArea, BOOL tablet);
FOUNDATION_EXPORT CGPoint DDViewportPoint(DDViewport viewport, CGPoint point);
