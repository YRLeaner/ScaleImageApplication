package com.example.tyr.scaleimageapplication;

import android.content.Context;
import android.graphics.Matrix;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.support.annotation.Nullable;
import android.support.v4.view.ViewPager;
import android.util.AttributeSet;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewTreeObserver;

/**
 * Created by tyr on 2018/12/25.
 */

public class ZoomImageView extends android.support.v7.widget.AppCompatImageView implements ViewTreeObserver.OnGlobalLayoutListener,
        ScaleGestureDetector.OnScaleGestureListener,View.OnTouchListener {

    //初始化操作
    private boolean mOnce = false;
    //初始化时缩放的值
    private float mInitScale;
    //双击时缩放的值
    private float mMidScale;
    //最大缩放值
    private float mMaxScale;

    /**
     * 自由移动 记录上一次多点触控的数量
     */
    private int mLastPointerCount;
    private float mLastX;
    private float mLastY;
    private int mTouchSlop;
    /**
     * 捕获用户多指触控时缩放的比例
     */
    private ScaleGestureDetector mScaleGestureDetector;
    private Matrix mScaleMatrix;
    private boolean isCanDrag;

    private boolean isCheckLeftAndRight;
    private boolean isCheckTopAndBottom;

    /**
     * 双击方法缩小
     * @param context
     */
    private GestureDetector mGestureDetector;
    private boolean isAutoScale;
    public ZoomImageView(Context context) {
        this(context,null);
    }

    public ZoomImageView(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs,0);
    }

    public ZoomImageView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        mScaleMatrix = new Matrix();
        setScaleType(ScaleType.MATRIX);
        mScaleGestureDetector = new ScaleGestureDetector(context,this);
        setOnTouchListener(this);
        mTouchSlop = ViewConfiguration.get(context).getScaledTouchSlop();
        mGestureDetector = new GestureDetector(context,new GestureDetector.SimpleOnGestureListener(){
            @Override
            public boolean onDoubleTap(MotionEvent e) {
                if (isAutoScale){
                    return true;
                }

                float x = e.getX();
                float y = e.getY();
                if (getScale()<mMidScale){
                    postDelayed(new AutoScaleRunable(mMidScale,x,y),5);
                    isAutoScale = true;
                }else {
                    postDelayed(new AutoScaleRunable(mInitScale,x,y),5);
                    isAutoScale = true;
                }

                return super.onDoubleTap(e);
            }
        });
    }

    /**
     * 自动放大缩小
     */
    private class AutoScaleRunable implements Runnable{
        /**
         * 目标的缩放值
         */
        private float mTarget;
        /**
         * 缩放中心点
         */
        private float x;
        private float y;

        /**
         * 缩小放大梯度
         */
        private final float BIGGER = 1.5f;
        private final float SMALL = 0.5f;

        private float tmpScale;

        public AutoScaleRunable(float mTarget, float x, float y) {
            this.mTarget = mTarget;
            this.x = x;
            this.y = y;

            if (getScale()<mTarget){
                tmpScale = BIGGER;
            }

            if (getScale()>mTarget){
                tmpScale = SMALL;
            }
        }

        @Override
        public void run() {
            mScaleMatrix.postScale(tmpScale,tmpScale,x,y);
            checkBorderAndCenterWhenScale();
            setImageMatrix(mScaleMatrix);
            float currentScale = getScale();
            if ((tmpScale>1.0f&&currentScale<mTarget)||
                    (tmpScale<1.0f&&currentScale>mTarget)){
                postDelayed(this,5);
            }else {
                //设置为目标值
                float scale = mTarget/currentScale;
                mScaleMatrix.postScale(scale,scale,x,y);
                checkBorderAndCenterWhenScale();
                setImageMatrix(mScaleMatrix);
                isAutoScale = false;
            }

        }
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        getViewTreeObserver().addOnGlobalLayoutListener(this);
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        getViewTreeObserver().removeGlobalOnLayoutListener(this);
    }

    /*
     全局布局完成后
     */
    @Override
    public void onGlobalLayout() {
        if(!mOnce){
            //控件的宽和高
            int width = getWidth();
            int height = getHeight();
            //获取图片和宽和高
            Drawable d = getDrawable();
            if (d==null) {
                return;
            }

            int dw = d.getIntrinsicWidth();
            int dh = d.getIntrinsicHeight();

            float scale = 1.0f;

            if (dw>width&&dh<height){
                scale = width*1.0f/dw;
            }else if (dh>height&&dw<width){
                scale = height*1.0f/dh;
            }else if (dw>width&&dh>height){
                scale = Math.min(width*1.0f/dw,height*1.0f/dh);
            }else if (dw<width&&dh<height){
                scale = Math.min(width*1.0f/dw,height*1.0f/dh);
            }

            /**
             * 初始化时缩放比例
             */
            mInitScale = scale;
            mMaxScale = mInitScale*4;
            mMidScale = mInitScale*2;

            //图片移动到控件的中心
            int dx = getWidth()/2-dw/2;
            int dy = getHeight()/2-dh/2;

            mScaleMatrix.postTranslate(dx,dy);
            mScaleMatrix.postScale(mInitScale,mInitScale,width/2,height/2);
            setImageMatrix(mScaleMatrix);
            mOnce = true;
        }
    }


    public float getScale(){
        float[] values = new float[9];
        mScaleMatrix.getValues(values);
        return values[Matrix.MSCALE_X];
    }
    /**
     * 判断缩放区间
     * @param detector
     * @return
     */
    @Override
    public boolean onScale(ScaleGestureDetector detector) {
        float scale = getScale();
        float scaleFactor = detector.getScaleFactor();

        if (getDrawable()==null){
            return true;
        }

        if ((scale < mMaxScale && scaleFactor>1.0f)
                ||(scale>mInitScale&&scaleFactor<1.0f)){
            if (scale*scaleFactor<mInitScale){
                scaleFactor = mInitScale/scale;
            }

            if (scale*scaleFactor>mMaxScale){
                scaleFactor = mMaxScale/scale;
            }

            mScaleMatrix.postScale(scaleFactor,scaleFactor,
                    detector.getFocusX(),detector.getFocusY());

            checkBorderAndCenterWhenScale();
            setImageMatrix(mScaleMatrix);
        }

        return true;
    }


    /**
     * 获取改变后图片大小宽高
     * @return
     */
    private RectF getMatrixRectF(){
        Matrix matrix = mScaleMatrix;
        RectF rectF = new RectF();
        Drawable d = getDrawable();
        if (d!=null){
            rectF.set(0,0,d.getIntrinsicWidth(),d.getIntrinsicHeight());
            matrix.mapRect(rectF);
        }
        return rectF;
    }

    /**
     * 缩放时进行边界控制我们的位置,防止白边  setImageMatrix(mScaleMatrix);
     */
    private void checkBorderAndCenterWhenScale() {
        RectF rectF = getMatrixRectF();
        float deltaX = 0;
        float deltaY = 0;

        int width = getWidth();
        int height = getHeight();

        if (rectF.width()>=width){
            if (rectF.left>0){
                deltaX = -rectF.left;
            }

            if (rectF.right<width){
                deltaX = width-rectF.right;
            }
        }

        if (rectF.height()>=height){
            if (rectF.top>0){
                deltaY = -rectF.top;
            }
            if (rectF.bottom<height){
                deltaY = height-rectF.bottom;
            }
        }

        //如果控件宽高小于剧中
        if (rectF.width()<width){
            deltaX = width/2-rectF.right+rectF.width()/2f;
        }
        if (rectF.height()<height){
             deltaY = height/2-rectF.bottom+rectF.height()/2f;
        }

        mScaleMatrix.postTranslate(deltaX,deltaY);
    }

    private void checkBorderAndCenterWhenTranslate() {
        RectF rectF = getMatrixRectF();
        float deltaX = 0;
        float deltaY = 0;

        int width = getWidth();
        int height = getHeight();

        if (rectF.top>0&&isCheckTopAndBottom){
            deltaY = -rectF.top;
        }

        if (rectF.bottom<height&&isCheckTopAndBottom){
            deltaY = height-rectF.bottom;
        }

        if (rectF.left>0&&isCheckLeftAndRight){
            deltaX = -rectF.left;
        }

        if (rectF.right<width&&isCheckLeftAndRight){
            deltaX = width-rectF.right;
        }

        mScaleMatrix.postTranslate(deltaX,deltaY);
    }
    @Override
    public boolean onScaleBegin(ScaleGestureDetector detector) {
        return true;
    }

    @Override
    public void onScaleEnd(ScaleGestureDetector detector) {

    }

    @Override
    public boolean onTouch(View v, MotionEvent event) {
        //避免双击产生移动操作
        if (mGestureDetector.onTouchEvent(event)) {
            return true;
        }

        mScaleGestureDetector.onTouchEvent(event);

        float x = 0;
        float y = 0;

        int pointCount = event.getPointerCount();
        for (int i=0;i<pointCount;i++){
            x+=event.getX(i);
            y+=event.getY(i);
        }

        x/=pointCount;
        y/=pointCount;

        if (mLastPointerCount!=pointCount){
            isCanDrag = false;
            mLastX = x;
            mLastY = y;
        }
        mLastPointerCount = pointCount;
        RectF rectF = getMatrixRectF();
        switch (event.getAction()){
            case MotionEvent.ACTION_DOWN:
                //处理滑动冲突
                if (rectF.width()>getWidth()||rectF.height()>getHeight()){
                    getParent().requestDisallowInterceptTouchEvent(true);
                }
                break;
            case MotionEvent.ACTION_MOVE:
                //避免浮点数操作误差
                if (rectF.width()>getWidth()+0.01||
                        rectF.height()>getHeight()+0.01){
                    if (getParent() instanceof ViewPager) {
                        getParent().requestDisallowInterceptTouchEvent(true);
                    }
                }

                float dx = x-mLastX;
                float dy = y-mLastY;

                if (!isCanDrag){
                    isCanDrag = isMoveAction(dx,dy);
                }

                if (isCanDrag){
                    if (getDrawable()!=null){
                        //宽高小于控件宽高不允许移动
                        isCheckLeftAndRight = isCheckTopAndBottom = true;
                        if (rectF.width()<getWidth()){
                            isCheckLeftAndRight = false;
                            dx = 0;
                        }

                        if (rectF.height()<getHeight()){
                            isCheckTopAndBottom = false;
                            dy = 0;
                        }
                        mScaleMatrix.postTranslate(dx,dy);
                        checkBorderAndCenterWhenTranslate();
                        setImageMatrix(mScaleMatrix);
                    }
                }
                mLastX = x;
                mLastY = y;
                break;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                mLastPointerCount = 0;
                break;
            default:
                break;
        }

        return true;
    }



    /**
     * 判断是否触发move
     * @param dx
     * @param dy
     * @return
     */
    private boolean isMoveAction(float dx, float dy) {
        return Math.sqrt(dx*dx+dy*dy)>mTouchSlop;
    }
}
