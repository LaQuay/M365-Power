package laquay.M365.dashboard.component;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.support.annotation.Nullable;
import android.support.v4.content.ContextCompat;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.RelativeLayout;
import android.widget.TextView;

import laquay.M365.dashboard.R;

public class DigitalTextView extends RelativeLayout {
    private static final String TAG = DigitalTextView.class.getSimpleName();
    private String unit = "Km/h";
    private float speedTextSize = dpTOpx(40f);
    private float unitTextSize = dpTOpx(10f);
    private String value = "";
    private int valueTextColor;
    private int unitTextColor;
    private int backgoundColor = Color.BLACK;
    private TextView mSpeedBgTextView;
    private TextView mSpeedTextView;
    private TextView mSpeedUnitTextView;
    private RelativeLayout mainLayout;
    private boolean showUnit = true;

    public DigitalTextView(Context context) {
        super(context);
        init(context);
    }

    public DigitalTextView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init(context);
        initAttributeSet(context, attrs);
    }

    public DigitalTextView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
        initAttributeSet(context, attrs);
    }

    private void init(Context context) {
        valueTextColor = ContextCompat.getColor(context, R.color.md_green_400);
        unitTextColor = ContextCompat.getColor(context, R.color.md_green_400);
        final View rootView = LayoutInflater.from(context).inflate(R.layout.digital_text_view, this, true);
        mainLayout = rootView.findViewById(R.id.digit_speed_main);
        mSpeedTextView = rootView.findViewById(R.id.digit_speed);
        mSpeedBgTextView = rootView.findViewById(R.id.digit_speed_bg);
        mSpeedUnitTextView = rootView.findViewById(R.id.digit_speed_unit);
        Typeface tf = Typeface.createFromAsset(getResources().getAssets(), "fonts/digital-7_mono.ttf");
        mSpeedTextView.setTypeface(tf);
        mSpeedBgTextView.setTypeface(tf);
        mSpeedUnitTextView.setTypeface(tf);
    }

    private void initAttributeSet(Context context, AttributeSet attrs) {
        TypedArray a = context.getTheme().obtainStyledAttributes(attrs, R.styleable.DigitalTextView, 0, 0);
        speedTextSize = a.getDimension(R.styleable.DigitalTextView_valueTextSize, speedTextSize);
        unitTextSize = a.getDimension(R.styleable.DigitalTextView_unitTextSize, unitTextSize);
        valueTextColor = a.getColor(R.styleable.DigitalTextView_valueTextColor, valueTextColor);
        unitTextColor = a.getColor(R.styleable.DigitalTextView_unitTextColor, unitTextColor);
        showUnit = a.getBoolean(R.styleable.DigitalTextView_showUnit, showUnit);
        if (!showUnit) {
            mSpeedUnitTextView.setVisibility(GONE);
        }
        value = a.getString(R.styleable.DigitalTextView_value);
        backgoundColor = a.getColor(R.styleable.DigitalTextView_backgroundColor, backgoundColor);
        String unit = a.getString(R.styleable.DigitalTextView_unit);
        this.unit = (unit != null) ? unit : this.unit;
        if (a.getBoolean(R.styleable.DigitalTextView_disableBackgroundImage, false)) {
            mainLayout.setBackgroundResource(0);
            mainLayout.setBackgroundColor(backgoundColor);
        } else {
            Drawable drawable = a.getDrawable(R.styleable.DigitalTextView_backgroundDrawable);
            if (drawable != null) {
                if (android.os.Build.VERSION.SDK_INT >= 16) {
                    mainLayout.setBackground(drawable);
                } else {
                    mainLayout.setBackgroundDrawable(drawable);
                }

            }
        }
        a.recycle();
        initAttributeValue();
    }

    private void initAttributeValue() {
        mSpeedTextView.setTextColor(valueTextColor);
        mSpeedTextView.setText("" + value);
        mSpeedTextView.setShadowLayer(20, 0, 0, valueTextColor);
        mSpeedTextView.setTextSize(speedTextSize);
        mSpeedBgTextView.setTextSize(speedTextSize);
        mSpeedUnitTextView.setText(unit);
        mSpeedUnitTextView.setTextColor(unitTextColor);
        mSpeedUnitTextView.setShadowLayer(20, 0, 0, unitTextColor);
        mSpeedUnitTextView.setTextSize(unitTextSize);
    }

    public void updateValue(String value, boolean showDot, boolean showAlwaysBgNegative) {
        this.value = value;

        int numberOfBgElem = (value.length() == 1) ? 2 : value.length();
        boolean containsDot = value.contains(".");
        boolean containsNegative = value.contains("-");
        String bgText = "";

        if (containsDot) {
            --numberOfBgElem;
        }
        if (containsNegative) {
            --numberOfBgElem;
        }

        for (int i = 0; i < numberOfBgElem; ++i) {
            bgText += "8";
        }

        if (containsDot || showDot) {
            bgText += ".8";
        }

        if (containsNegative || showAlwaysBgNegative) {
            bgText = "-" + bgText;
        }

        mSpeedBgTextView.setText("" + bgText);
        mSpeedTextView.setText("" + value);
    }

    /**
     * Show unit text
     */
    public void showUnit() {
        mSpeedUnitTextView.setVisibility(VISIBLE);
    }

    /**
     * Hide unit text
     */
    public void hideUnit() {
        mSpeedUnitTextView.setVisibility(GONE);
    }

    /**
     * convert dp to <b>pixel</b>.
     *
     * @param dp to convert.
     * @return Dimension in pixel.
     */
    public float dpTOpx(float dp) {
        return dp * getContext().getResources().getDisplayMetrics().density;
    }

    /**
     * convert pixel to <b>dp</b>.
     *
     * @param px to convert.
     * @return Dimension in dp.
     */
    public float pxTOdp(float px) {
        return px / getContext().getResources().getDisplayMetrics().density;
    }
}
