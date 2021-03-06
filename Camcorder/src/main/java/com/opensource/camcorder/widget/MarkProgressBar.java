/*
 * Copyright (C) 2014 The Android Open Source Project.
 *
 *        yinglovezhuzhu@gmail.com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.opensource.camcorder.widget;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;

import com.opensource.magiccamcorder.R;

import java.util.Stack;

/**
 * Use:
 * Created by yinglovezhuzhu@gmail.com on 2014-05-30.
 */
public class MarkProgressBar extends View {

    private static final int MIN_SPLIT_WIDTH = 2;
    private static final int INVALID_POSITION = -1;

    private int mMaxProgress = 100;
    private int mProgress = 0;
    private int mSplitWidth = MIN_SPLIT_WIDTH;
    private int mMinMask = 10;

    /** Background color */
    private int mBackgroundColor = Color.TRANSPARENT;
    /** Progress an text color */
    private int mProgressColor = Color.argb(0xff, 0x06, 0xD2, 0x85);
    /** Mark color */
    private int mSplitColor = Color.RED;
    private int mMinMaskColor = Color.argb(0xff, 0x06, 0xD2, 0x85);

    private int mDeleteComfirmColor = Color.RED;
    private int mLastSplitPosition = INVALID_POSITION;


    private RectF mRectF;
    private Paint mPaint;

    private Stack<Integer> mSplits = new Stack<Integer>();

    private long mUiThreadId;

    private boolean mConfirming = false;

    private OnDeleteListener mDeleteListener;

    public MarkProgressBar(Context context) {
        this(context, null);
    }

    public MarkProgressBar(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public MarkProgressBar(Context context, AttributeSet attrs, int defStyleAttr) {
        this(context, attrs, defStyleAttr, 0);
    }

    public MarkProgressBar(Context context, AttributeSet attrs, int defStyleAttr, int styleRes) {
        super(context, attrs, defStyleAttr);
        mUiThreadId = Thread.currentThread().getId();
        initProgressBar();
        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.DrawProgressBar, defStyleAttr, 0);

        setBackgroundColor(a.getColor(R.styleable.DrawProgressBar_backgroundColor, mBackgroundColor));
        setProgressColor(a.getColor(R.styleable.DrawProgressBar_progressColor, mProgressColor));
        setSplitColor(a.getColor(R.styleable.DrawProgressBar_splitColor, mSplitColor));
        setMaxProgress(a.getInt(R.styleable.DrawProgressBar_max, mMaxProgress));
        setProgress(a.getInt(R.styleable.DrawProgressBar_progress, mProgress));
        setSplitWidth(a.getDimensionPixelSize(R.styleable.DrawProgressBar_splitWidth, MIN_SPLIT_WIDTH));
        setMinMask(a.getInt(R.styleable.DrawProgressBar_minMask, 0));
        setMinMaskColor(a.getColor(R.styleable.DrawProgressBar_minMaskColor, mMinMaskColor));
        a.recycle();
    }

    /**
     * Set background color
     */
    public void setBackgroundColor(int color) {
        this.mBackgroundColor = color;
        refreshProgress();
    }

    public int getBackgroundColor(int color) {
        return mBackgroundColor;
    }

    /**
     * Set progress color
     * @param color
     */
    public void setProgressColor(int color) {
        this.mProgressColor = color;
        refreshProgress();
    }

    /**
     * Get progress color
     * @return
     */
    public int getProgressColor() {
        return mProgressColor;
    }

    /**
     * Set split stroke color
     * @param color
     */
    public void setSplitColor(int color) {
        this.mSplitColor = color;
        refreshProgress();
    }

    /**
     * Get split stroke color
     * @return
     */
    public int getSplitColor() {
        return mSplitColor;
    }

    /**
     * Set max progress
     * @param maxProgress
     */
    public void setMaxProgress(int maxProgress) {
        this.mMaxProgress = maxProgress;
    }

    /**
     * Get max progress
     * @return
     */
    public int getMaxProgress() {
        return mMaxProgress;
    }

    /**
     * Set progress
     * @param progress
     */
    public synchronized void setProgress(int progress) {
        if(progress < 0) {
            progress = 0;
        }
        if(progress > mMaxProgress) {
            progress = mMaxProgress;
        }
        if(mProgress != progress) {
            this.mProgress = progress;
            if(progress == 0) {
                clearSplits();
            } else {
                refreshProgress();
            }
        }
    }

    /**
     * Get current progress.
     * @return
     */
    public int getProgress() {
        return mProgress;
    }


    public void setSplitWidth(int width) {
        this.mSplitWidth = width;
        refreshProgress();
    }

    /**
     * Get split stoke width
     * @return
     */
    public int getSplitWidth() {
        return mSplitWidth;
    }

    /**
     * Set min mask position<br/>
     * <p/>The min mask position is a progress value
     * @param progress
     */
    public void setMinMask(int progress) {
        this.mMinMask = progress;
        refreshProgress();
    }

    /**
     * Get min mask position
     */
    public int getmMinMask() {
        return this.mMinMask;
    }

    /**
     * Set the color of min mask
     * @param color
     */
    public void setMinMaskColor(int color) {
        this.mMinMaskColor = color;
        refreshProgress();
    }

    /**
     * Get the color of min mask
     * @return
     */
    public int getMinMaskColor() {
        return mMinMaskColor;
    }

    /**
     * Add a split line<br/>
     * <p/> The split position is a progress value.
     * @param progress
     */
    public void pushSplit(int progress) {
        if(progress == 0 || mSplits.contains(progress)) {
            return;
        }
        mSplits.push(progress);
        refreshProgress();
    }

    /**
     * Delete the last split position.
     * @return the las split position.
     */
    public int popSplit() {
        if(mSplits.empty()) {
            return 0;
        }
        Integer split = mSplits.pop();
        refreshProgress();
        return split == null ? 0 : split;
    }

    /**
     * Peek the last split position
     * @return the last split position
     */
    public int peekSplit() {
        if(mSplits.empty()) {
            return 0;
        }
        Integer split = mSplits.peek();
        return split == null ? 0 : split;
    }

    /**
     * Empty split positions
     */
    public void clearSplits() {
        mSplits.clear();
        refreshProgress();
    }

    /**
     * Exit confirm state
     */
    public void clearConfirm() {
        if(mConfirming) {
            mLastSplitPosition = INVALID_POSITION;
            mConfirming = false;
        }
    }

    /**
     * Delete back to last split
     * @param isConfirm
     */
    public void deleteBack(boolean isConfirm) {
        if(isConfirm) {
            if(mConfirming) {
                if(mProgress < mMaxProgress) {
                    popSplit();
                }
                if(mDeleteListener != null) {
                    mDeleteListener.onDelete(mLastSplitPosition, mProgress);
                }
                setProgress(mLastSplitPosition);
                mLastSplitPosition = INVALID_POSITION;
                mConfirming = false;
            } else {
                int latest = INVALID_POSITION;
                if(mProgress < mMaxProgress) {
                    latest = popSplit();
                }
                mLastSplitPosition = peekSplit();
                if(latest != INVALID_POSITION) {
                    mSplits.push(latest);
                }
                if(mDeleteListener != null) {
                    mDeleteListener.onConfirm(mLastSplitPosition, mProgress);
                }
                refreshProgress();
                mConfirming = true;
            }
        } else {
            if(mProgress < mMaxProgress) {
                popSplit();
            }
            int split = peekSplit();
            if(mDeleteListener != null) {
                mDeleteListener.onDelete(split, mProgress);
            }
            setProgress(split);
        }
    }

    /**
     * Delete back to las split
     * @param isConfirm
     * @param l
     */
    public void deleteBack(boolean isConfirm, OnDeleteListener l) {
        setOnDeleteListener(l);
        deleteBack(isConfirm);
    }

    /**
     * Set the listener when delete back.
     * @param l
     */
    public void setOnDeleteListener(OnDeleteListener l) {
        this.mDeleteListener = l;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        canvas.drawColor(mBackgroundColor); //Draw background color.

        drawMinMask(canvas);

        drawProgress(canvas);

        drawSplits(canvas);

        super.onDraw(canvas);
    }

    private void initProgressBar() {
        mPaint = new Paint();
        mPaint.setAntiAlias(true);

        mRectF = new RectF();
    }

    private void drawProgress(Canvas canvas) {
        int width = getWidth();
        mRectF.top = getTop();
        mRectF.bottom = getBottom();
        if(mLastSplitPosition == INVALID_POSITION) {
            mPaint.setColor(mProgressColor);
            mRectF.left = getLeft();
            mRectF.right = mRectF.left + ((float)mProgress * width) / mMaxProgress;
            canvas.drawRect(mRectF, mPaint);
        } else {
            if(mLastSplitPosition > 0) {
                mPaint.setColor(mProgressColor);
                mRectF.left = getLeft();
                mRectF.right = mRectF.left + ((float)mLastSplitPosition * width) / mMaxProgress;
                canvas.drawRect(mRectF, mPaint);
            }
            mPaint.setColor(mDeleteComfirmColor);
            mRectF.left = ((float)mLastSplitPosition * width) / mMaxProgress;
            mRectF.right = mRectF.left + ((float)(mProgress - mLastSplitPosition) * width) / mMaxProgress;
            canvas.drawRect(mRectF, mPaint);
        }
    }

    private void drawMinMask(Canvas canvas) {
        mPaint.setColor(mMinMaskColor);
        mRectF.top = getTop();
        mRectF.bottom = getBottom();
        if(mMinMask < mSplitWidth) {
            mRectF.left = 0;
            mRectF.right = mSplitWidth;
        } else {
            mRectF.right = ((float)mMinMask * getWidth()) / mMaxProgress;
            mRectF.left = mRectF.right - mSplitWidth;
        }
        canvas.drawRect(mRectF, mPaint);
    }

    private void drawSplits(Canvas canvas) {
        int width = getWidth();
        mPaint.setColor(mSplitColor);
        for(Integer split : mSplits) {
            mRectF.top = getTop();
            mRectF.bottom = getBottom();
            if(split < mSplitWidth) {
                mRectF.left = 0;
                mRectF.right = mSplitWidth;
            } else {
                mRectF.right = ((float)split * width) / mMaxProgress;
                mRectF.left = mRectF.right - mSplitWidth;
            }
            canvas.drawRect(mRectF, mPaint);
        }
    }

    private synchronized void refreshProgress() {
        if (mUiThreadId == Thread.currentThread().getId()) {
            this.postInvalidate();
        }
    }


    /**
     * The listener to listen delete back<br/>
     * <p/>when sue {@link #deleteBack(boolean)} with value true, it would <br/>
     * callback {@link com.opensource.widget.MarkProgressBar.OnDeleteListener#onConfirm(int, int)}<br/>
     * and {@link com.opensource.widget.MarkProgressBar.OnDeleteListener#onDelete(int, int)}<br/>
     * But only {@link com.opensource.widget.MarkProgressBar.OnDeleteListener#onDelete(int, int)} callback<br/>
     * when delete with not confirm mode.
     */
    public static interface OnDeleteListener {

        /**
         * In confirm step
         * @param lastProgress
         * @param progress
         */
        public void onConfirm(int lastProgress, int progress);

        /**
         * Delete back
         * @param lastProgress
         * @param progress
         */
        public void onDelete(int lastProgress, int progress);
    }
}