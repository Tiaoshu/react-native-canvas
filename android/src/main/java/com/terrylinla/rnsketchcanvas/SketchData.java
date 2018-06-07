package com.terrylinla.rnsketchcanvas;

import android.graphics.PointF;
import android.graphics.Color;
import android.graphics.Path;

import java.util.ArrayList;

public class SketchData {
    public ArrayList<PointF> points = new ArrayList<PointF>();
    public int id, strokeColor, strokeWidth;
    public boolean isEraser;
    public Path path;

    public SketchData(int id, int strokeColor, int strokeWidth, boolean isEraser) {
        this.id = id;
        this.strokeColor = strokeColor;
        this.strokeWidth = strokeWidth;
        this.isEraser = isEraser;
    }

    public SketchData(int id, int strokeColor, int strokeWidth, boolean isEraser, ArrayList<PointF> points) {
        this.id = id;
        this.strokeColor = strokeColor;
        this.strokeWidth = strokeWidth;
        this.isEraser = isEraser;
        this.points.addAll(points);
    }

    public void addPoint(PointF p) {
        this.points.add(p);
    }

    public void end() {
        Path canvasPath = new Path();
        for(PointF p: this.points) {
            if (canvasPath.isEmpty()) canvasPath.moveTo(p.x, p.y);
            else canvasPath.lineTo(p.x, p.y);
        }

        this.path = canvasPath;
    }
}