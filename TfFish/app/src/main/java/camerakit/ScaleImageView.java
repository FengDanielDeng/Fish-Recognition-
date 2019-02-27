package camerakit;

import android.content.Context;
import android.graphics.Matrix;
import android.graphics.PointF;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.widget.ImageView;

/**
 * Created by OID on 2016/11/25.
 */

public class ScaleImageView extends ImageView {

    public ScaleImageView(Context context) {
        super(context);
    }

    private PointF startPoint = new PointF();
    private Matrix matrix = new Matrix();
    private Matrix currentMaritx = new Matrix();
    private Matrix iniMaritx = new Matrix();

    private int mode = 0;//用于标记模式
    private static final int DRAG = 1;//拖动
    private static final int ZOOM = 2;//放大
    private float startDis = 0;
    private PointF midPoint;//中心点
    private static long firstTime=0;
    private static boolean firstTouch=true;
    /**
     * 该构造方法在静态引入XML文件中是必须的
     *
     * @param context
     * @param paramAttributeSet
     */
    public ScaleImageView(Context context, AttributeSet paramAttributeSet) {
        super(context, paramAttributeSet);
        this.setScaleType(ScaleType.FIT_CENTER);
        iniMaritx.set(this.getImageMatrix());
        //this.setScaleType(ScaleType.MATRIX);
       // this.setImageMatrix(iniMaritx);
    }
    @Override
    public boolean onTouchEvent(MotionEvent event) {

        switch (event.getAction() & MotionEvent.ACTION_MASK) {
            case MotionEvent.ACTION_DOWN:

                //----------------------------判断双击---------------------------------->
                if(firstTouch==true){
                    firstTime = System.currentTimeMillis();
                    firstTouch=false;
                }else if(System.currentTimeMillis()-firstTime>200){
                    firstTime = System.currentTimeMillis();
                }
                else if((firstTouch==false)&&(System.currentTimeMillis()-firstTime>200)){
                    firstTouch=true;
                }else if((firstTouch==false)&&(System.currentTimeMillis()-firstTime>5/*消抖*/)){
                    firstTouch=true;
                    this.setScaleType(ScaleType.FIT_CENTER);
                    matrix.set(iniMaritx);
                    this.setImageMatrix(iniMaritx);
                }
                //-----------------------------------------------------------------------------<
                mode = DRAG;
                currentMaritx.set(this.getImageMatrix());//记录ImageView当期的移动位置
                startPoint.set(event.getX(), event.getY());//开始点
                break;

            case MotionEvent.ACTION_MOVE://移动事件
                if (mode == DRAG) {//图片拖动事件
                    this.setScaleType(ScaleType.MATRIX);
                    float dx = event.getX() - startPoint.x;//x轴移动距离
                    float dy = event.getY() - startPoint.y;
                    matrix.set(currentMaritx);//在当前的位置基础上移动
                    matrix.postTranslate(dx, dy);

                } else if (mode == ZOOM) {//图片放大事件
                    float endDis = distance(event);//结束距离
                    if (endDis > 10f) {
                        this.setScaleType(ScaleType.MATRIX);
                        float scale = endDis / startDis;//放大倍数
                        //Log.v("scale=", String.valueOf(scale));
                        matrix.set(currentMaritx);
                        matrix.postScale(scale, scale, midPoint.x, midPoint.y);
                    }


                }

                break;

            case MotionEvent.ACTION_UP:
                mode = 0;
                break;
            //有手指离开屏幕，但屏幕还有触点(手指)
            case MotionEvent.ACTION_POINTER_UP:
                mode = 0;
                break;
            //当屏幕上已经有触点（手指）,再有一个手指压下屏幕
            case MotionEvent.ACTION_POINTER_DOWN:
                mode = ZOOM;
                startDis = distance(event);

                if (startDis > 10f) {//避免手指上有两个茧
                    midPoint = mid(event);
                    currentMaritx.set(this.getImageMatrix());//记录当前的缩放倍数
                }


                break;


        }
        this.setImageMatrix(matrix);
        return true;
    }

    /**
     * 两点之间的距离
     *
     * @param event
     * @return
     */
    private static float distance(MotionEvent event) {
        //两根线的距离
        float dx = event.getX(1) - event.getX(0);
        float dy = event.getY(1) - event.getY(0);
        return (float) Math.sqrt(dx * dx + dy * dy);
    }

    /**
     * 计算两点之间中心点的距离
     *
     * @param event
     * @return
     */
    private static PointF mid(MotionEvent event) {
        float midx = event.getX(1) + event.getX(0);
        float midy = event.getY(1) - event.getY(0);

        return new PointF(midx / 2, midy / 2);
    }

}