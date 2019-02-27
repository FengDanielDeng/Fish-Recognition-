package camerakit;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.widget.ImageView;

import java.util.ArrayList;

public class MyImageView extends ImageView {
    public static class ViewPoint
    {
        public  float x;
        public  float y;
        public ViewPoint(){

        }
        public ViewPoint(ImageView v,float x, float y) {
            this.x = x;
            this.y = y;
            float [] pts=getPointerCoords(v,x,y);
            rx=pts[0];
            ry=pts[1];
        }

        //实际坐标
        public  float rx;
        public  float ry;
    }

    //表示一条线
    public class Line
    {
        public  Line(){
            pt1=new ViewPoint();
            pt2=new ViewPoint();
        }
        public  ViewPoint pt1;
        public ViewPoint pt2;
    }
    //二维码端点
   public ArrayList<ViewPoint> ptEnd;

    public   float clickX;
    public  float clickY;
    public  float rclickX;
    public  float rclickY;
   public Line curLine;
    public MyImageView(Context context) {
        super(context);
//
    }
    /**
     * 该构造方法在静态引入XML文件中是必须的
     *
     * @param context
     * @param paramAttributeSet
     */
    public MyImageView(Context context, AttributeSet paramAttributeSet) {
        super(context, paramAttributeSet);
        this.setScaleType(ScaleType.FIT_CENTER);
        curLine=new Line();
    }
    @Override
    public boolean onTouchEvent(MotionEvent event)
    {

        //获取坐标
        clickX = event.getX();
        clickY = event.getY();
        float [] pts=getPointerCoords(clickX,clickY);
        rclickX=pts[0];
        rclickY=pts[1];

        ViewPoint point = new ViewPoint();
        point.x = clickX;
        point.y = clickY;

        float[] rpt=getPointerCoords(this,event);
        point.rx = rpt[0];
        point.ry = rpt[1];
        if (event.getAction() == MotionEvent.ACTION_DOWN)
        {
            invalidate();
            curLine.pt1=point;
            curLine.pt2=point;
            return true;
        }
        else if (event.getAction() == MotionEvent.ACTION_MOVE)
        {

            //在移动时添加所经过的点
            curLine.pt2=point;

            invalidate();
return true;
        }
        else if (event.getAction() == MotionEvent.ACTION_UP)
        {
            //添加画过的线
            curLine.pt2=point;

            invalidate();
            return true;
        }


        return super.onTouchEvent(event);
    }
    @Override
    protected void onDraw(Canvas canvas)
    {
        super.onDraw(canvas);
        float x1 = curLine.pt1.x;
        float y1 =  curLine.pt1.y;
        float x2 = curLine.pt2.x;
        float y2 =  curLine.pt2.y;
        Paint paint= new Paint();
        paint.setColor(Color.RED);// 设置红色
        paint.setStrokeWidth(8);
        canvas.drawLine(x1, y1, x2, y2, paint);

        if(ptEnd!=null&&ptEnd.size()==3){
            float pt1_x = ptEnd.get(0).x;
            float pt1_y = ptEnd.get(0).y;
            float pt2_x = ptEnd.get(1).x;
            float pt2_y = ptEnd.get(1).y;
            float pt3_x = ptEnd.get(2).x;
            float pt3_y = ptEnd.get(2).y;
            paint.setColor(Color.BLUE);// 设置红色
            paint.setStrokeWidth(5);
            canvas.drawLine(pt2_x, pt2_y, pt1_x, pt1_y, paint);
            canvas.drawLine(pt2_x, pt2_y, pt3_x, pt3_y, paint);
        }
        if(ptEnd!=null&&ptEnd.size()==4){
            paint.setColor(Color.BLUE);// 设置红色
            paint.setStrokeWidth(5);
//            for(int i=0;i<4;i++){
//                float pt1_x = ptEnd.get(i).x;
//                float pt1_y = ptEnd.get(i).y;
//                float pt2_x = ptEnd.get((i+1)%4).x;
//                float pt2_y = ptEnd.get((i+1)%4).y;
//                canvas.drawLine(pt2_x, pt2_y, pt1_x, pt1_y, paint);
//            }


            float pt1_x = ptEnd.get(0).x;
            float pt1_y = ptEnd.get(0).y;
            float pt2_x = ptEnd.get(1).x;
            float pt2_y = ptEnd.get(1).y;
            float pt3_x = ptEnd.get(2).x;
            float pt3_y = ptEnd.get(2).y;
            paint.setColor(Color.BLUE);// 设置红色
            paint.setStrokeWidth(5);
            canvas.drawLine(pt2_x, pt2_y, pt1_x, pt1_y, paint);
            canvas.drawLine(pt2_x, pt2_y, pt3_x, pt3_y, paint);

        }

    }

    public float[] getPointerCoords(ImageView view, MotionEvent e) {
        final int index = e.getActionIndex();
        final float[] coords = new float[]{e.getX(index), e.getY(index)};
        Matrix matrix = new Matrix();
        view.getImageMatrix().invert(matrix);
        matrix.postTranslate(view.getScrollX(), view.getScrollY());
        matrix.mapPoints(coords);
        return coords;
    }
    public float[] getPointerCoords(float x,float y) {

        final float[] coords = new float[]{x,y};
        Matrix matrix = new Matrix();
        getImageMatrix().invert(matrix);
        matrix.postTranslate(getScrollX(), getScrollY());
        matrix.mapPoints(coords);
        return coords;
    }

    static public float[] getPointerCoords(ImageView view,float x,float y) {

        final float[] coords = new float[]{x,y};
        Matrix matrix = new Matrix();
        view.getImageMatrix().invert(matrix);
        matrix.postTranslate(view.getScrollX(), view.getScrollY());
        matrix.mapPoints(coords);
        return coords;
    }

    public ArrayList<ViewPoint> getPtEnd() {
        return ptEnd;
    }

    public void setPtEnd(ArrayList<ViewPoint>  ptEnd) {
        this.ptEnd = ptEnd;
        invalidate();
    }

    @Override
    public void setImageBitmap(Bitmap bm) {
        super.setImageBitmap(bm);
        ptEnd=null;
        curLine=new Line();
        invalidate();
    }
}
