package com.oplw.common.customview.rv;

import android.content.Context;
import android.hardware.SensorManager;
import android.util.AttributeSet;
import android.view.ViewConfiguration;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;

/**
 * @author opLW
 * @date 2019/5/8
 */
public class CardRollingRecyclerView extends RecyclerView {

    private Boolean enableSpecialFunction = true;

    public CardRollingRecyclerView(@NonNull Context context) {
        super(context);
        setChildrenDrawingOrderEnabled(true);
    }

    public CardRollingRecyclerView(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        setChildrenDrawingOrderEnabled(true);
    }

    public CardRollingRecyclerView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        setChildrenDrawingOrderEnabled(true);
    }

    /**
     * 是否激活recyclerView的特殊用途
     * 默认激活配合CardRollingLayoutManager
     */
    public void enableSpecialFunction(Boolean enableSpecialFunction) {
        this.enableSpecialFunction = enableSpecialFunction;
    }

    @Override
    protected int getChildDrawingOrder(int childCount, int i) {
        if (!enableSpecialFunction) {
            return super.getChildDrawingOrder(childCount, i);
        }

        int center = getCardRollingLayoutManager().getCenterVisiblePosition();
        int first = getCardRollingLayoutManager().getFirstVisiblePosition();
        center -= first;

        if (i < center) {
            return i;
        } else if (i == center) {
            return childCount - 1;
        } else {
            return childCount - 1 - (i - center);
        }
    }

    @Override
    public boolean fling(int velocityX, int velocityY) {
        if (!enableSpecialFunction) {
            return super.fling(velocityX, velocityY);
        }

        /*
        对fling距离进行矫正使得子View刚好在UI最中间
         */
        int mVelocityX = velocityX;
        CardRollingLayoutManager manger = getCardRollingLayoutManager();
        double distance = getSplineFlingDistance(mVelocityX);
        double newDistance = manger.calculateDistance(velocityX, distance);
        int fixVelocityX = getVelocity(newDistance);
        mVelocityX = velocityX > 0 ? fixVelocityX : - fixVelocityX;
        return super.fling(mVelocityX, velocityY);
    }

    private CardRollingLayoutManager getCardRollingLayoutManager() {
        return (CardRollingLayoutManager) getLayoutManager();
    }

    /**
     * 根据松手后的滑动速度计算出fling的距离
     * @param velocity 松手时的滑动速度
     * @return 返回此滑动速度对应的fling的距离
     */
    private double getSplineFlingDistance(int velocity) {
        final double l = getSplineDeceleration(velocity);
        final double decelMinusOne = DECELERATION_RATE - 1.0;
        return mFlingFriction * getPhysicalCoeff() * Math.exp(DECELERATION_RATE / decelMinusOne * l);
    }

    /**
     * 根据矫正后的距离计算出速度
     * @param distance 经过矫正之后的fling距离
     * @return  矫正后的距离对应的速度
     */
    private int getVelocity(double distance) {
        final double decelMinusOne = DECELERATION_RATE - 1.0;
        double aecel = Math.log(distance / (mFlingFriction * mPhysicalCoeff)) * decelMinusOne / DECELERATION_RATE;
        return Math.abs((int) (Math.exp(aecel) * (mFlingFriction * mPhysicalCoeff) / INFLEXION));
    }

    private static final float INFLEXION = 0.35f;
    private float mFlingFriction = ViewConfiguration.getScrollFriction();
    private static float DECELERATION_RATE = (float) (Math.log(0.78) / Math.log(0.9));
    private float mPhysicalCoeff = 0;

    private double getSplineDeceleration(int velocity) {
        final float ppi = this.getResources().getDisplayMetrics().density * 160.0f;
        float mPhysicalCoeff = SensorManager.GRAVITY_EARTH
                * 39.37f // inch/meter
                * ppi
                * 0.84f; // look and feel tuning


        return Math.log(INFLEXION * Math.abs(velocity) / (mFlingFriction * mPhysicalCoeff));
    }

    private float getPhysicalCoeff() {
        if (mPhysicalCoeff == 0) {
            final float ppi = this.getResources().getDisplayMetrics().density * 160.0f;
            mPhysicalCoeff = SensorManager.GRAVITY_EARTH
                    * 39.37f // inch/meter
                    * ppi
                    * 0.84f; // look and feel tuning
        }
        return mPhysicalCoeff;
    }
}
