#import <UIKit/UIKit.h>

@class RCTEventDispatcher;

@interface RNSketchCanvas : UIView

@property (nonatomic, copy) void (^onChange)(NSDictionary *body);

- (instancetype)initWithEventDispatcher:(RCTEventDispatcher *)eventDispatcher;

- (void)newPath:(int) pathId strokeColor:(UIColor*) strokeColor strokeWidth:(int) strokeWidth;
- (void)erasePath:(int)pathId strokeColor:(UIColor *)strokeColor strokeWidth:(int)strokeWidth;

- (void)addPath:(int) pathId strokeColor:(UIColor*) strokeColor strokeWidth:(int) strokeWidth points:(NSArray*) points erase:(BOOL)isErase;
- (void)restorePath:(int) pathId strokeColor:(UIColor*) strokeColor strokeWidth:(int) strokeWidth points:(NSArray*) points erase:(BOOL)isErase;
- (void)deletePath:(int) pathId;
- (void)addPointX: (float)x Y: (float)y;
- (void)endPath;
- (void)clear;
- (void)saveImageOfType: (NSString*) type withTransparentBackground: (BOOL) transparent;
- (NSString*) transferToBase64OfType: (NSString*) type withTransparentBackground: (BOOL) transparent;

+ (void)setOffsetY:(NSInteger)offset containerSize:(CGSize)size;

@end
