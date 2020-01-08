package com.oplw.common.customview.rv;

import android.graphics.Rect;
import android.util.SparseArray;
import android.util.SparseBooleanArray;
import android.view.View;

import androidx.recyclerview.widget.RecyclerView;

/**
 * @author opLW
 * @date 2019/5/7
 */
public class CardRollingLayoutManager extends RecyclerView.LayoutManager {
    private int totalWidth = 0;
    /**
     * 记录当前视图最左边位置的累加值
     */
    private int currentLeft = 0;
    private int halfOfViewWidth = 0;

    private boolean isRollingToBoundary = false;
    private SparseArray<Rect> cardPositions = new SparseArray<>();
    private SparseBooleanArray mAttachedItems = new SparseBooleanArray();

    public void resetCurrentLeft() {
        currentLeft = 0;
    }

    @Override
    public RecyclerView.LayoutParams generateDefaultLayoutParams() {
        return new RecyclerView.LayoutParams(
                RecyclerView.LayoutParams.WRAP_CONTENT,
                RecyclerView.LayoutParams.WRAP_CONTENT
        );
    }

    @Override
    public void onLayoutChildren(RecyclerView.Recycler recycler, RecyclerView.State state) {
        if (0 == getItemCount() || state.isPreLayout()) {
            return;
        }

        View view = recycler.getViewForPosition(0);
        measureChildWithMargins(view, 0 ,0);
        int vWidth = getDecoratedMeasuredWidth(view);
        int vHeight = getDecoratedMeasuredHeight(view);
        halfOfViewWidth = vWidth / 2;
        int topMargin = (getHeight() - vHeight) / 2;

        //为开始和结尾留下空白，使得第一张图片刚好位于中心点
        int additionalWidth = getWidth() / 2 - halfOfViewWidth;
        totalWidth = additionalWidth;
        detachAndScrapAttachedViews(recycler);
        for (int i = 0; i < getItemCount(); i ++) {
            Rect current = cardPositions.get(i);
            if (null == current) {
                current = new Rect();
            }
            current.set(totalWidth, topMargin, totalWidth + vWidth, vHeight + topMargin);
            cardPositions.put(i, current);
            mAttachedItems.put(i, false);

            if (i != getItemCount() - 1) {
                //定义每次totalWidth增加的位移为图片大小的二分之一，达到覆盖的目的
                totalWidth += halfOfViewWidth;
            } else {
                //最后一张时不做特殊处理
                totalWidth += vWidth;
            }
        }
        totalWidth = Math.max(getHorizontalSpace(), totalWidth);
        totalWidth += additionalWidth;
        fill(recycler, 0);
    }

    @Override
    public boolean canScrollHorizontally() {
        return true;
    }

    @Override
    public int scrollHorizontallyBy(int dx, RecyclerView.Recycler recycler, RecyclerView.State state) {
        if (0 == getItemCount() || state != null && state.isPreLayout()) {
            return 0;
        }

        if (currentLeft + dx < 0) {
            dx = -currentLeft;
        } else if (currentLeft + getHorizontalSpace() + dx > totalWidth) {
            dx = totalWidth - getHorizontalSpace() - currentLeft;
        }

        currentLeft += dx;
        checkUpdateMidItem();
        isRollingToBoundary = currentLeft == 0 || currentLeft == totalWidth;
        fill(recycler, dx);
        offsetChildrenHorizontal(-dx);

        return dx;
    }

    private void fill(RecyclerView.Recycler recycler, int dx) {
        Rect visibleRegion = getVisibleRegion(dx);
        for (int i = 0; i < getChildCount(); i ++) {
            View child = getChildAt(i);
            Rect childRegion = getChildRegion(child);
            int pos = getPosition(child);

            if (!Rect.intersects(childRegion, visibleRegion)) {
                removeAndRecycleView(child, recycler);
                mAttachedItems.put(pos, false);
            } else {
                //没有被移除的view需要变化位置
                Rect childPosition = cardPositions.get(pos);
                layoutDecoratedWithMargins(child, childPosition.left - currentLeft + dx, childPosition.top,
                        childPosition.right - currentLeft + dx, childPosition.bottom);

                int pivotX = (childPosition.right - currentLeft + dx) - halfOfViewWidth;
                makeAnimation(child, pivotX);
            }
        }

        if (dx >= 0) {
            View firstChild = getChildAt(Math.max(getChildCount() - 1, 0));
            int firstPos;
            //因为可能还没有child，所以加上这些判断
            if (null == firstChild) {
                firstPos = 0;
            } else {
                firstPos = getPosition(firstChild);
            }
            for (int i = firstPos; i < getItemCount(); i ++) {
                insertView(recycler, dx, i, false);
            }
        } else {
            View lastChild = getChildAt(0);
            int lastPos = getPosition(lastChild);
            for (int i = lastPos; i >= 0; i --) {
                insertView(recycler, dx, i, true);
            }
        }
    }

    private void insertView(RecyclerView.Recycler recycler, int dx, int pos, boolean isFirst) {
        Rect visibleRegion = getLiveRegion();
        Rect viewPosition = cardPositions.get(pos);
        if (Rect.intersects(visibleRegion, viewPosition) && !mAttachedItems.get(pos)) {
            mAttachedItems.put(pos, true);

            View child = recycler.getViewForPosition(pos);
            measureChildWithMargins(child, 0, 0);
            if (isFirst) {
                addView(child, 0);
            } else {
                addView(child);
            }
            layoutDecoratedWithMargins(child, viewPosition.left - currentLeft + dx, viewPosition.top,
                    viewPosition.right - currentLeft + dx, viewPosition.bottom);

            int pivotX = (viewPosition.right - currentLeft + dx) - halfOfViewWidth;
            makeAnimation(child, pivotX);
        }
    }

    /**
     * 根据当前card与界面中心点的距离计算translationY，scale，rotateY的值,并设置给对应的view
     * @param child  待设置的view
     * @param pivotXInCard card中心点的值
     */
    private void makeAnimation(View child, int pivotXInCard) {
        int centerX = getWidth() / 2;

        float translationY;
        float scale;
        float minScale = 0.6f;
        float rotateY;
        float maxRotateY = 70.0f;
        float ratio;
        //位于界面中心点的左边
        if (pivotXInCard <= centerX) {
            ratio = (float) pivotXInCard / centerX;
            rotateY = (1 - ratio) * maxRotateY;
            child.setPivotY(0.0f);
        } else {
            pivotXInCard -= centerX;
            ratio = 1 - (float) pivotXInCard / centerX;
            rotateY = - (1 - ratio) * maxRotateY;
            child.setPivotY(1.0f);
        }
        child.setRotationY(rotateY);

        translationY = (ratio - 1) * getHeight() / 12;
        scale = minScale + ratio * (1 - minScale);
        child.setPivotY(0.0f);
        child.setTranslationY(translationY);
        child.setScaleX(scale);
        child.setScaleY(scale);
    }

    private int preNum = -1;

    private void checkUpdateMidItem() {
        int curNum = Math.round((float) currentLeft / halfOfViewWidth);
        if (curNum != preNum) {
            preNum = curNum;
            updateMidItem(curNum);
        }
    }

    private void updateMidItem(int position) {
        if (null == midItemChangeListener) {
            return;
        }
        midItemChangeListener.onMidItemChange(position);
    }

    public interface OnMidItemChangeListener {
        /**
         * 在中心child变化时，通知监听者
         * @param position 位于中心点的child在所有child中的位置
         */
        void onMidItemChange(int position);
    }

    private OnMidItemChangeListener midItemChangeListener;

    public void setMidItemChangeListener(OnMidItemChangeListener listener) {
        midItemChangeListener = listener;
    }

    public double calculateDistance(int velocityX, double distance) {
        int extra = currentLeft % halfOfViewWidth;
        double realDistance;
        if (velocityX > 0){
            if (distance < halfOfViewWidth) {
                realDistance = halfOfViewWidth - extra;
            }else {
                realDistance = distance - distance % halfOfViewWidth - extra;
            }
        }else {
            if (distance < halfOfViewWidth) {
                realDistance = extra;
            }else {
                realDistance = distance - distance % halfOfViewWidth + extra;
            }
        }
        return realDistance;
    }

    /**
     * 判断当前是否滚动到了左边界或者右边界，以便RecyclerView通知父容器拦截触摸事件
     * @return true代表到达边界，false代表还没有到达边界
     */
    public boolean isRollingToBoundary() {
        return isRollingToBoundary;
    }

    /**
     * 获取当前可见的view中第一个在所有的view中的序号。
     * @return 代表在所有view中的序号
     */
    public int getFirstVisiblePosition() {
        if (getChildCount() <= 0 ) {
            return 0;
        }

        View first = getChildAt(0);
        return getPosition(first);
    }

    /**
     * 获取当前可见的view中最中间那个在所有的view中的序号。
     * @return 代表在所有view中的序号
     */
    public int getCenterVisiblePosition() {
        return Math.round((float) currentLeft / halfOfViewWidth);
    }

    private Rect getChildRegion(View child) {
        return new Rect(child.getLeft(), child.getTop(), child.getRight(), child.getBottom());
    }

    /**
     * 返回当前可见的界面位置，因为先假设移动，所以要加上dx
     * @param dx 移动值
     * @return 返回当前可见的界面位置
     */
    private Rect getVisibleRegion(int dx) {
        return new Rect(getPaddingStart() + dx, getPaddingTop(),
                getWidth() + dx - getPaddingEnd(), getHeight() - getPaddingBottom());
    }

    /**
     * 当前可以放置图片的区域，因为利用的是一开始测量好的图片的position来判断是否应该添加该图片。
     * 是一种累加值，所以这里不同于{@link #getVisibleRegion}，要加上{@link #currentLeft}
     * @return 返回基于累加值的存活区域
     */
    private Rect getLiveRegion() {
        return new Rect(getPaddingStart() + currentLeft, getPaddingTop(),
                currentLeft + getWidth() - getPaddingEnd(), getHeight() - getPaddingBottom());
    }

    private int getHorizontalSpace() {
        return getWidth() - getPaddingStart() - getPaddingEnd();
    }
}
