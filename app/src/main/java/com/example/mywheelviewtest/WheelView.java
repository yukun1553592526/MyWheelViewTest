package com.example.mywheelviewtest;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.os.Handler;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.view.GestureDetector;
import android.view.Gravity;
import android.view.View;

import androidx.annotation.Nullable;

import com.example.mywheelviewtest.adapter.WheelAdapter;
import com.example.mywheelviewtest.listener.LoopViewGestureListener;
import com.example.mywheelviewtest.listener.OnItemSelectedListener;
import com.example.mywheelviewtest.timer.MessageHandler;
import com.example.mywheelviewtest.interfaces.IPickerViewData;
import com.example.mywheelviewtest.timer.SmoothScrollTimerTask;

import org.jetbrains.annotations.NotNull;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public class WheelView extends View {

    public enum MotionAction {
        CLICK, DAGGLE, FLIING;

    }

    public enum DividerType {
        FILL, WRAP, CIRCLE;

    }
    public static final int DEFAULT_OUTER_TEXT_COLOR = 0xFFA8A8A8;
    public static final int DEFAULT_CENTER_TEXT_COLOR = 0xFF2A2A2A;

    public static final int DEFAULT_DIVIDER_COLOR = 0xFFD5D5D5;
    public static final int DEFAULT_DIVIDER_WIDTH = 2;
    private static final String[] TIME_NUM = {"00", "01", "02", "03", "04", "05", "06", "07", "08", "09"};
    // 分割线类型

    private DividerType dividerType;
    private Context mContext;
    private Handler handler;
    private GestureDetector mGestureDetector;
    private OnItemSelectedListener onItemSelectedListener;
    private boolean isOptions = false;
    private boolean isCenterLabel = true;

    // Timer mTimer
    private ScheduledExecutorService mExecutor = Executors.newSingleThreadScheduledExecutor();

    private ScheduledFuture<?> mFuture;
    private Paint outerTextPaint;
    private Paint centerTextPaint;

    private Paint indicatorPaint;
    private WheelAdapter adapter;
    private String unit;  // 附加单位

    private int textSize; // 选项的文字大小
    private int maxTextWidth;
    private int maxTextHeight;
    private int textOffsetX;
    private float itemHeight;
    private Typeface typeface = Typeface.MONOSPACE;
    private int outTextColor;

    private int centerTextColor;
    private int dividerColor;
    private int dividerWidth;
    // 条目间距倍数
    private float lineSpacingMultiplier = 1.6f;

    private boolean isLoop;
    // 第一条线Y坐标值
    private float firstLineY;

    // 第二条线Y坐标值
    private float secondLineY;
    // 中间label绘制的Y坐标
    private float centerY;
    // 当前滚动总高度y值
    private float totalScrollY;

    // 初始化默认选中项
    private int initPosition;

    // 选中的Item是第几个
    private int selectedItem;

    private int preCurrentIndex;
    // 绘制几个条目，实际上第一项和最后一项Y轴压缩成0%，所以可见的数目实际为9
    private int itemsVisible = 11;

    // WheelView 控件高度
    private int measureHeight;

    // WheelView 控件宽度
    private int measureWidth;
    // 半径
    private int radius;

    private int mOffset = 0;
    private float previousY = 0;

    private long startTime = 0;
    // 修改这个值可以改变滑行速度
    private static final int FLING_VELOCITY = 5;

    private int widthMeasureSpec;
    private int mGravity = Gravity.CENTER;
    // 中间选中文字开始绘制位置

    private int drawCenterContentStart = 0;
    // 非中间文字开始绘制位置
    private int drawOutContentStart = 0;
    // 非中间文字则用此控制高度
    private static final float SCAle_CONTENT = 0.8f;
    // 偏移量
    private float CENTER_CONTENT_OFFSET;
    // 透明度渐变效果
    private boolean isAlphaGradient = false;

    public WheelView(Context context) {
        this(context, null);
    }
    public WheelView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        this.mContext = context;

        textSize = getResources().getDimensionPixelSize(R.dimen.pickerview_textsize);

        DisplayMetrics dm =  getResources().getDisplayMetrics();
        // 屏幕密度比（0.75/1.0/1.5/2.0/3.0）
        float density = dm.density;

        if (density < 1) {
            CENTER_CONTENT_OFFSET = 2.4f;
        } else if (1 <= density && density < 2) {
            CENTER_CONTENT_OFFSET = 4.0f;
        } else if (2 <= density && density < 3) {
            CENTER_CONTENT_OFFSET = 6.0f;
        } else if (3 <= density) {
            CENTER_CONTENT_OFFSET = density * 2.5f;
        }

        if (attrs != null) {
            TypedArray ta = context.obtainStyledAttributes(attrs, R.styleable.prickerview);
            mGravity = ta.getInt(R.styleable.prickerview_wheelview_gravity, Gravity.CENTER);
            outTextColor = ta.getColor(R.styleable.prickerview_wheelview_outerTextColor, DEFAULT_OUTER_TEXT_COLOR);
            centerTextColor = ta.getColor(R.styleable.prickerview_wheelview_centerTextColor, DEFAULT_OUTER_TEXT_COLOR);
            dividerColor = ta.getColor(R.styleable.prickerview_wheelview_dividerColor, DEFAULT_DIVIDER_COLOR);
            dividerWidth = ta.getDimensionPixelSize(R.styleable.prickerview_wheelview_dividerWidth, DEFAULT_DIVIDER_WIDTH);
            textSize = ta.getDimensionPixelSize(R.styleable.prickerview_wheelview_textSize, textSize);
            lineSpacingMultiplier = ta.getFloat(R.styleable.prickerview_wheelview_lineSpacingMultiplier, lineSpacingMultiplier);
            ta.recycle();
        }
        judgeLineSpace();
        initLoopView();

    }

    /**
     * 判断间距是否在1.0~4.0之间
     */
    private void judgeLineSpace() {
        if (lineSpacingMultiplier < 1) {
            lineSpacingMultiplier = 1.0f;
        } else if (lineSpacingMultiplier > 4) {
            lineSpacingMultiplier = 4.0f;
        }
    }

    private void initLoopView() {
        handler = new MessageHandler(this);
        mGestureDetector = new GestureDetector(mContext, new LoopViewGestureListener(this));
        mGestureDetector.setIsLongpressEnabled(true);
        isLoop = true;

        totalScrollY = 0;
        initPosition = -1;
        initPaints();
    }

    /**
     *
     * 初始化各个Paint
     */
    private void initPaints() {
        outerTextPaint = new Paint();
        outerTextPaint.setAntiAlias(true);
        outerTextPaint.setColor(outTextColor);
        outerTextPaint.setTypeface(typeface);
        outerTextPaint.setTextSize(textSize);

        centerTextPaint = new Paint();
        centerTextPaint.setAntiAlias(true);
        centerTextPaint.setColor(centerTextColor);
        centerTextPaint.setTextScaleX(1.1F);
        centerTextPaint.setTypeface(typeface);
        centerTextPaint.setTextSize(textSize);

        indicatorPaint = new Paint();
        indicatorPaint.setColor(dividerColor);
        indicatorPaint.setAntiAlias(true);

        setLayerType(LAYER_TYPE_SOFTWARE, null);
    }

    /**
     * 重新测量
     */
    private void reMeasure() {
        if (adapter == null) {
            return;
        }

        measureTextParam();

        // 半圆的周长 = item高乘以item数目-1
        int halfCircumference = (int) (itemHeight * (itemsVisible - 1));
        // 整个圆的周长除以PI得到直径，这个直径用作控件的总高度
        measureHeight = (int) ((halfCircumference * 2) / Math.PI);
        // 求出半径
        radius = (int) (halfCircumference / Math.PI);
        // 控件宽度，这里支持weight
        measureWidth = MeasureSpec.getSize(widthMeasureSpec);
        // 计算两条横线 和 选中项画线的基线Y位置
        firstLineY = (measureHeight - itemHeight) / 2.0f;
        secondLineY = (measureHeight + itemHeight) / 2.0f;
        centerY = secondLineY - (itemHeight - maxTextHeight) / 2.0f - CENTER_CONTENT_OFFSET;

        // init the position of item displaying
        if (initPosition < 0) {
            if (isLoop) {
                initPosition = (adapter.getItemsCount() + 1) / 2;
            } else {
                initPosition = 0;
            }
        }
        preCurrentIndex = initPosition;
    }

    /**
     * Measures width and height of item's text with the maximum length
     */
    private void measureTextParam() {
        Rect rect = new Rect();
        for (int i = 0; i < adapter.getItemsCount(); i++) {
            String s1 = getContentText(adapter.getItem(i));
            centerTextPaint.getTextBounds(s1, 0 , s1.length(), rect);
            int textWidth = rect.width();
            if (textWidth > maxTextWidth) {
                maxTextWidth = textWidth;
            }
        }
        centerTextPaint.getTextBounds("\u661F\u671F", 0 ,2, rect);
        maxTextHeight = rect.height() + 2;
        itemHeight = lineSpacingMultiplier * maxTextHeight;
    }

    /**
     * Returns the displaying data source on purpose
     * @param item
     * @return
     */
    private String getContentText(Object item) {
        if (item == null) {
            return "";
        } else if (item instanceof IPickerViewData) {
            return ((IPickerViewData) item).getPickerViewText();
        } else if (item instanceof Integer) {
            //  if item belongs to Integer, it must stay 2 decimals
            return getFixNum((int) item);
        }
        return item.toString();
    }

    private String getFixNum(int item) {
        return item >= 0 && item < 10 ? TIME_NUM[item] : String.valueOf(item);
    }

    /**
     * Achieves the effect smoothly scrolling
     * @param actionMethod
     */
    public void smoothScroll(@NotNull MotionAction actionMethod) {
        cancelFuture();
        if (actionMethod == MotionAction.FLIING || actionMethod == MotionAction.DAGGLE) {
            mOffset = (int) ((totalScrollY % itemHeight + itemHeight) % itemHeight);
            if ((float) mOffset > itemHeight / 2.0f) {
                mOffset = (int) (itemHeight - (float) mOffset);
            } else {
                mOffset = -mOffset;
            }
        }

        /**
         * when it stops, items have translated. So not all items are able to
         * stop correctly in the middle position.  This is why it's necessary
         * to move the texts back to the middle position.
         */
        mFuture = mExecutor.scheduleWithFixedDelay(new SmoothScrollTimerTask(this, mOffset), 0 , 10, TimeUnit.MILLISECONDS);
    }

    public void cancelFuture() {
        if (mFuture != null && !mFuture.isCancelled()) {
            mFuture.cancel(true);
            mFuture = null;
        }
    }

    public void onItemSelected() {

    }

    public void scrollBy(float velocityY) {

    }

    public float getTotalScrollY() {
        return totalScrollY;
    }

    public void setTotalScrollY(float totalScrollY) {
        this.totalScrollY = totalScrollY;
    }

    public float getItemHeight() {
        return itemHeight;
    }

    public boolean isLoop() {
        return isLoop;
    }

    public int getInitPosition() {
        return initPosition;
    }

    public int getItemsCount() {
        return adapter == null ? 0 : adapter.getItemsCount();
    }

    public void setLabel(String unit) {
        this.unit = unit;
    }

    public void setCenterLabel(boolean isCenterLabel) {
        this.isCenterLabel = isCenterLabel;
    }

    public void setGravity(int gravity) {
        this.mGravity = gravity;
    }

    public int getTextWidth(Paint paint, String string) {
        int iRet = 0;
        if (string != null && string.length() > 0) {
            int len = string.length();
            float[] widths = new float[len];
            paint.getTextWidths(string, widths);
            for (int j = 0; j < len; j++) {
                iRet += (int) Math.ceil(widths[j]);
            }
        }
        return iRet;
    }

    public void setIsOptions(boolean options) {
        this.isOptions = options;
    }

    public void setOutTextColor(int outTextColor) {
        this.outTextColor = outTextColor;
        outerTextPaint.setColor(outTextColor);
    }

    public void setCenterTextColor(int centerTextColor) {
        this.centerTextColor = centerTextColor;
        centerTextPaint.setColor(centerTextColor);
    }

    public void setTextOffsetX(int textOffsetX) {
        this.textOffsetX = textOffsetX;
        if (textOffsetX != 0) {
            centerTextPaint.setTextScaleX(1.0f);
        }
    }

    public void setDividerWidth(int dividerWidth) {
        this.dividerWidth = dividerWidth;
        indicatorPaint.setStrokeWidth(dividerWidth);
    }

    public void setDividerColor(int dividerColor) {
        this.dividerColor = dividerColor;
        indicatorPaint.setColor(dividerColor);
    }

    public void setDividerType(DividerType dividerType) {
        this.dividerType = dividerType;
    }

    public void setLineSpaacingMultiplier(float lineSpacingMultiplier) {
        if (lineSpacingMultiplier != 0) {
            this.lineSpacingMultiplier = lineSpacingMultiplier;
            judgeLineSpace();
        }
    }
}
