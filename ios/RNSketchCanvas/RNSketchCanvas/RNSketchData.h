//
//  RNSketchCanvasData.h
//  RNSketchCanvas
//
//  Created by terry on 03/08/2017.
//  Copyright © 2017 Terry. All rights reserved.
//

#import <Foundation/Foundation.h>
#import <UIKit/UIKit.h>

@interface RNSketchData : NSObject

@property CGPoint* cgPoints;
@property int pointCount, pathId, strokeWidth;
@property UIColor* strokeColor;
@property BOOL isErase;

- (instancetype)initWithId:(int) pathId strokeColor:(UIColor*) strokeColor strokeWidth:(int) strokeWidth points: (NSArray*) points erase:(BOOL)isErase;
- (instancetype)initWithId:(int) pathId strokeColor:(UIColor*) strokeColor strokeWidth:(int) strokeWidth erase:(BOOL)isErase;

- (NSArray*)addPoint:(CGPoint) point;
- (void)end;

@end
