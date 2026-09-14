#import "DDViewport.h"

DDViewport DDViewportMake(CGRect bounds, UIEdgeInsets safeArea, BOOL tablet) {
    DDViewport viewport = {0};
    if (!tablet) {
        viewport.frame = bounds;
        viewport.size = bounds.size;
        viewport.insets = safeArea;
        viewport.insets.top += 44;
        viewport.navigation = CGRectMake(CGRectGetMaxX(bounds) - safeArea.right - 46,
                                         CGRectGetMinY(bounds) + safeArea.top, 44, 44);
        viewport.scale = 1;
        return viewport;
    }
    CGRect available = UIEdgeInsetsInsetRect(bounds, safeArea);
    // Reserve an unscaled native pause target above the score/lives HUD.
    CGFloat height = MAX(0, available.size.height - 44);
    viewport.size = CGSizeMake(390, 800);
    viewport.scale = MAX(0, MIN(MAX(0, available.size.width) / 390, height / 800));
    CGSize fitted = CGSizeMake(390 * viewport.scale, 800 * viewport.scale);
    viewport.frame = CGRectMake(CGRectGetMidX(available) - fitted.width / 2,
        CGRectGetMidY(available) - (fitted.height + 44) / 2 + 44, fitted.width, fitted.height);
    viewport.navigation = CGRectMake(CGRectGetMaxX(viewport.frame) - 44,
                                      CGRectGetMinY(viewport.frame) - 44, 44, 44);
    return viewport;
}

CGPoint DDViewportPoint(DDViewport viewport, CGPoint point) {
    if (viewport.scale <= 0) return CGPointZero;
    return CGPointMake((point.x - viewport.frame.origin.x) / viewport.scale,
                       (point.y - viewport.frame.origin.y) / viewport.scale);
}
