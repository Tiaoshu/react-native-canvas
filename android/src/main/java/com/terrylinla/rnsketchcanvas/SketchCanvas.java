package com.terrylinla.rnsketchcanvas;

import android.graphics.*;
import com.facebook.react.bridge.WritableMap;
import com.facebook.react.bridge.Arguments;
import com.facebook.react.uimanager.ThemedReactContext;
import com.facebook.react.uimanager.events.RCTEventEmitter;

import android.view.View;
import android.util.Log;
import android.os.Environment;
import android.util.Base64;

import java.io.FileOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

public class SketchCanvas extends View {  
  
    private ArrayList<SketchData> _paths = new ArrayList<SketchData>();
    private Set<Integer> _drawIds = new HashSet<>();
    private SketchData _currentPath = null;
    private Bitmap _bufferBitmap;
    private Canvas _bufferCanvas;
    private Paint _paint;
    private PorterDuffXfermode _clearPorterDuff;

    private ThemedReactContext mContext;
    
    public SketchCanvas(ThemedReactContext context) {  
        super(context);
        mContext = context;
        _paint = new Paint();
        _paint.setFilterBitmap(true);
        _paint.setStyle(Paint.Style.STROKE);
        _paint.setStrokeJoin(Paint.Join.ROUND);
        _paint.setStrokeCap(Paint.Cap.ROUND);
        _paint.setAntiAlias(true);
        _clearPorterDuff = new PorterDuffXfermode(PorterDuff.Mode.CLEAR);
    }

    private void initBuffer() {
        this._bufferBitmap = Bitmap.createBitmap(getWidth(), getHeight(), Bitmap.Config.ARGB_8888);
        this._bufferCanvas = new Canvas(_bufferBitmap);
    }

    public void clear() {
        this._paths.clear();
        this._drawIds.clear();
        this._currentPath = null;
        if (this._bufferBitmap != null) {
            this._bufferBitmap.recycle();
            this._bufferBitmap = null;
            this._bufferCanvas = null;
        }
        invalidateCanvas(true);
    }

    public void newPath(int id, int strokeColor, int strokeWidth, boolean isEraser) {
        this._currentPath = new SketchData(id, strokeColor, strokeWidth, isEraser);
        this._paths.add(this._currentPath);
        invalidateCanvas(true);
    }

    public void addPoint(float x, float y) {
        this._currentPath.addPoint(new PointF(x, y));
        invalidateCanvas(false);
    }

    public void addPath(int id, int strokeColor, int strokeWidth, ArrayList<PointF> points, boolean isEraser) {
        boolean exist = false;
        for(SketchData data: this._paths) {
            if (data.id == id) {
                exist = true;
                break;
            }
        }

        if (!exist) {
            this._paths.add(new SketchData(id, strokeColor, strokeWidth, isEraser, points));
            invalidateCanvas(true);
        }
    }

    public void deletePath(int id) {
        int index = -1;
        for(int i=0; i<this._paths.size(); i++) {
            if (this._paths.get(i).id == id) {
                index = i;
                break;
            }
        }

        if (index > -1) {
            this._paths.remove(index);
            invalidateCanvas(true);
        }
    }

    public void onSaved(boolean success) {
        WritableMap event = Arguments.createMap();
        event.putBoolean("success", success);
        mContext.getJSModule(RCTEventEmitter.class).receiveEvent(
            getId(),
            "topChange",
            event);
    }

    public void save(String format, String folder, String filename, boolean transparent) {
        File f = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES) + File.separator + folder);
        boolean success = true;
        if (!f.exists())   success = f.mkdirs();
        if (success) {
            Bitmap  bitmap = Bitmap.createBitmap(this.getWidth(), this.getHeight(), Bitmap.Config.ARGB_8888);
            Canvas canvas = new Canvas(bitmap);
            if (format.equals("png")) {
                canvas.drawARGB(transparent ? 0 : 255, 255, 255, 255);
            } else {
                canvas.drawARGB(255, 255, 255, 255);
            }
            this.configureAllPath(canvas);

            File file = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES) + 
                File.separator + folder + File.separator + filename + (format.equals("png") ? ".png" : ".jpg"));
            try {
                bitmap.compress(
                    format.equals("png") ? Bitmap.CompressFormat.PNG : Bitmap.CompressFormat.JPEG, 
                    format.equals("png") ? 100 : 90, 
                    new FileOutputStream(file));
                this.onSaved(true);
            } catch (Exception e) {
                e.printStackTrace();
                this.onSaved(false);
            }   
        } else {
            Log.e("SketchCanvas", "Failed to create folder!");
            this.onSaved(false);
        }
    }

    public void end() {
        if (this._currentPath != null) {
            this._currentPath.end();
        }
    }

    public String getBase64(String format, boolean transparent) {
        return Base64.encodeToString(getBytes(format, transparent), Base64.DEFAULT);
    }

    public byte[] getBytes(String format, boolean transparent) {
        int width = this.getWidth();
        int height = this.getHeight();
        if (width <= 0 || height <= 0) {
            width = 1080;
            height = (int) (width * 0.65);
        }
        Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        if (format.equals("png")) {
            canvas.drawARGB(transparent ? 0 : 255, 255, 255, 255);
        } else {
            canvas.drawARGB(255, 255, 255, 255);
        }
        this.configureAllPath(canvas);

        ByteArrayOutputStream byteArrayOS = new ByteArrayOutputStream();
        bitmap.compress(
                format.equals("png") ? Bitmap.CompressFormat.PNG : Bitmap.CompressFormat.JPEG,
                format.equals("png") ? 100 : 90,
                byteArrayOS);
        return byteArrayOS.toByteArray();
    }

    @Override  
    protected void onDraw(Canvas canvas) {
        if (_bufferBitmap == null) {
            initBuffer();
        }
        configurePath(_bufferCanvas);
        if (_bufferBitmap != null) {
            canvas.drawBitmap(_bufferBitmap, 0, 0, null);
        }
    }

    private void invalidateCanvas(boolean shouldDispatchEvent) {
        if (shouldDispatchEvent) {
            WritableMap event = Arguments.createMap();
            event.putInt("pathsUpdate", this._paths.size());
            mContext.getJSModule(RCTEventEmitter.class).receiveEvent(
                getId(),
                "topChange",
                event);
        }
        invalidate();
    }

    private void configureAllPath(Canvas canvas) {
        for (SketchData path : this._paths) {
            if (path.isEraser) {
                _paint.setXfermode(_clearPorterDuff);
                _paint.setColor(Color.TRANSPARENT);
            } else {
                _paint.setXfermode(null);
                _paint.setColor(path.strokeColor);
            }
            _paint.setStrokeWidth(path.strokeWidth);

            if (path.path != null) {
                // draw initial dot
                PointF origin = path.points.get(0);
                canvas.drawPoint(origin.x, origin.y, _paint);

                // draw path
                canvas.drawPath(path.path, _paint);
            } else {
                Path canvasPath = new Path();
                PointF previousPoint = null;
                for (PointF p : path.points) {
                    if (canvasPath.isEmpty()) {
                        canvas.drawPoint(p.x, p.y, _paint);
                        canvasPath.moveTo(p.x, p.y);
                    } else {
                        canvasPath.quadTo((previousPoint.x) / 1, (previousPoint.y) / 1, p.x, p.y);
                    }
                    previousPoint = p;
                }
                canvas.drawPath(canvasPath, _paint);
            }
        }
    }

    private void configurePath(Canvas canvas) {
        for (SketchData path : this._paths) {
            if (_currentPath != null && path.path != null && _drawIds.contains(path.id)) {
                continue;
            }
            if (_currentPath == null && _drawIds.contains(path.id)) {
                continue;
            }
            if (path.isEraser) {
                _paint.setXfermode(_clearPorterDuff);
                _paint.setColor(Color.TRANSPARENT);
            } else {
                _paint.setXfermode(null);
                _paint.setColor(path.strokeColor);
            }
            _paint.setStrokeWidth(path.strokeWidth);
            Path canvasPath = new Path();
            PointF previousPoint = null;
            for(PointF p: path.points) {
                if (canvasPath.isEmpty()) {
                    canvas.drawPoint(p.x, p.y, _paint);
                    canvasPath.moveTo(p.x, p.y);
                } else {
                    canvasPath.quadTo((previousPoint.x) / 1, (previousPoint.y) / 1, p.x, p.y);
                }
                previousPoint = p;
            }
            canvas.drawPath(canvasPath, _paint);
            _drawIds.add(path.id);
        }
    }

//    private void drawPath(Canvas canvas) {
//        for(SketchData path: this._paths) {
//            Paint paint = new Paint();
//            Log.e("SketchData", path.strokeColor + "");
//            if (path.strokeColor == Color.BLACK) {
//                Log.e("SketchData", "Eraser");
//                paint.setColor(Color.TRANSPARENT);
//                paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.CLEAR));
//                Path canvasPath = new Path();
//                PointF previousPoint = null;
//                for(PointF p: path.points) {
//                    if (canvasPath.isEmpty()) {
//                        canvas.drawPoint(p.x, p.y, paint);
//                        canvasPath.moveTo(p.x, p.y);
//                    } else {
//                        canvasPath.quadTo((previousPoint.x) / 1, (previousPoint.y) / 1, p.x, p.y);
//                    }
//                    previousPoint = p;
//                }
//
//                canvas.drawPath(canvasPath, paint);
//            } else {
//                paint.setColor(path.strokeColor);
//                paint.setStrokeWidth(path.strokeWidth);
//                paint.setStyle(Paint.Style.STROKE);
//                paint.setStrokeCap(Paint.Cap.ROUND);
//                paint.setAntiAlias(true);
//
//                if (path.path != null) {
//
//                    // draw initial dot
//                    PointF origin = path.points.get(0);
//                    canvas.drawPoint(origin.x, origin.y, paint);
//
//                    // draw path
//                    canvas.drawPath(path.path, paint);
//                } else {
//                    Path canvasPath = new Path();
//                    PointF previousPoint = null;
//                    for(PointF p: path.points) {
//                        if (canvasPath.isEmpty()) {
//                            canvas.drawPoint(p.x, p.y, paint);
//                            canvasPath.moveTo(p.x, p.y);
//                        } else {
//                            canvasPath.quadTo((previousPoint.x) / 1, (previousPoint.y) / 1, p.x, p.y);
//                        }
//                        previousPoint = p;
//                    }
//
//                    canvas.drawPath(canvasPath, paint);
//                }
//            }
//        }
//    }
}  