#import "RNSketchCanvasManager.h"
#import "RNSketchCanvas.h"
#import "RNSketchData.h"
#import "RNSketchCanvasDelegate.h"
#import <React/RCTEventDispatcher.h>
#import <React/RCTView.h>
#import <React/UIView+React.h>
#import "AppConst.h"
#import "AppMacro.h"
#import "AppEvents.h"
#import "NSObject+Utils.h"

static NSInteger kOffset;
static CGSize kContainerSize;


@implementation RNSketchCanvas
{
    RCTEventDispatcher *_eventDispatcher;
    NSMutableArray *_paths;
    RNSketchData *_currentPath;
    NSArray *_currentPoints;

    RNSketchCanvasDelegate *delegate;

    UIBezierPath *_bezierPath;
    CAShapeLayer *_layer;

    BOOL _isRestore;

    NSMutableArray *_lines;
    BOOL _isEraser;
    BOOL _isOtherLine;
    BOOL _shouldDispatchEvent;
    UIColor *_lineColor;
    NSInteger _lineWidth;
    NSMutableDictionary *_pathIDs;


}

- (instancetype)initWithEventDispatcher:(RCTEventDispatcher *)eventDispatcher
{
    self = [super init];
    if (self) {
        _eventDispatcher = eventDispatcher;
        _pathIDs = [NSMutableDictionary dictionary];
        _lines = [NSMutableArray array];
        
        self.clipsToBounds = YES;
    }
    return self;
}

- (void)newPath:(int) pathId strokeColor:(UIColor*) strokeColor strokeWidth:(int) strokeWidth {
    _isEraser = NO;
    _isOtherLine = YES;
    _lineColor = strokeColor;
    _lineWidth = strokeWidth;
    _shouldDispatchEvent = NO;
    [self preDrawWithID:@(pathId).stringValue restore:NO];
}

- (void)erasePath:(int)pathId strokeColor:(UIColor *)strokeColor strokeWidth:(int)strokeWidth
{
    _isEraser = YES;
    _isOtherLine = YES;
    _shouldDispatchEvent = NO;
    UIImage *image = [UIImage imageNamed:@"game_draw_bg"] ;
    _lineColor = [[UIColor alloc] initWithPatternImage:image];
    _lineWidth = strokeWidth;
    [self preDrawWithID:@(pathId).stringValue restore:NO];
}

- (void) addPath:(int) pathId strokeColor:(UIColor*) strokeColor strokeWidth:(int) strokeWidth points:(NSArray*) points erase:(BOOL)isErase{
    if (![_pathIDs objectForKey:@(pathId)] ) {
        NSLog(@"DRAW======== %@", points);
        _isEraser = isErase;
        _isOtherLine = YES;
        _shouldDispatchEvent = YES;
        if (isErase) {
            UIImage *image = [UIImage imageNamed:@"game_draw_bg"] ;
            _lineColor = [[UIColor alloc] initWithPatternImage:image];
        }else {
            _lineColor = strokeColor;
        }
        _lineWidth = strokeWidth;
        if ( points.count == 1 ) {
            [self preDrawWithID:@(pathId).stringValue restore:NO];
            CGPoint startPoint = [points.firstObject CGPointValue];
            [self startWithPoint:startPoint];
        }else {
            CGPoint endPoint = [points.lastObject CGPointValue];
            [self addPoint:endPoint];
        }
    }
}

- (void)restorePath:(int)pathId strokeColor:(UIColor *)strokeColor strokeWidth:(int)strokeWidth points:(NSArray *)points erase:(BOOL)isErase
{
    if (![_pathIDs objectForKey:@(pathId)] ) {
        _isEraser = isErase;
        _isOtherLine = YES;
        _shouldDispatchEvent = YES;
        if (isErase) {
            UIImage *image = [UIImage imageNamed:@"game_draw_bg"] ;
            _lineColor = [[UIColor alloc] initWithPatternImage:image];
        }else {
            _lineColor = strokeColor;
        }
        _lineWidth = strokeWidth;

        CGPoint startPoint = [points.firstObject CGPointValue];
        [self preDrawWithID:@(pathId).stringValue restore:YES];
        [self startWithPoint:startPoint];
        for (NSInteger index = 1; index < points.count; index++) {
            CGPoint point = [points[index] CGPointValue];
            [_bezierPath addLineToPoint:point];
        }
        _layer.path = _bezierPath.CGPath;
    }
}

- (void)deletePath:(int) pathId {
    int index = -1;
    for(int i=0; i<_paths.count; i++) {
        if (((RNSketchData*)_paths[i]).pathId == pathId) {
            index = i;
            break;
        }
    }
    
    if (index > -1) {
        [_paths removeObjectAtIndex: index];
    }
}

- (void)addPointX: (float)x Y: (float)y {
    if (_isOtherLine) {
        [self startWithPoint:CGPointMake(x, y)];
    }else {
        [self addPoint:CGPointMake(x, y)];
    }
    _isOtherLine = NO;
}

- (void)endPath {
    if (_currentPath) {
        [_currentPath end];
    }
}

- (void) clear {
    if (!_pathIDs.count) {
        return;
    }
    _isEraser = NO;
    self.layer.sublayers = nil;
    [_pathIDs removeAllObjects];
}

- (void) saveImageOfType: (NSString*) type withTransparentBackground: (BOOL) transparent {
    CGRect rect = self.frame;
    UIGraphicsBeginImageContextWithOptions(rect.size, !transparent, 0);
    CGContextRef context = UIGraphicsGetCurrentContext();
    if ([type isEqualToString: @"png"] && !transparent) {
        CGContextSetRGBFillColor(context, 1.0f, 1.0f, 1.0f, 1.0f);
        CGContextFillRect(context, CGRectMake(0, 0, rect.size.width, rect.size.height));
    }
    [_layer renderInContext:context];
    UIImage *img = UIGraphicsGetImageFromCurrentImageContext();
    UIGraphicsEndImageContext();
    if ([type isEqualToString: @"jpg"]) {
        UIImageWriteToSavedPhotosAlbum(img, self, @selector(image:didFinishSavingWithError:contextInfo:), nil);
    } else {
        UIImageWriteToSavedPhotosAlbum([UIImage imageWithData: UIImagePNGRepresentation(img)], self, @selector(image:didFinishSavingWithError:contextInfo:), nil);
    }
}

- (NSString*) transferToBase64OfType: (NSString*) type withTransparentBackground: (BOOL) transparent {
    CGRect rect = self.frame;
    
    UIGraphicsBeginImageContextWithOptions(rect.size, !transparent, 0);
    CGContextRef context = UIGraphicsGetCurrentContext();
    if ([type isEqualToString: @"png"] && !transparent) {
        CGContextSetRGBFillColor(context, 1.0f, 1.0f, 1.0f, 1.0f);
        CGContextFillRect(context, CGRectMake(0, 0, rect.size.width, rect.size.height));
    }
    [_layer renderInContext:context];
    UIImage *img = UIGraphicsGetImageFromCurrentImageContext();
    UIGraphicsEndImageContext();
    if ([type isEqualToString: @"jpg"]) {
        return [UIImageJPEGRepresentation(img, 0.9) base64EncodedStringWithOptions: NSDataBase64Encoding64CharacterLineLength];
    } else {
        return [UIImagePNGRepresentation(img) base64EncodedStringWithOptions: NSDataBase64Encoding64CharacterLineLength];
    }
}

- (void)image:(UIImage *)image didFinishSavingWithError:(NSError *)error contextInfo: (void *) contextInfo {
    if (_onChange) {
        _onChange(@{ @"success": error != nil ? @NO : @YES });
    }
}

#pragma Draw

- (void)preDrawWithID:(NSString *)pathID restore:(BOOL)isRestore
{
    if (pathID.length == 0) {
        return;
    }
    CGRect frame = self.bounds;

    if (_isEraser) {
        CGSize size = [self containerSize];
        CGFloat y = isRestore ? 0 : -[self offset];
        frame = CGRectMake(0, y, size.width, size.height);
    }

    UIBezierPath *path = [[UIBezierPath alloc] init];
    path.lineWidth = _lineWidth;
    path.lineCapStyle = kCGLineCapRound; //线条拐角
    path.lineJoinStyle = kCGLineCapRound; //终点处理
    _bezierPath = path;

    CAShapeLayer * slayer = [CAShapeLayer layer];
    slayer.frame = frame;
    slayer.backgroundColor = [UIColor clearColor].CGColor;
    slayer.fillColor = [UIColor clearColor].CGColor;
    slayer.lineCap = kCALineCapRound;
    slayer.lineJoin = kCALineJoinRound;
    slayer.lineWidth = _bezierPath.lineWidth;
    slayer.strokeColor = _lineColor.CGColor;
    [self.layer addSublayer:slayer];
    _layer = slayer;
    [_pathIDs setObject:_layer forKey:pathID];
}

- (void)startWithPoint:(CGPoint)startPoint
{
    CGPoint point = startPoint;
    point.y += [self offset];
    [_bezierPath moveToPoint:startPoint];
    [_bezierPath addLineToPoint:startPoint];
    _layer.path = _bezierPath.CGPath;
}

- (void)addPoint:(CGPoint)movePoint
{
    if (_onChange && _shouldDispatchEvent) {
        _onChange(@{ @"pathsUpdate": @(_pathIDs.allKeys.count) });
    }
    CGPoint point = movePoint;
    if (_isEraser) {
        point.y += [self offset];
    }
    [_bezierPath addLineToPoint:point];
    _layer.path = _bezierPath.CGPath;
}

- (NSInteger)offset
{
    return kOffset;
}

- (CGSize)containerSize
{
    return kContainerSize;
}

+ (void)setOffsetY:(NSInteger)offset containerSize:(CGSize)size
{
    kOffset = offset;
    kContainerSize = size;
}

@end
