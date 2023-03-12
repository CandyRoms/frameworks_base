/*
 * Copyright (C) 2021 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.android.systemui.battery;

import static java.lang.annotation.RetentionPolicy.SOURCE;

import android.animation.LayoutTransition;
import android.animation.ObjectAnimator;
import android.annotation.IntDef;
import android.annotation.IntRange;
import android.content.Context;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.Gravity;
import android.os.UserHandle;
import android.provider.Settings;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import com.android.systemui.tuner.TunerService;

import com.candy.org.utils.BatteryUtils;

import androidx.annotation.StyleRes;
import androidx.annotation.VisibleForTesting;

import com.android.app.animation.Interpolators;
import com.android.settingslib.graph.CircleBatteryDrawable;
import com.android.settingslib.graph.FullCircleBatteryDrawable;
import com.android.settingslib.graph.RLandscapeBatteryDrawable;
import com.android.settingslib.graph.LandscapeBatteryDrawable;
import com.android.settingslib.graph.ThemedBatteryDrawable;
import com.android.settingslib.graph.LandscapeBatteryA;
import com.android.settingslib.graph.LandscapeBatteryB;
import com.android.settingslib.graph.LandscapeBatteryC;
import com.android.settingslib.graph.LandscapeBatteryD;
import com.android.settingslib.graph.LandscapeBatteryE;
import com.android.settingslib.graph.LandscapeBatteryF;
import com.android.settingslib.graph.LandscapeBatteryG;
import com.android.settingslib.graph.LandscapeBatteryH;
import com.android.settingslib.graph.LandscapeBatteryI;
import com.android.settingslib.graph.LandscapeBatteryJ;
import com.android.settingslib.graph.LandscapeBatteryK;
import com.android.settingslib.graph.LandscapeBatteryL;
import com.android.settingslib.graph.LandscapeBatteryM;
import com.android.settingslib.graph.LandscapeBatteryN;
import com.android.settingslib.graph.LandscapeBatteryO;
import com.android.systemui.Dependency;
import com.android.systemui.DualToneHandler;
import com.android.systemui.plugins.DarkIconDispatcher;
import com.android.systemui.plugins.DarkIconDispatcher.DarkReceiver;
import com.android.systemui.res.R;
import com.android.systemui.statusbar.policy.BatteryController;

import java.io.PrintWriter;
import java.lang.annotation.Retention;
import java.text.NumberFormat;
import java.util.ArrayList;

public class BatteryMeterView extends LinearLayout implements DarkReceiver ,TunerService.Tunable {

    protected static final int BATTERY_STYLE_PORTRAIT = 0;
    protected static final int BATTERY_STYLE_CIRCLE = 1;
    protected static final int BATTERY_STYLE_DOTTED_CIRCLE = 2;
    protected static final int BATTERY_STYLE_FULL_CIRCLE = 3;
    protected static final int BATTERY_STYLE_TEXT = 4;
    protected static final int BATTERY_STYLE_HIDDEN = 5;
    protected static final int BATTERY_STYLE_RLANDSCAPE = 6;
    protected static final int BATTERY_STYLE_LANDSCAPE = 7;
    protected static final int BATTERY_STYLE_BIG_CIRCLE = 8;
    protected static final int BATTERY_STYLE_BIG_DOTTED_CIRCLE = 9;
    protected static final int BATTERY_STYLE_LANDSCAPEA = 10;
    protected static final int BATTERY_STYLE_LANDSCAPEB = 11;
    protected static final int BATTERY_STYLE_LANDSCAPEC = 12;
    protected static final int BATTERY_STYLE_LANDSCAPED = 13;
    protected static final int BATTERY_STYLE_LANDSCAPEE = 14;
    protected static final int BATTERY_STYLE_LANDSCAPEF = 15;
    protected static final int BATTERY_STYLE_LANDSCAPEG = 16;
    protected static final int BATTERY_STYLE_LANDSCAPEH = 17;
    protected static final int BATTERY_STYLE_LANDSCAPEI = 18;
    protected static final int BATTERY_STYLE_LANDSCAPEJ = 19;
    protected static final int BATTERY_STYLE_LANDSCAPEK = 20;
    protected static final int BATTERY_STYLE_LANDSCAPEL = 21;
    protected static final int BATTERY_STYLE_LANDSCAPEM = 22;
    protected static final int BATTERY_STYLE_LANDSCAPEN = 23;
    protected static final int BATTERY_STYLE_LANDSCAPEO = 24;

    @Retention(SOURCE)
    @IntDef({MODE_DEFAULT, MODE_ON, MODE_OFF, MODE_ESTIMATE})
    public @interface BatteryPercentMode {}
    public static final int MODE_DEFAULT = 0;
    public static final int MODE_ON = 1;
    public static final int MODE_OFF = 2;
    public static final int MODE_ESTIMATE = 3; // Not to be used
    private static final String EVL_BATTERY_IMAGE_ROTATION = "system:evl_battery_image_rotation";
    private static final String EVL_BATTERY_CUSTOM_DIMENSION = "system:evl_battery_custom_dimension";
    private static final String EVL_BATTERY_CUSTOM_MARGIN_LEFT = "system:evl_battery_custom_margin_left";
    private static final String EVL_BATTERY_CUSTOM_MARGIN_TOP = "system:evl_battery_custom_margin_top";
    private static final String EVL_BATTERY_CUSTOM_MARGIN_RIGHT = "system:evl_battery_custom_margin_right";
    private static final String EVL_BATTERY_CUSTOM_MARGIN_BOTTOM = "system:evl_battery_custom_margin_bottom";
    private static final String EVL_BATTERY_CUSTOM_SCALE_HEIGHT = "system:evl_battery_custom_scale_height";
    private static final String EVL_BATTERY_CUSTOM_SCALE_WIDTH = "system:evl_battery_custom_scale_width";
    
    private static final String EVL_BATTERY_SCALED_PERIMETER_ALPHA = "system:evl_battery_scaled_perimeter_alpha";
    private static final String EVL_BATTERY_SCALED_FILL_ALPHA = "system:evl_battery_scaled_fill_alpha";
    private static final String EVL_BATTERY_RAINBOW_FILL_COLOR = "system:evl_battery_rainbow_fill_color";
    private static final String EVL_BATTERY_CUSTOM_COLOR = "system:evl_battery_custom_color";
    private static final String EVL_BATTERY_CHARGING_COLOR = "system:evl_battery_charging_color";
    private static final String EVL_BATTERY_FILL_COLOR = "system:evl_battery_fill_color";
    private static final String EVL_BATTERY_FILL_GRADIENT_COLOR = "system:evl_battery_fill_gradient_color";
    private static final String EVL_BATTERY_POWERSAVE_COLOR = "system:evl_battery_powersave_color";
    private static final String EVL_BATTERY_POWERSAVEFILL_COLOR = "system:evl_battery_powersavefill_color"; 
    
    private static final String FLIPLAYOUTBATRE =
            "system:" + "FLIPLAYOUTBATRE";
    private static final String CUSTOM_CHARGE_SWITCH =
            "system:" + "CUSTOM_CHARGE_SWITCH";
    private static final String CUSTOM_CHARGE_SYMBOL =
            "system:" + "CUSTOM_CHARGE_SYMBOL";
            
    private static final String CUSTOM_CHARGING_ICON_SWITCH = "system:custom_charging_icon_switch";
    private static final String CUSTOM_CHARGING_ICON_STYLE = "system:custom_charging_icon_style";
    private static final String CUSTOM_CHARGING_ICON_ML = "system:custom_charging_icon_ml";
    private static final String CUSTOM_CHARGING_ICON_MR = "system:custom_charging_icon_mr";
    private static final String CUSTOM_CHARGING_ICON_WH = "system:custom_charging_icon_wh";

    private int ChargeSymbol;
    private boolean idcSwitch; 
    
    private boolean mChargingIconSwitch;
    private int mChargingIconStyle;
    private int mChargingIconML;
    private int mChargingIconMR;
    private int mChargingIconWH;
    
    private boolean mBatteryLayoutReverse;
    private boolean mBatteryCustomDimension;
    private int mBatteryMarginLeft;
    private int mBatteryMarginTop;
    private int mBatteryMarginRight;
    private int mBatteryMarginBottom;
    private int mBatteryScaleWidth;
    private int mBatteryScaleHeight;

    private boolean mScaledPerimeterAlpha;
    private boolean mScaledFillAlpha;
    private boolean mRainbowFillColor;
    private boolean mCustomBlendColor;
    private int mCustomChargingColor;
    private int mCustomFillColor;
    private int mCustomFillGradColor;
    private int mCustomPowerSaveColor;
    private int mCustomPowerSaveFillColor;

    private final ThemedBatteryDrawable mThemedDrawable;
    private final AccessorizedBatteryDrawable mAccessorizedDrawable;
    private final CircleBatteryDrawable mCircleDrawable;
    private final FullCircleBatteryDrawable mFullCircleDrawable;
    private final RLandscapeBatteryDrawable mRLandscapeDrawable;
    private final LandscapeBatteryDrawable mLandscapeDrawable;
    private final LandscapeBatteryA mLandscapeBatteryA;
    private final LandscapeBatteryB mLandscapeBatteryB;
    private final LandscapeBatteryC mLandscapeBatteryC;
    private final LandscapeBatteryD mLandscapeBatteryD;
    private final LandscapeBatteryE mLandscapeBatteryE;
    private final LandscapeBatteryF mLandscapeBatteryF;
    private final LandscapeBatteryG mLandscapeBatteryG;
    private final LandscapeBatteryH mLandscapeBatteryH;
    private final LandscapeBatteryI mLandscapeBatteryI;
    private final LandscapeBatteryJ mLandscapeBatteryJ;
    private final LandscapeBatteryK mLandscapeBatteryK;
    private final LandscapeBatteryL mLandscapeBatteryL;
    private final LandscapeBatteryM mLandscapeBatteryM;
    private final LandscapeBatteryN mLandscapeBatteryN;
    private final LandscapeBatteryO mLandscapeBatteryO;
    private final ImageView mBatteryIconView;
    private final ImageView mChargingIconView;
    private TextView mBatteryPercentView;

    private final @StyleRes int mPercentageStyleId;
    private int mTextColor;
    private int mLevel;
    private int mShowPercentMode = MODE_DEFAULT;
    private String mEstimateText = null;
    private boolean mPluggedIn;
    private boolean mIsBatteryDefender;
    private boolean mIsIncompatibleCharging;
    private boolean mDisplayShieldEnabled;
    private boolean mPCharging;
    // Error state where we know nothing about the current battery state
    private boolean mBatteryStateUnknown;
    // Lazily-loaded since this is expected to be a rare-if-ever state
    private Drawable mUnknownStateDrawable;

    private int mBatteryStyle = BATTERY_STYLE_PORTRAIT;
    private int mShowBatteryPercent;
    private boolean mBatteryPercentCharging;

    private DualToneHandler mDualToneHandler;
    private boolean mIsStaticColor = false;

    private final ArrayList<BatteryMeterViewCallbacks> mCallbacks = new ArrayList<>();

    private boolean plip;

    private BatteryEstimateFetcher mBatteryEstimateFetcher;

    public BatteryMeterView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public BatteryMeterView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);

        setOrientation(LinearLayout.HORIZONTAL);
        setGravity(Gravity.CENTER_VERTICAL | Gravity.START);

        TypedArray atts = context.obtainStyledAttributes(attrs, R.styleable.BatteryMeterView,
                defStyle, 0);
        final int frameColor = atts.getColor(R.styleable.BatteryMeterView_frameColor,
                context.getColor(com.android.settingslib.R.color.meter_background_color));
        mPercentageStyleId = atts.getResourceId(R.styleable.BatteryMeterView_textAppearance, 0);
        mAccessorizedDrawable = new AccessorizedBatteryDrawable(context, frameColor);
        mCircleDrawable = new CircleBatteryDrawable(context, frameColor);
        mFullCircleDrawable = new FullCircleBatteryDrawable(context, frameColor);
        mRLandscapeDrawable = new RLandscapeBatteryDrawable(context, frameColor);
        mLandscapeDrawable = new LandscapeBatteryDrawable(context, frameColor);
        mThemedDrawable = new ThemedBatteryDrawable(context, frameColor);
        mLandscapeBatteryA = new LandscapeBatteryA(context, frameColor);
        mLandscapeBatteryB = new LandscapeBatteryB(context, frameColor);
        mLandscapeBatteryC = new LandscapeBatteryC(context, frameColor);
        mLandscapeBatteryD = new LandscapeBatteryD(context, frameColor);
        mLandscapeBatteryE = new LandscapeBatteryE(context, frameColor);
        mLandscapeBatteryF = new LandscapeBatteryF(context, frameColor);
        mLandscapeBatteryG = new LandscapeBatteryG(context, frameColor);
        mLandscapeBatteryH = new LandscapeBatteryH(context, frameColor);
        mLandscapeBatteryI = new LandscapeBatteryI(context, frameColor);
        mLandscapeBatteryJ = new LandscapeBatteryJ(context, frameColor);
        mLandscapeBatteryK = new LandscapeBatteryK(context, frameColor);
        mLandscapeBatteryL = new LandscapeBatteryL(context, frameColor);
        mLandscapeBatteryM = new LandscapeBatteryM(context, frameColor);
        mLandscapeBatteryN = new LandscapeBatteryN(context, frameColor);
        mLandscapeBatteryO = new LandscapeBatteryO(context, frameColor);
        atts.recycle();

        setupLayoutTransition();

        mBatteryIconView = new ImageView(context);
        mBatteryStyle = Settings.System.getIntForUser(mContext.getContentResolver(),
                Settings.System.STATUS_BAR_BATTERY_STYLE, BATTERY_STYLE_PORTRAIT, UserHandle.USER_CURRENT);
        updateDrawable();

        int batteryHeight = mBatteryStyle == BATTERY_STYLE_CIRCLE || mBatteryStyle == BATTERY_STYLE_DOTTED_CIRCLE
                || mBatteryStyle == BATTERY_STYLE_FULL_CIRCLE ?
                getResources().getDimensionPixelSize(R.dimen.status_bar_battery_icon_circle_width) :
                getResources().getDimensionPixelSize(R.dimen.status_bar_battery_icon_height);
        int batteryWidth = mBatteryStyle == BATTERY_STYLE_CIRCLE || mBatteryStyle == BATTERY_STYLE_DOTTED_CIRCLE
                || mBatteryStyle == BATTERY_STYLE_FULL_CIRCLE ?
                getResources().getDimensionPixelSize(R.dimen.status_bar_battery_icon_circle_width) :
                getResources().getDimensionPixelSize(R.dimen.status_bar_battery_icon_width);

        final MarginLayoutParams mlp = new MarginLayoutParams(batteryWidth, batteryHeight);
        mlp.setMargins(0, 0, 0,
                getResources().getDimensionPixelOffset(R.dimen.battery_margin_bottom));
        addView(mBatteryIconView, mlp);
        
        // Charging icon
        mChargingIconView = new ImageView(context);
        addView(mChargingIconView, mlp);
        updateChargingIconView();

        updatePercentView();
        updateVisibility();

        mDualToneHandler = new DualToneHandler(context);
        // Init to not dark at all.
        if (isNightMode()) {
            onDarkChanged(new ArrayList<Rect>(), 0, DarkIconDispatcher.DEFAULT_ICON_TINT);
        }

        setClipChildren(false);
        setClipToPadding(false);
    }
    
    private void updateFlipper() {
        setOrientation(LinearLayout.HORIZONTAL);
        setGravity(Gravity.CENTER_VERTICAL | Gravity.START);
        if (plip) {
            setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        } else {
            setLayoutDirection(View.LAYOUT_DIRECTION_LTR);
        }
    }

    private boolean isNightMode() {
        return (mContext.getResources().getConfiguration().uiMode
                & Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES;
    }

    private void setupLayoutTransition() {
        LayoutTransition transition = new LayoutTransition();
        transition.setDuration(200);

        // Animates appearing/disappearing of the battery percentage text using fade-in/fade-out
        // and disables all other animation types
        ObjectAnimator appearAnimator = ObjectAnimator.ofFloat(null, "alpha", 0f, 1f);
        transition.setAnimator(LayoutTransition.APPEARING, appearAnimator);
        transition.setInterpolator(LayoutTransition.APPEARING, Interpolators.ALPHA_IN);

        ObjectAnimator disappearAnimator = ObjectAnimator.ofFloat(null, "alpha", 1f, 0f);
        transition.setInterpolator(LayoutTransition.DISAPPEARING, Interpolators.ALPHA_OUT);
        transition.setAnimator(LayoutTransition.DISAPPEARING, disappearAnimator);

        transition.setAnimator(LayoutTransition.CHANGE_APPEARING, null);
        transition.setAnimator(LayoutTransition.CHANGE_DISAPPEARING, null);
        transition.setAnimator(LayoutTransition.CHANGING, null);

        setLayoutTransition(transition);
    }

    public int getBatteryStyle() {
        return mBatteryStyle;
    }

    public void setBatteryStyle(int batteryStyle) {
        if (batteryStyle == mBatteryStyle) return;
        mBatteryStyle = batteryStyle;
        updateBatteryStyle();
    }

    protected void updateBatteryStyle() {
        updateDrawable();
        scaleBatteryMeterViews();
        updatePercentView();
        updateVisibility();
    }

    public void setBatteryPercent(int showBatteryPercent) {
        if (showBatteryPercent == mShowBatteryPercent) return;
        mShowBatteryPercent = showBatteryPercent;
        updatePercentView();
    }

    protected void setBatteryPercentCharging(boolean batteryPercentCharging) {
        if (batteryPercentCharging == mBatteryPercentCharging) return;
        mBatteryPercentCharging = batteryPercentCharging;
        updatePercentView();
    }

    public void setForceShowPercent(boolean show) {
        setPercentShowMode(show ? MODE_ON : MODE_DEFAULT);
    }

    /**
     * Force a particular mode of showing percent
     *
     * 0 - No preference
     * 1 - Force on
     * 2 - Force off
     * 3 - Estimate
     * @param mode desired mode (none, on, off)
     */
    public void setPercentShowMode(@BatteryPercentMode int mode) {
        if (mode == mShowPercentMode) return;
        mShowPercentMode = mode;
        updateShowPercent();
        updatePercentText();
    }

    @Override
    protected void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        updateBatteryStyle();
        mAccessorizedDrawable.notifyDensityChanged();
    }

    public void setColorsFromContext(Context context) {
        if (context == null) {
            return;
        }

        mDualToneHandler.setColorsFromContext(context);
    }

    @Override
    public boolean hasOverlappingRendering() {
        return false;
    }
    
    @Override
    public void onTuningChanged(String key, String newValue) {
        if (EVL_BATTERY_IMAGE_ROTATION.equals(key)) {
            mBatteryLayoutReverse = TunerService.parseIntegerSwitch(newValue, false);
        } else if (EVL_BATTERY_CUSTOM_DIMENSION.equals(key)) {
            mBatteryCustomDimension = TunerService.parseIntegerSwitch(newValue, false);
        } else if (EVL_BATTERY_CUSTOM_MARGIN_LEFT.equals(key)) {
            mBatteryMarginLeft = TunerService.parseInteger(newValue, 0);
        } else if (EVL_BATTERY_CUSTOM_MARGIN_TOP.equals(key)) {
            mBatteryMarginTop = TunerService.parseInteger(newValue, 0);
        } else if (EVL_BATTERY_CUSTOM_MARGIN_RIGHT.equals(key)) {
            mBatteryMarginRight = TunerService.parseInteger(newValue, 0);
        } else if (EVL_BATTERY_CUSTOM_MARGIN_BOTTOM.equals(key)) {
            mBatteryMarginBottom = TunerService.parseInteger(newValue, 0);
        } else if (EVL_BATTERY_CUSTOM_SCALE_HEIGHT.equals(key)) {
            mBatteryScaleHeight = TunerService.parseInteger(newValue, 20);
        } else if (EVL_BATTERY_CUSTOM_SCALE_WIDTH.equals(key)) {
            mBatteryScaleWidth = TunerService.parseInteger(newValue, 28);
        } else if (EVL_BATTERY_SCALED_PERIMETER_ALPHA.equals(key)) {
            mScaledPerimeterAlpha = TunerService.parseIntegerSwitch(newValue, false);
        } else if (EVL_BATTERY_SCALED_FILL_ALPHA.equals(key)) {
            mScaledFillAlpha = TunerService.parseIntegerSwitch(newValue, false);
        } else if (EVL_BATTERY_RAINBOW_FILL_COLOR.equals(key)) {
            mRainbowFillColor = TunerService.parseIntegerSwitch(newValue, false);
        } else if (EVL_BATTERY_CUSTOM_COLOR.equals(key)) {
            mCustomBlendColor = TunerService.parseIntegerSwitch(newValue, false);
        } else if (EVL_BATTERY_CHARGING_COLOR.equals(key)) {
            mCustomChargingColor = TunerService.parseInteger(newValue, Color.BLACK);
        } else if (EVL_BATTERY_FILL_COLOR.equals(key)) {
            mCustomFillColor = TunerService.parseInteger(newValue, Color.BLACK);
        } else if (EVL_BATTERY_FILL_GRADIENT_COLOR.equals(key)) {
            mCustomFillGradColor = TunerService.parseInteger(newValue, Color.BLACK);
        } else if (EVL_BATTERY_POWERSAVE_COLOR.equals(key)) {
            mCustomPowerSaveColor = TunerService.parseInteger(newValue, Color.BLACK);
        } else if (EVL_BATTERY_POWERSAVEFILL_COLOR.equals(key)) {
            mCustomPowerSaveFillColor = TunerService.parseInteger(newValue, Color.BLACK);
        } else if (FLIPLAYOUTBATRE.equals(key)) {
            plip = TunerService.parseIntegerSwitch(newValue, false);
        } else if (CUSTOM_CHARGE_SYMBOL.equals(key)) {
            ChargeSymbol = TunerService.parseInteger(newValue, 0);
            setPercentTextAtCurrentLevel();
        } else if (CUSTOM_CHARGE_SWITCH.equals(key)) {
            idcSwitch = TunerService.parseIntegerSwitch(newValue, false);
            setPercentTextAtCurrentLevel();
        } else if (CUSTOM_CHARGING_ICON_SWITCH.equals(key)) {
            mChargingIconSwitch = TunerService.parseIntegerSwitch(newValue, false);
        } else if (CUSTOM_CHARGING_ICON_STYLE.equals(key)) {
            mChargingIconStyle = TunerService.parseInteger(newValue, 0);
        } else if (CUSTOM_CHARGING_ICON_ML.equals(key)) {
            mChargingIconML = TunerService.parseInteger(newValue, 1);
        } else if (CUSTOM_CHARGING_ICON_MR.equals(key)) {
            mChargingIconMR = TunerService.parseInteger(newValue, 0);
        } else if (CUSTOM_CHARGING_ICON_WH.equals(key)) {
            mChargingIconWH = TunerService.parseInteger(newValue, 14);
        }
        updateSettings();
    }
    
    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        Dependency.get(TunerService.class)
            .addTunable(this, new String[] {
                            EVL_BATTERY_IMAGE_ROTATION,
                            EVL_BATTERY_CUSTOM_DIMENSION,
                            EVL_BATTERY_CUSTOM_MARGIN_LEFT,
                            EVL_BATTERY_CUSTOM_MARGIN_TOP,
                            EVL_BATTERY_CUSTOM_MARGIN_RIGHT,
                            EVL_BATTERY_CUSTOM_MARGIN_BOTTOM,
                            EVL_BATTERY_CUSTOM_SCALE_HEIGHT,
                            EVL_BATTERY_CUSTOM_SCALE_WIDTH,
                            EVL_BATTERY_SCALED_PERIMETER_ALPHA,
                            EVL_BATTERY_SCALED_FILL_ALPHA,
                            EVL_BATTERY_RAINBOW_FILL_COLOR,
                            EVL_BATTERY_CUSTOM_COLOR,
                            EVL_BATTERY_CHARGING_COLOR,
                            EVL_BATTERY_FILL_COLOR,
                            EVL_BATTERY_FILL_GRADIENT_COLOR,
                            EVL_BATTERY_POWERSAVE_COLOR,
                            EVL_BATTERY_POWERSAVEFILL_COLOR,
                            CUSTOM_CHARGE_SWITCH, 
                            CUSTOM_CHARGE_SYMBOL,
                            CUSTOM_CHARGING_ICON_SWITCH,
                            CUSTOM_CHARGING_ICON_STYLE,
                            CUSTOM_CHARGING_ICON_ML,
                            CUSTOM_CHARGING_ICON_MR,
                            CUSTOM_CHARGING_ICON_WH,
                            FLIPLAYOUTBATRE
                        });
    }
    
    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        Dependency.get(TunerService.class).removeTunable(this);
    }

    private void updateRotationLandscape() {
        if (mBatteryLayoutReverse) {
            if (getBatteryStyle() == BATTERY_STYLE_LANDSCAPEA
                || getBatteryStyle() == BATTERY_STYLE_LANDSCAPEB
                || getBatteryStyle() == BATTERY_STYLE_LANDSCAPEC
                || getBatteryStyle() == BATTERY_STYLE_LANDSCAPEF
                || getBatteryStyle() == BATTERY_STYLE_LANDSCAPEG
                || getBatteryStyle() == BATTERY_STYLE_LANDSCAPEH
                || getBatteryStyle() == BATTERY_STYLE_LANDSCAPEI
                || getBatteryStyle() == BATTERY_STYLE_LANDSCAPEJ
                || getBatteryStyle() == BATTERY_STYLE_LANDSCAPEK
                || getBatteryStyle() == BATTERY_STYLE_LANDSCAPEL
                || getBatteryStyle() == BATTERY_STYLE_LANDSCAPEM
                || getBatteryStyle() == BATTERY_STYLE_LANDSCAPEN
                || getBatteryStyle() == BATTERY_STYLE_LANDSCAPEO) {
                mBatteryIconView.setRotation(180f);
            } else {
                mBatteryIconView.setRotation(0f);
            }
        } else {
            mBatteryIconView.setRotation(0f);
        }
    }
    
    public void setIsQsPercent(boolean isQs) {
        mLandscapeBatteryL.setQsPercent(isQs);
    }

    private void updateChargingIconView() {
        final Context c = mContext;

        mLandscapeBatteryA.setCustomChargingIcon(mChargingIconSwitch);
        mLandscapeBatteryB.setCustomChargingIcon(mChargingIconSwitch);
        mLandscapeBatteryC.setCustomChargingIcon(mChargingIconSwitch);
        mLandscapeBatteryD.setCustomChargingIcon(mChargingIconSwitch);
        mLandscapeBatteryE.setCustomChargingIcon(mChargingIconSwitch);
        mLandscapeBatteryF.setCustomChargingIcon(mChargingIconSwitch);
        mLandscapeBatteryG.setCustomChargingIcon(mChargingIconSwitch);
        mLandscapeBatteryH.setCustomChargingIcon(mChargingIconSwitch);
        mLandscapeBatteryJ.setCustomChargingIcon(mChargingIconSwitch);
        mLandscapeBatteryK.setCustomChargingIcon(mChargingIconSwitch);
        mLandscapeBatteryL.setCustomChargingIcon(mChargingIconSwitch);
        mLandscapeBatteryM.setCustomChargingIcon(mChargingIconSwitch);
        mLandscapeBatteryN.setCustomChargingIcon(mChargingIconSwitch);
        mLandscapeBatteryO.setCustomChargingIcon(mChargingIconSwitch);

        Drawable d = null;
        switch (mChargingIconStyle) {
            case 0:
                d = c.getDrawable(R.drawable.ic_charging_bold);
                break;
            case 1:
                d = c.getDrawable(R.drawable.ic_charging_asus);
                break;
            case 2:
                d = c.getDrawable(R.drawable.ic_charging_buddy);
                break;
            case 3:
                d = c.getDrawable(R.drawable.ic_charging_evplug);
                break;
            case 4:
                d = c.getDrawable(R.drawable.ic_charging_idc);
                break;
            case 5:
                d = c.getDrawable(R.drawable.ic_charging_ios);
                break;
            case 6:
                d = c.getDrawable(R.drawable.ic_charging_koplak);
                break;
            case 7:
                d = c.getDrawable(R.drawable.ic_charging_miui);
                break;
            case 8:
                d = c.getDrawable(R.drawable.ic_charging_mmk);
                break;
            case 9:
                d = c.getDrawable(R.drawable.ic_charging_moto);
                break;
            case 10:
                d = c.getDrawable(R.drawable.ic_charging_nokia);
                break;
            case 11:
                d = c.getDrawable(R.drawable.ic_charging_plug);
                break;
            case 12:
                d = c.getDrawable(R.drawable.ic_charging_powercable);
                break;
            case 13:
                d = c.getDrawable(R.drawable.ic_charging_powercord);
                break;
            case 14:
                d = c.getDrawable(R.drawable.ic_charging_powerstation);
                break;
            case 15:
                d = c.getDrawable(R.drawable.ic_charging_realme);
                break;
            case 16:
                d = c.getDrawable(R.drawable.ic_charging_soak);
                break;
            case 17:
                d = c.getDrawable(R.drawable.ic_charging_stres);
                break;
            case 18:
                d = c.getDrawable(R.drawable.ic_charging_strip);
                break;
            case 19:
                d = c.getDrawable(R.drawable.ic_charging_usbcable);
                break;
            case 20:
                d = c.getDrawable(R.drawable.ic_charging_xiaomi);
                break;
            default:
                d = null;
        }

        if (d != null)
            mChargingIconView.setImageDrawable(d);

        int l = BatteryUtils.dpToPx(mChargingIconML);
        int r = BatteryUtils.dpToPx(mChargingIconMR);
        int wh = BatteryUtils.dpToPx(mChargingIconWH);

        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(wh, wh);
        lp.setMargins(l, 0, r, getResources().getDimensionPixelSize(R.dimen.battery_margin_bottom));
        mChargingIconView.setLayoutParams(lp);

        mChargingIconView.setVisibility(
            mPluggedIn && mChargingIconSwitch ? View.VISIBLE : View.GONE
        );
    }
    
    private void updateCustomizeBatteryDrawable() {
        
        mThemedDrawable.customizeBatteryDrawable(
            mBatteryLayoutReverse,
            mScaledPerimeterAlpha,
            mScaledFillAlpha,
            mCustomBlendColor,
            mCustomFillColor,
            mCustomFillGradColor,
            mCustomChargingColor,
            mCustomPowerSaveColor,
            mCustomPowerSaveFillColor);
        
        mLandscapeBatteryA.customizeBatteryDrawable(
            mBatteryLayoutReverse,
            mScaledPerimeterAlpha,
            mScaledFillAlpha,
            mCustomBlendColor,
            mCustomFillColor,
            mCustomFillGradColor,
            mCustomChargingColor,
            mCustomPowerSaveColor,
            mCustomPowerSaveFillColor);

        mLandscapeBatteryB.customizeBatteryDrawable(
            mBatteryLayoutReverse,
            mScaledPerimeterAlpha,
            mScaledFillAlpha,
            mCustomBlendColor,
            mCustomFillColor,
            mCustomFillGradColor,
            mCustomChargingColor,
            mCustomPowerSaveColor,
            mCustomPowerSaveFillColor);

        mLandscapeBatteryC.customizeBatteryDrawable(
            mBatteryLayoutReverse,
            mScaledPerimeterAlpha,
            mScaledFillAlpha,
            mCustomBlendColor,
            mCustomFillColor,
            mCustomFillGradColor,
            mCustomChargingColor,
            mCustomPowerSaveColor,
            mCustomPowerSaveFillColor);

        mLandscapeBatteryD.customizeBatteryDrawable(
            mScaledPerimeterAlpha,
            mScaledFillAlpha,
            mCustomBlendColor,
            mCustomFillColor,
            mCustomFillGradColor,
            mCustomChargingColor,
            mCustomPowerSaveColor,
            mCustomPowerSaveFillColor);

        mLandscapeBatteryE.customizeBatteryDrawable(
            mScaledPerimeterAlpha,
            mScaledFillAlpha,
            mCustomBlendColor,
            mCustomFillColor,
            mCustomFillGradColor,
            mCustomChargingColor,
            mCustomPowerSaveColor,
            mCustomPowerSaveFillColor);

        mLandscapeBatteryF.customizeBatteryDrawable(
            mScaledPerimeterAlpha,
            mScaledFillAlpha,
            mCustomBlendColor,
            mCustomFillColor,
            mCustomFillGradColor,
            mCustomChargingColor,
            mCustomPowerSaveColor,
            mCustomPowerSaveFillColor);

        mLandscapeBatteryG.customizeBatteryDrawable(
            mBatteryLayoutReverse,
            mScaledPerimeterAlpha,
            mScaledFillAlpha,
            mCustomBlendColor,
            mCustomFillColor,
            mCustomFillGradColor,
            mCustomChargingColor,
            mCustomPowerSaveColor,
            mCustomPowerSaveFillColor);

        mLandscapeBatteryH.customizeBatteryDrawable(
            mBatteryLayoutReverse,
            mScaledPerimeterAlpha,
            mScaledFillAlpha,
            mCustomBlendColor,
            mCustomFillColor,
            mCustomFillGradColor,
            mCustomChargingColor,
            mCustomPowerSaveColor,
            mCustomPowerSaveFillColor);

        mLandscapeBatteryI.customizeBatteryDrawable(
            mBatteryLayoutReverse,
            mScaledPerimeterAlpha,
            mScaledFillAlpha,
            mCustomBlendColor,
            mRainbowFillColor,
            mCustomFillColor,
            mCustomFillGradColor,
            mCustomChargingColor,
            mCustomPowerSaveColor,
            mCustomPowerSaveFillColor);

        mLandscapeBatteryJ.customizeBatteryDrawable(
            mScaledPerimeterAlpha,
            mScaledFillAlpha,
            mCustomBlendColor,
            mRainbowFillColor,
            mCustomFillColor,
            mCustomFillGradColor,
            mCustomChargingColor,
            mCustomPowerSaveColor,
            mCustomPowerSaveFillColor);

        mLandscapeBatteryK.customizeBatteryDrawable(
            mBatteryLayoutReverse,
            mScaledPerimeterAlpha,
            mScaledFillAlpha,
            mCustomBlendColor,
            mCustomFillColor,
            mCustomFillGradColor,
            mCustomChargingColor,
            mCustomPowerSaveColor,
            mCustomPowerSaveFillColor);

        mLandscapeBatteryL.customizeBatteryDrawable(
            mBatteryLayoutReverse,
            mCustomBlendColor,
            mCustomFillColor,
            mCustomFillGradColor,
            mCustomChargingColor,
            mCustomPowerSaveColor,
            mCustomPowerSaveFillColor);

        mLandscapeBatteryM.customizeBatteryDrawable(
            mBatteryLayoutReverse,
            mCustomBlendColor,
            mCustomFillColor,
            mCustomFillGradColor,
            mCustomChargingColor,
            mCustomPowerSaveColor,
            mCustomPowerSaveFillColor);

        mLandscapeBatteryN.customizeBatteryDrawable(
            mBatteryLayoutReverse,
            mScaledPerimeterAlpha,
            mScaledFillAlpha,
            mCustomBlendColor,
            mCustomFillColor,
            mCustomFillGradColor,
            mCustomChargingColor,
            mCustomPowerSaveColor,
            mCustomPowerSaveFillColor);

        mLandscapeBatteryO.customizeBatteryDrawable(
            mBatteryLayoutReverse,
            mScaledPerimeterAlpha,
            mScaledFillAlpha,
            mCustomBlendColor,
            mCustomFillColor,
            mCustomFillGradColor,
            mCustomChargingColor,
            mCustomPowerSaveColor,
            mCustomPowerSaveFillColor);
    }
    
    private void updateSettings() {
        updateCustomizeBatteryDrawable();
        updateChargingIconView();
        updateRotationLandscape();
        updateShowPercent();
        scaleBatteryMeterViews();
        updateFlipper();
    }

    /**
     * Update battery level
     *
     * @param level     int between 0 and 100 (representing percentage value)
     * @param pluggedIn whether the device is plugged in or not
     */
    public void onBatteryLevelChanged(@IntRange(from = 0, to = 100) int level, boolean pluggedIn) {
        if (mLevel != level) {
            mLevel = level;
            mAccessorizedDrawable.setBatteryLevel(mLevel);
            mThemedDrawable.setBatteryLevel(mLevel);
            mCircleDrawable.setBatteryLevel(mLevel);
            mFullCircleDrawable.setBatteryLevel(mLevel);
            mRLandscapeDrawable.setBatteryLevel(mLevel);
            mLandscapeDrawable.setBatteryLevel(mLevel);
            mLandscapeBatteryA.setBatteryLevel(mLevel);
            mLandscapeBatteryB.setBatteryLevel(mLevel);
            mLandscapeBatteryC.setBatteryLevel(mLevel);
            mLandscapeBatteryD.setBatteryLevel(mLevel);
            mLandscapeBatteryE.setBatteryLevel(mLevel);
            mLandscapeBatteryF.setBatteryLevel(mLevel);
            mLandscapeBatteryG.setBatteryLevel(mLevel);
            mLandscapeBatteryH.setBatteryLevel(mLevel);
            mLandscapeBatteryI.setBatteryLevel(mLevel);
            mLandscapeBatteryJ.setBatteryLevel(mLevel);
            mLandscapeBatteryK.setBatteryLevel(mLevel);
            mLandscapeBatteryL.setBatteryLevel(mLevel);
            mLandscapeBatteryM.setBatteryLevel(mLevel);
            mLandscapeBatteryN.setBatteryLevel(mLevel);
            mLandscapeBatteryO.setBatteryLevel(mLevel);
            updatePercentText();
        }
        if (mPluggedIn != pluggedIn) {
            mPluggedIn = pluggedIn;
            mAccessorizedDrawable.setCharging(mPluggedIn);
            mThemedDrawable.setCharging(mPluggedIn);
            mCircleDrawable.setCharging(mPluggedIn);
            mFullCircleDrawable.setCharging(mPluggedIn);
            mFullCircleDrawable.setCharging(mPluggedIn);
            mRLandscapeDrawable.setCharging(mPluggedIn);
            mLandscapeDrawable.setCharging(mPluggedIn);
            mLandscapeDrawable.setCharging(mPluggedIn);
            mLandscapeBatteryA.setCharging(mPluggedIn);
            mLandscapeBatteryB.setCharging(mPluggedIn);
            mLandscapeBatteryC.setCharging(mPluggedIn);
            mLandscapeBatteryD.setCharging(mPluggedIn);
            mLandscapeBatteryE.setCharging(mPluggedIn);
            mLandscapeBatteryF.setCharging(mPluggedIn);
            mLandscapeBatteryG.setCharging(mPluggedIn);
            mLandscapeBatteryH.setCharging(mPluggedIn);
            mLandscapeBatteryI.setCharging(mPluggedIn);
            mLandscapeBatteryJ.setCharging(mPluggedIn);
            mLandscapeBatteryK.setCharging(mPluggedIn);
            mLandscapeBatteryL.setCharging(mPluggedIn);
            mLandscapeBatteryM.setCharging(mPluggedIn);
            mLandscapeBatteryN.setCharging(mPluggedIn);
            mLandscapeBatteryO.setCharging(mPluggedIn);
            updateShowPercent();
            updatePercentText();
            updateChargingIconView();
        }
    }

    void onPowerSaveChanged(boolean isPowerSave) {
        mAccessorizedDrawable.setPowerSaveEnabled(isPowerSave);
        mCircleDrawable.setPowerSaveEnabled(isPowerSave);
        mFullCircleDrawable.setPowerSaveEnabled(isPowerSave);
        mRLandscapeDrawable.setPowerSaveEnabled(isPowerSave);
        mLandscapeDrawable.setPowerSaveEnabled(isPowerSave);
        mThemedDrawable.setPowerSaveEnabled(isPowerSave);
        mLandscapeBatteryA.setPowerSaveEnabled(isPowerSave);
        mLandscapeBatteryB.setPowerSaveEnabled(isPowerSave);
        mLandscapeBatteryC.setPowerSaveEnabled(isPowerSave);
        mLandscapeBatteryD.setPowerSaveEnabled(isPowerSave);
        mLandscapeBatteryE.setPowerSaveEnabled(isPowerSave);
        mLandscapeBatteryF.setPowerSaveEnabled(isPowerSave);
        mLandscapeBatteryG.setPowerSaveEnabled(isPowerSave);
        mLandscapeBatteryH.setPowerSaveEnabled(isPowerSave);
        mLandscapeBatteryI.setPowerSaveEnabled(isPowerSave);
        mLandscapeBatteryJ.setPowerSaveEnabled(isPowerSave);
        mLandscapeBatteryK.setPowerSaveEnabled(isPowerSave);
        mLandscapeBatteryL.setPowerSaveEnabled(isPowerSave);
        mLandscapeBatteryM.setPowerSaveEnabled(isPowerSave);
        mLandscapeBatteryN.setPowerSaveEnabled(isPowerSave);
        mLandscapeBatteryO.setPowerSaveEnabled(isPowerSave);
    }

    void onIsBatteryDefenderChanged(boolean isBatteryDefender) {
        boolean valueChanged = mIsBatteryDefender != isBatteryDefender;
        mIsBatteryDefender = isBatteryDefender;
        if (valueChanged) {
            updateContentDescription();
            // The battery drawable is a different size depending on whether it's currently
            // overheated or not, so we need to re-scale the view when overheated changes.
            scaleBatteryMeterViews();
        }
    }

    void onIsIncompatibleChargingChanged(boolean isIncompatibleCharging) {
        boolean valueChanged = mIsIncompatibleCharging != isIncompatibleCharging;
        mIsIncompatibleCharging = isIncompatibleCharging;
        if (valueChanged) {
            mAccessorizedDrawable.setCharging(isCharging());
            updateContentDescription();
        }
    }

    private TextView loadPercentView() {
        return (TextView) LayoutInflater.from(getContext())
                .inflate(R.layout.battery_percentage_view, null);
    }

    /**
     * Updates percent view by removing old one and reinflating if necessary
     */
    public void updatePercentView() {
        if (mBatteryPercentView != null) {
            removeView(mBatteryPercentView);
            mBatteryPercentView = null;
        }
        updateShowPercent();
    }

    /**
     * Sets the fetcher that should be used to get the estimated time remaining for the user's
     * battery.
     */
    void setBatteryEstimateFetcher(BatteryEstimateFetcher fetcher) {
        mBatteryEstimateFetcher = fetcher;
    }

    void setDisplayShieldEnabled(boolean displayShieldEnabled) {
        mDisplayShieldEnabled = displayShieldEnabled;
    }

    void updatePercentText() {
        if (mBatteryStateUnknown) {
            return;
        }

        if (mBatteryEstimateFetcher == null) {
            setPercentTextAtCurrentLevel();
            return;
        }
        
        if (mBatteryPercentView != null) {
            if (mShowPercentMode == MODE_ESTIMATE && !mPluggedIn) {
                mBatteryEstimateFetcher.fetchBatteryTimeRemainingEstimate(
                        (String estimate) -> {
                    if (mBatteryPercentView == null) {
                        return;
                    }
                    if (estimate != null && mShowPercentMode == MODE_ESTIMATE) {
                        mEstimateText = estimate;
                        mBatteryPercentView.setText(estimate);
                        updateContentDescription();
                    } else {
                        setPercentTextAtCurrentLevel();
                    }
                });
            } else {
                setPercentTextAtCurrentLevel();
            }
        } else {
            updateContentDescription();
        }
    }

    private void setPercentTextAtCurrentLevel() {
        if (mBatteryPercentView == null) return;

        String PercentText = NumberFormat.getPercentInstance().format(mLevel / 100f);
        // Setting text actually triggers a layout pass (because the text view is set to
        // wrap_content width and TextView always relayouts for this). Avoid needless
        // relayout if the text didn't actually change.
        if (!TextUtils.equals(mBatteryPercentView.getText(), PercentText) || mPCharging != mPluggedIn) {
            mPCharging = mPluggedIn;
            // Use the high voltage symbol ⚡ (u26A1 unicode) but prevent the system
            // to load its emoji colored variant with the uFE0E flag
            // only use it when there is no batt icon showing
            String indication = mPluggedIn && (getBatteryStyle() == BATTERY_STYLE_TEXT)
                    ? "\u26A1 " : "";
            String indication0 = mPluggedIn && (getBatteryStyle() == BATTERY_STYLE_TEXT)
                    ? "" : "";
            String indication1 = mPluggedIn && (getBatteryStyle() == BATTERY_STYLE_TEXT)
                    ? "\u2623 " : "";
            String indication2 = mPluggedIn && (getBatteryStyle() == BATTERY_STYLE_TEXT)
                    ? "\u2605 " : "";
            String indication3 = mPluggedIn && (getBatteryStyle() == BATTERY_STYLE_TEXT)
                    ? "\u263a " : "";
            String indication4 = mPluggedIn && (getBatteryStyle() == BATTERY_STYLE_TEXT)
                    ? "\u267d " : "";
            String indication5 = mPluggedIn && (getBatteryStyle() == BATTERY_STYLE_TEXT)
                    ? "\u199f " : "";
            String indication6 = mPluggedIn && (getBatteryStyle() == BATTERY_STYLE_TEXT)
                    ? "\u2741 " : "";
            String indication7 = mPluggedIn && (getBatteryStyle() == BATTERY_STYLE_TEXT)
                    ? "\u274a " : "";
            String indication8 = mPluggedIn && (getBatteryStyle() == BATTERY_STYLE_TEXT)
                    ? "\u2746 " : "";
            String indication9 = mPluggedIn && (getBatteryStyle() == BATTERY_STYLE_TEXT)
                    ? "\u224b " : "";
            if (ChargeSymbol == 1) {
                     mBatteryPercentView.setText(indication1 + PercentText);
                  } else if (ChargeSymbol == 2) {
                     mBatteryPercentView.setText(indication2 + PercentText);
                  } else if (ChargeSymbol == 3) {
                     mBatteryPercentView.setText(indication3 + PercentText);
                  } else if (ChargeSymbol == 4) {
                     mBatteryPercentView.setText(indication4 + PercentText);
                  } else if (ChargeSymbol == 5) {
                     mBatteryPercentView.setText(indication5 + PercentText);
                  } else if (ChargeSymbol == 6) {
                     mBatteryPercentView.setText(indication6 + PercentText);
                  } else if (ChargeSymbol == 7) {
                     mBatteryPercentView.setText(indication7 + PercentText);
                  } else if (ChargeSymbol == 8) {
                     mBatteryPercentView.setText(indication8 + PercentText);
                  } else if (ChargeSymbol == 9) {
                     mBatteryPercentView.setText(indication9 + PercentText);
                  } else {
                    mBatteryPercentView.setText(indication + PercentText);       
             }
       	}
        setContentDescription(
                getContext().getString(mPluggedIn ? R.string.accessibility_battery_level_charging
                        : R.string.accessibility_battery_level, mLevel));
    }

    private void updateContentDescription() {
        Context context = getContext();

        String contentDescription;
        if (mBatteryStateUnknown) {
            contentDescription = context.getString(R.string.accessibility_battery_unknown);
        } else if (mShowPercentMode == MODE_ESTIMATE && !TextUtils.isEmpty(mEstimateText)) {
            contentDescription = context.getString(
                    mIsBatteryDefender
                            ? R.string.accessibility_battery_level_charging_paused_with_estimate
                            : R.string.accessibility_battery_level_with_estimate,
                    mLevel,
                    mEstimateText);
        } else if (mIsBatteryDefender) {
            contentDescription =
                    context.getString(R.string.accessibility_battery_level_charging_paused, mLevel);
        } else if (isCharging()) {
            contentDescription =
                    context.getString(R.string.accessibility_battery_level_charging, mLevel);
        } else {
            contentDescription = context.getString(R.string.accessibility_battery_level, mLevel);
        }

        setContentDescription(contentDescription);
    }

    private void removeBatteryPercentView() {
        if (mBatteryPercentView != null) {
            removeView(mBatteryPercentView);
            mBatteryPercentView = null;
        }
    }

    void updateShowPercent() {
        boolean drawPercentInside = mShowBatteryPercent == 1
                                    && !isCharging() && !mBatteryStateUnknown;
        boolean showPercent = mShowBatteryPercent >= 2
                                    || mBatteryStyle == BATTERY_STYLE_TEXT
                                    || mShowPercentMode == MODE_ON;
        showPercent = showPercent && !mBatteryStateUnknown
                                    && mBatteryStyle != BATTERY_STYLE_HIDDEN;
                                    
        mThemedDrawable.setShowPercent(drawPercentInside);
        mLandscapeBatteryA.setShowPercent(drawPercentInside);
        mLandscapeBatteryB.setShowPercent(drawPercentInside);
        mLandscapeBatteryC.setShowPercent(drawPercentInside);
        mLandscapeBatteryD.setShowPercent(drawPercentInside);
        mLandscapeBatteryE.setShowPercent(drawPercentInside);
        mLandscapeBatteryF.setShowPercent(drawPercentInside);
        mLandscapeBatteryG.setShowPercent(drawPercentInside);
        mLandscapeBatteryH.setShowPercent(drawPercentInside);
        mLandscapeBatteryI.setShowPercent(drawPercentInside);
        mLandscapeBatteryJ.setShowPercent(drawPercentInside);
        mLandscapeBatteryK.setShowPercent(drawPercentInside);
        mLandscapeBatteryL.setShowPercent(drawPercentInside);
        mLandscapeBatteryM.setShowPercent(drawPercentInside);
        mLandscapeBatteryN.setShowPercent(drawPercentInside);
        mLandscapeBatteryO.setShowPercent(drawPercentInside);
        mAccessorizedDrawable.showPercent(drawPercentInside);
        mCircleDrawable.setShowPercent(drawPercentInside);
        mFullCircleDrawable.setShowPercent(drawPercentInside);
        mRLandscapeDrawable.setShowPercent(drawPercentInside);
        mLandscapeDrawable.setShowPercent(drawPercentInside);

        if (showPercent || (mBatteryPercentCharging && isCharging())
                || mShowPercentMode == MODE_ESTIMATE) {
            if (mBatteryPercentView == null) {
                mBatteryPercentView = loadPercentView();
                if (mPercentageStyleId != 0) { // Only set if specified as attribute
                    mBatteryPercentView.setTextAppearance(mPercentageStyleId);
                }
                float fontHeight = mBatteryPercentView.getPaint().getFontMetricsInt(null);
                mBatteryPercentView.setLineHeight(TypedValue.COMPLEX_UNIT_PX, fontHeight);
                if (mTextColor != 0) mBatteryPercentView.setTextColor(mTextColor);
                updatePercentText();
                addView(mBatteryPercentView, new LayoutParams(
                        LayoutParams.WRAP_CONTENT,
                        (int) Math.ceil(fontHeight)));
            }
            if (mBatteryStyle == BATTERY_STYLE_HIDDEN || mBatteryStyle == BATTERY_STYLE_TEXT) {
                mBatteryPercentView.setPaddingRelative(0, 0, 0, 0);
            } else {
                Resources res = getContext().getResources();
                mBatteryPercentView.setPaddingRelative(
                        res.getDimensionPixelSize(R.dimen.battery_level_padding_start), 0, 0, 0);
                setLayoutDirection(mShowBatteryPercent > 2 ? View.LAYOUT_DIRECTION_RTL : View.LAYOUT_DIRECTION_LTR);
            }

        } else {
            removeBatteryPercentView();
        }
    }

    private void updateVisibility() {
        if (mBatteryStyle == BATTERY_STYLE_HIDDEN || mBatteryStyle == BATTERY_STYLE_TEXT) {
            mBatteryIconView.setVisibility(View.GONE);
            mBatteryIconView.setImageDrawable(null);
        } else {
            mBatteryIconView.setVisibility(View.VISIBLE);
            scaleBatteryMeterViews();
        }
        for (int i = 0; i < mCallbacks.size(); i++) {
            mCallbacks.get(i).onHiddenBattery(mBatteryStyle == BATTERY_STYLE_HIDDEN);
        }
    }

    private Drawable getUnknownStateDrawable() {
        if (mUnknownStateDrawable == null) {
            mUnknownStateDrawable = mContext.getDrawable(R.drawable.ic_battery_unknown);
            mUnknownStateDrawable.setTint(mTextColor);
        }

        return mUnknownStateDrawable;
    }

    void onBatteryUnknownStateChanged(boolean isUnknown) {
        if (mBatteryStateUnknown == isUnknown) {
            return;
        }

        mBatteryStateUnknown = isUnknown;
        updateContentDescription();

        if (mBatteryStateUnknown) {
            mBatteryIconView.setImageDrawable(getUnknownStateDrawable());
        } else {
            updateDrawable();
        }

        updateShowPercent();
    }

    /**
     * Looks up the scale factor for status bar icons and scales the battery view by that amount.
     */
    void scaleBatteryMeterViews() {
        if (mBatteryIconView == null) {
            return;
        }
        Resources res = getContext().getResources();
        TypedValue typedValue = new TypedValue();

        res.getValue(R.dimen.status_bar_icon_scale_factor, typedValue, true);
        float iconScaleFactor = typedValue.getFloat();
        
        int defaultHeight = res.getDimensionPixelSize(R.dimen.status_bar_battery_icon_height);
        int defaultWidth = res.getDimensionPixelSize(R.dimen.status_bar_battery_icon_width);
        int defaultMarginBottom = res.getDimensionPixelSize(R.dimen.battery_margin_bottom);
        int marginLeft = BatteryUtils.dpToPx(mBatteryMarginLeft);
        int marginTop = BatteryUtils.dpToPx(mBatteryMarginTop);
        int marginRight = BatteryUtils.dpToPx(mBatteryMarginRight);
        int marginBottom = BatteryUtils.dpToPx(mBatteryMarginBottom);
        int scaleHeight = BatteryUtils.dpToPx(mBatteryScaleHeight);
        int scaleWidth = BatteryUtils.dpToPx(mBatteryScaleWidth);
        float mainBatteryHeight = defaultHeight * iconScaleFactor;
        float mainBatteryWidth = defaultWidth * iconScaleFactor;

        int mBatteryStyle = getBatteryStyle();
        
        if (mBatteryCustomDimension) {
            if (mBatteryStyle == BATTERY_STYLE_LANDSCAPEA    ||
                mBatteryStyle == BATTERY_STYLE_LANDSCAPEB    ||
                mBatteryStyle == BATTERY_STYLE_LANDSCAPEC    ||
                mBatteryStyle == BATTERY_STYLE_LANDSCAPED    ||
                mBatteryStyle == BATTERY_STYLE_LANDSCAPEE    ||
                mBatteryStyle == BATTERY_STYLE_LANDSCAPEF    ||
                mBatteryStyle == BATTERY_STYLE_LANDSCAPEG    ||
                mBatteryStyle == BATTERY_STYLE_LANDSCAPEH    ||
                mBatteryStyle == BATTERY_STYLE_LANDSCAPEI    ||
                mBatteryStyle == BATTERY_STYLE_LANDSCAPEJ    ||
                mBatteryStyle == BATTERY_STYLE_LANDSCAPEK    ||
                mBatteryStyle == BATTERY_STYLE_LANDSCAPEL    ||
                mBatteryStyle == BATTERY_STYLE_LANDSCAPEM    ||
                mBatteryStyle == BATTERY_STYLE_LANDSCAPEN    ||
                mBatteryStyle == BATTERY_STYLE_LANDSCAPEO    ||
                mBatteryStyle == BATTERY_STYLE_PORTRAIT      ||
                mBatteryStyle == BATTERY_STYLE_CIRCLE        ||
                mBatteryStyle == BATTERY_STYLE_FULL_CIRCLE   ||
                mBatteryStyle == BATTERY_STYLE_DOTTED_CIRCLE ||
                mBatteryStyle == BATTERY_STYLE_BIG_CIRCLE    ||
                mBatteryStyle == BATTERY_STYLE_BIG_DOTTED_CIRCLE ||
                mBatteryStyle == BATTERY_STYLE_LANDSCAPE     ||
                mBatteryStyle == BATTERY_STYLE_RLANDSCAPE) {
                LinearLayout.LayoutParams scaledLayoutParams = new LinearLayout.LayoutParams(
                    (scaleWidth), (scaleHeight));
                scaledLayoutParams.setMargins(marginLeft,
                                              marginTop,
                                              marginRight,
                                              marginBottom);
                mBatteryIconView.setLayoutParams(scaledLayoutParams);
            } else {
                LinearLayout.LayoutParams scaledLayoutParams = new LinearLayout.LayoutParams(
                    (defaultWidth), (defaultHeight));
                scaledLayoutParams.setMargins(0, 0, 0, defaultMarginBottom);
                mBatteryIconView.setLayoutParams(scaledLayoutParams);
            }
        } else {
            if (mBatteryStyle == BATTERY_STYLE_LANDSCAPEA) {
                int batteryWidth = res.getDimensionPixelSize(R.dimen.status_bar_battery_icon_width_landscape_a);
                int batteryHeight = res.getDimensionPixelSize(R.dimen.status_bar_battery_icon_height_landscape_a);
                LinearLayout.LayoutParams scaledLayoutParams = new LinearLayout.LayoutParams(
                    (int) (batteryWidth * iconScaleFactor), (int) (batteryHeight * iconScaleFactor));
                scaledLayoutParams.setMargins(0, 0, 0, defaultMarginBottom);
                mBatteryIconView.setLayoutParams(scaledLayoutParams);
            } else if (mBatteryStyle == BATTERY_STYLE_LANDSCAPEB) {
                int batteryWidth = res.getDimensionPixelSize(R.dimen.status_bar_battery_icon_width_landscape_b);
                int batteryHeight = res.getDimensionPixelSize(R.dimen.status_bar_battery_icon_height_landscape_b);
                LinearLayout.LayoutParams scaledLayoutParams = new LinearLayout.LayoutParams(
                    (int) (batteryWidth * iconScaleFactor), (int) (batteryHeight * iconScaleFactor));
                scaledLayoutParams.setMargins(0, 0, 0, defaultMarginBottom);
                mBatteryIconView.setLayoutParams(scaledLayoutParams);
            } else if (mBatteryStyle == BATTERY_STYLE_LANDSCAPEC) {
                int batteryWidth = res.getDimensionPixelSize(R.dimen.status_bar_battery_icon_width_landscape_c);
                int batteryHeight = res.getDimensionPixelSize(R.dimen.status_bar_battery_icon_height_landscape_c);
                LinearLayout.LayoutParams scaledLayoutParams = new LinearLayout.LayoutParams(
                    (int) (batteryWidth * iconScaleFactor), (int) (batteryHeight * iconScaleFactor));
                scaledLayoutParams.setMargins(0, 0, 0, defaultMarginBottom);
                mBatteryIconView.setLayoutParams(scaledLayoutParams);
            } else if (mBatteryStyle == BATTERY_STYLE_LANDSCAPED) {
                int batteryWidth = res.getDimensionPixelSize(R.dimen.status_bar_battery_icon_width_landscape_d);
                int batteryHeight = res.getDimensionPixelSize(R.dimen.status_bar_battery_icon_height_landscape_d);
                LinearLayout.LayoutParams scaledLayoutParams = new LinearLayout.LayoutParams(
                    (int) (batteryWidth * iconScaleFactor), (int) (batteryHeight * iconScaleFactor));
                scaledLayoutParams.setMargins(0, 0, 0, defaultMarginBottom);
                mBatteryIconView.setLayoutParams(scaledLayoutParams);
            } else if (mBatteryStyle == BATTERY_STYLE_LANDSCAPEE) {
                int batteryWidth = res.getDimensionPixelSize(R.dimen.status_bar_battery_icon_width_landscape_e);
                int batteryHeight = res.getDimensionPixelSize(R.dimen.status_bar_battery_icon_height_landscape_e);
                LinearLayout.LayoutParams scaledLayoutParams = new LinearLayout.LayoutParams(
                    (int) (batteryWidth * iconScaleFactor), (int) (batteryHeight * iconScaleFactor));
                scaledLayoutParams.setMargins(0, 0, 0, defaultMarginBottom);
                mBatteryIconView.setLayoutParams(scaledLayoutParams);
            } else if (mBatteryStyle == BATTERY_STYLE_LANDSCAPEF) {
                int batteryWidth = res.getDimensionPixelSize(R.dimen.status_bar_battery_icon_width_landscape_f);
                int batteryHeight = res.getDimensionPixelSize(R.dimen.status_bar_battery_icon_height_landscape_f);
                LinearLayout.LayoutParams scaledLayoutParams = new LinearLayout.LayoutParams(
                    (int) (batteryWidth * iconScaleFactor), (int) (batteryHeight * iconScaleFactor));
                scaledLayoutParams.setMargins(0, 0, 0, defaultMarginBottom);
                mBatteryIconView.setLayoutParams(scaledLayoutParams);
            } else if (mBatteryStyle == BATTERY_STYLE_LANDSCAPEG) {
                int batteryWidth = res.getDimensionPixelSize(R.dimen.status_bar_battery_icon_width_landscape_g);
                int batteryHeight = res.getDimensionPixelSize(R.dimen.status_bar_battery_icon_height_landscape_g);
                LinearLayout.LayoutParams scaledLayoutParams = new LinearLayout.LayoutParams(
                    (int) (batteryWidth * iconScaleFactor), (int) (batteryHeight * iconScaleFactor));
                scaledLayoutParams.setMargins(0, 0, 0, defaultMarginBottom);
                mBatteryIconView.setLayoutParams(scaledLayoutParams);
            } else if (mBatteryStyle == BATTERY_STYLE_LANDSCAPEH) {
                int batteryWidth = res.getDimensionPixelSize(R.dimen.status_bar_battery_icon_width_landscape_h);
                int batteryHeight = res.getDimensionPixelSize(R.dimen.status_bar_battery_icon_height_landscape_h);
                LinearLayout.LayoutParams scaledLayoutParams = new LinearLayout.LayoutParams(
                    (int) (batteryWidth * iconScaleFactor), (int) (batteryHeight * iconScaleFactor));
                scaledLayoutParams.setMargins(0, 0, 0, defaultMarginBottom);
                mBatteryIconView.setLayoutParams(scaledLayoutParams);
            } else if (mBatteryStyle == BATTERY_STYLE_LANDSCAPEI) {
                int batteryWidth = res.getDimensionPixelSize(R.dimen.status_bar_battery_icon_width_landscape_i);
                int batteryHeight = res.getDimensionPixelSize(R.dimen.status_bar_battery_icon_height_landscape_i);
                LinearLayout.LayoutParams scaledLayoutParams = new LinearLayout.LayoutParams(
                    (int) (batteryWidth * iconScaleFactor), (int) (batteryHeight * iconScaleFactor));
                scaledLayoutParams.setMargins(0, 0, 0, defaultMarginBottom);
                mBatteryIconView.setLayoutParams(scaledLayoutParams);
            } else if (mBatteryStyle == BATTERY_STYLE_LANDSCAPEJ) {
                int batteryWidth = res.getDimensionPixelSize(R.dimen.status_bar_battery_icon_width_landscape_j);
                int batteryHeight = res.getDimensionPixelSize(R.dimen.status_bar_battery_icon_height_landscape_j);
                LinearLayout.LayoutParams scaledLayoutParams = new LinearLayout.LayoutParams(
                    (int) (batteryWidth * iconScaleFactor), (int) (batteryHeight * iconScaleFactor));
                scaledLayoutParams.setMargins(0, 0, 0, defaultMarginBottom);
                mBatteryIconView.setLayoutParams(scaledLayoutParams);
            } else if (mBatteryStyle == BATTERY_STYLE_LANDSCAPEK) {
                int batteryWidth = res.getDimensionPixelSize(R.dimen.status_bar_battery_icon_width_landscape_k);
                int batteryHeight = res.getDimensionPixelSize(R.dimen.status_bar_battery_icon_height_landscape_k);
                LinearLayout.LayoutParams scaledLayoutParams = new LinearLayout.LayoutParams(
                    (int) (batteryWidth * iconScaleFactor), (int) (batteryHeight * iconScaleFactor));
                scaledLayoutParams.setMargins(0, 0, 0, defaultMarginBottom);
                mBatteryIconView.setLayoutParams(scaledLayoutParams);
            } else if (mBatteryStyle == BATTERY_STYLE_LANDSCAPEL) {
                int batteryWidth = res.getDimensionPixelSize(R.dimen.status_bar_battery_icon_width_landscape_l);
                int batteryHeight = res.getDimensionPixelSize(R.dimen.status_bar_battery_icon_height_landscape_l);
                LinearLayout.LayoutParams scaledLayoutParams = new LinearLayout.LayoutParams(
                    (int) (batteryWidth * iconScaleFactor), (int) (batteryHeight * iconScaleFactor));
                scaledLayoutParams.setMargins(0, 0, 0, defaultMarginBottom);
                mBatteryIconView.setLayoutParams(scaledLayoutParams);
            } else if (mBatteryStyle == BATTERY_STYLE_LANDSCAPEM) {
                int batteryWidth = res.getDimensionPixelSize(R.dimen.status_bar_battery_icon_width_landscape_m);
                int batteryHeight = res.getDimensionPixelSize(R.dimen.status_bar_battery_icon_height_landscape_m);
                LinearLayout.LayoutParams scaledLayoutParams = new LinearLayout.LayoutParams(
                    (int) (batteryWidth * iconScaleFactor), (int) (batteryHeight * iconScaleFactor));
                scaledLayoutParams.setMargins(0, 0, 0, defaultMarginBottom);
                mBatteryIconView.setLayoutParams(scaledLayoutParams);
            } else if (mBatteryStyle == BATTERY_STYLE_LANDSCAPEN) {
                int batteryWidth = res.getDimensionPixelSize(R.dimen.status_bar_battery_icon_width_landscape_n);
                int batteryHeight = res.getDimensionPixelSize(R.dimen.status_bar_battery_icon_height_landscape_n);
                LinearLayout.LayoutParams scaledLayoutParams = new LinearLayout.LayoutParams(
                    (int) (batteryWidth * iconScaleFactor), (int) (batteryHeight * iconScaleFactor));
                scaledLayoutParams.setMargins(0, 0, 0, defaultMarginBottom);
                mBatteryIconView.setLayoutParams(scaledLayoutParams);
            } else if (mBatteryStyle == BATTERY_STYLE_LANDSCAPEO) {
                int batteryWidth = res.getDimensionPixelSize(R.dimen.status_bar_battery_icon_width_landscape_o);
                int batteryHeight = res.getDimensionPixelSize(R.dimen.status_bar_battery_icon_height_landscape_o);
                LinearLayout.LayoutParams scaledLayoutParams = new LinearLayout.LayoutParams(
                    (int) (batteryWidth * iconScaleFactor), (int) (batteryHeight * iconScaleFactor));
                scaledLayoutParams.setMargins(0, 0, 0, defaultMarginBottom);
                mBatteryIconView.setLayoutParams(scaledLayoutParams);
            } else if (mBatteryStyle == BATTERY_STYLE_CIRCLE || mBatteryStyle == BATTERY_STYLE_DOTTED_CIRCLE || mBatteryStyle == BATTERY_STYLE_FULL_CIRCLE || mBatteryStyle == BATTERY_STYLE_BIG_CIRCLE || mBatteryStyle == BATTERY_STYLE_BIG_DOTTED_CIRCLE) {
                int batteryWidth = res.getDimensionPixelSize(R.dimen.status_bar_battery_icon_circle_width);
                int batteryHeight = res.getDimensionPixelSize(R.dimen.status_bar_battery_icon_height);
                LinearLayout.LayoutParams scaledLayoutParams = new LinearLayout.LayoutParams(
                    (int) (batteryWidth * iconScaleFactor), (int) (batteryHeight * iconScaleFactor));
                scaledLayoutParams.setMargins(0, 0, 0, defaultMarginBottom);
                mBatteryIconView.setLayoutParams(scaledLayoutParams);
            }
        }

        boolean displayShield = mDisplayShieldEnabled && mIsBatteryDefender;
        float fullBatteryIconHeight =
                BatterySpecs.getFullBatteryHeight(mainBatteryHeight, displayShield);
        float fullBatteryIconWidth =
                BatterySpecs.getFullBatteryWidth(mainBatteryWidth, displayShield);

        if (displayShield) {
            // If the shield is displayed, we need some extra marginTop so that the bottom of the
            // main icon is still aligned with the bottom of all the other system icons.
            int shieldHeightAddition = Math.round(fullBatteryIconHeight - mainBatteryHeight);
            // However, the other system icons have some embedded bottom padding that the battery
            // doesn't have, so we shouldn't move the battery icon down by the full amount.
            // See b/258672854.
            marginTop = shieldHeightAddition
                    - res.getDimensionPixelSize(R.dimen.status_bar_battery_extra_vertical_spacing);
        } else {
            marginTop = 0;
        }
    }

    private void updateDrawable() {
        switch (mBatteryStyle) {
            case BATTERY_STYLE_PORTRAIT:
                mBatteryIconView.setImageDrawable(mAccessorizedDrawable);
                break;
            case BATTERY_STYLE_RLANDSCAPE:
                mBatteryIconView.setImageDrawable(mRLandscapeDrawable);
                break;
            case BATTERY_STYLE_LANDSCAPE:
                mBatteryIconView.setImageDrawable(mLandscapeDrawable);
                break;
            case BATTERY_STYLE_FULL_CIRCLE:
                mBatteryIconView.setImageDrawable(mFullCircleDrawable);
                break;
            case BATTERY_STYLE_CIRCLE:
            case BATTERY_STYLE_DOTTED_CIRCLE:
            case BATTERY_STYLE_BIG_CIRCLE:
            case BATTERY_STYLE_BIG_DOTTED_CIRCLE:
                mCircleDrawable.setMeterStyle(mBatteryStyle);
                mBatteryIconView.setImageDrawable(mCircleDrawable);
                break;
            case BATTERY_STYLE_LANDSCAPEA:
                mBatteryIconView.setImageDrawable(mLandscapeBatteryA);
                mBatteryIconView.setVisibility(View.VISIBLE);
                break;
            case BATTERY_STYLE_LANDSCAPEB:
                mBatteryIconView.setImageDrawable(mLandscapeBatteryB);
                mBatteryIconView.setVisibility(View.VISIBLE);
                break;
            case BATTERY_STYLE_LANDSCAPEC:
                mBatteryIconView.setImageDrawable(mLandscapeBatteryC);
                mBatteryIconView.setVisibility(View.VISIBLE);
                break;
            case BATTERY_STYLE_LANDSCAPED:
                mBatteryIconView.setImageDrawable(mLandscapeBatteryD);
                mBatteryIconView.setVisibility(View.VISIBLE);
                break;
            case BATTERY_STYLE_LANDSCAPEE:
                mBatteryIconView.setImageDrawable(mLandscapeBatteryE);
                mBatteryIconView.setVisibility(View.VISIBLE);
                break;
            case BATTERY_STYLE_LANDSCAPEF:
                mBatteryIconView.setImageDrawable(mLandscapeBatteryF);
                mBatteryIconView.setVisibility(View.VISIBLE);
                break;
            case BATTERY_STYLE_LANDSCAPEG:
                mBatteryIconView.setImageDrawable(mLandscapeBatteryG);
                mBatteryIconView.setVisibility(View.VISIBLE);
                break;
            case BATTERY_STYLE_LANDSCAPEH:
                mBatteryIconView.setImageDrawable(mLandscapeBatteryH);
                mBatteryIconView.setVisibility(View.VISIBLE);
                break;
            case BATTERY_STYLE_LANDSCAPEI:
                mBatteryIconView.setImageDrawable(mLandscapeBatteryI);
                mBatteryIconView.setVisibility(View.VISIBLE);
                break;
            case BATTERY_STYLE_LANDSCAPEJ:
                mBatteryIconView.setImageDrawable(mLandscapeBatteryJ);
                mBatteryIconView.setVisibility(View.VISIBLE);
                break;
            case BATTERY_STYLE_LANDSCAPEK:
                mBatteryIconView.setImageDrawable(mLandscapeBatteryK);
                break;
            case BATTERY_STYLE_LANDSCAPEL:
                mBatteryIconView.setImageDrawable(mLandscapeBatteryL);
                break;
            case BATTERY_STYLE_LANDSCAPEM:
                mBatteryIconView.setImageDrawable(mLandscapeBatteryM);
                break;
            case BATTERY_STYLE_LANDSCAPEN:
                mBatteryIconView.setImageDrawable(mLandscapeBatteryN);
                break;
            case BATTERY_STYLE_LANDSCAPEO:
                mBatteryIconView.setImageDrawable(mLandscapeBatteryO);
                break;
            case BATTERY_STYLE_HIDDEN:
            case BATTERY_STYLE_TEXT:
                return;
            default:
        }
    }

    @Override
    public void onDarkChanged(ArrayList<Rect> areas, float darkIntensity, int tint) {
        if (mIsStaticColor) return;
        float intensity = DarkIconDispatcher.isInAreas(areas, this) ? darkIntensity : 0;
        int nonAdaptedSingleToneColor = mDualToneHandler.getSingleColor(intensity);
        int nonAdaptedForegroundColor = mDualToneHandler.getFillColor(intensity);
        int nonAdaptedBackgroundColor = mDualToneHandler.getBackgroundColor(intensity);

        updateColors(nonAdaptedForegroundColor, nonAdaptedBackgroundColor,
                nonAdaptedSingleToneColor);
    }

    public void setStaticColor(boolean isStaticColor) {
        mIsStaticColor = isStaticColor;
    }

    /**
     * Sets icon and text colors. This will be overridden by {@code onDarkChanged} events,
     * if registered.
     *
     * @param foregroundColor
     * @param backgroundColor
     * @param singleToneColor
     */
    public void updateColors(int foregroundColor, int backgroundColor, int singleToneColor) {
        mAccessorizedDrawable.setColors(foregroundColor, backgroundColor, singleToneColor);
        mCircleDrawable.setColors(foregroundColor, backgroundColor, singleToneColor);
        mFullCircleDrawable.setColors(foregroundColor, backgroundColor, singleToneColor);
        mRLandscapeDrawable.setColors(foregroundColor, backgroundColor, singleToneColor);
        mLandscapeDrawable.setColors(foregroundColor, backgroundColor, singleToneColor);
        mThemedDrawable.setColors(foregroundColor, backgroundColor, singleToneColor);
        mLandscapeBatteryA.setColors(foregroundColor, backgroundColor, singleToneColor);
        mLandscapeBatteryB.setColors(foregroundColor, backgroundColor, singleToneColor);
        mLandscapeBatteryC.setColors(foregroundColor, backgroundColor, singleToneColor);
        mLandscapeBatteryD.setColors(foregroundColor, backgroundColor, singleToneColor);
        mLandscapeBatteryE.setColors(foregroundColor, backgroundColor, singleToneColor);
        mLandscapeBatteryF.setColors(foregroundColor, backgroundColor, singleToneColor);
        mLandscapeBatteryG.setColors(foregroundColor, backgroundColor, singleToneColor);
        mLandscapeBatteryH.setColors(foregroundColor, backgroundColor, singleToneColor);
        mLandscapeBatteryI.setColors(foregroundColor, backgroundColor, singleToneColor);
        mLandscapeBatteryJ.setColors(foregroundColor, backgroundColor, singleToneColor);
        mLandscapeBatteryK.setColors(foregroundColor, backgroundColor, singleToneColor);
        mLandscapeBatteryL.setColors(foregroundColor, backgroundColor, singleToneColor);
        mLandscapeBatteryM.setColors(foregroundColor, backgroundColor, singleToneColor);
        mLandscapeBatteryN.setColors(foregroundColor, backgroundColor, singleToneColor);
        mLandscapeBatteryO.setColors(foregroundColor, backgroundColor, singleToneColor);
        mTextColor = singleToneColor;
        if (mBatteryPercentView != null) {
            mBatteryPercentView.setTextColor(singleToneColor);
        }

        if (mUnknownStateDrawable != null) {
            mUnknownStateDrawable.setTint(singleToneColor);
        }
        
        if (mChargingIconView != null) {
            mChargingIconView.setImageTintList(android.content.res.ColorStateList.valueOf(singleToneColor));
        }
    }

    private boolean isCharging() {
        return mPluggedIn && !mIsIncompatibleCharging;
    }

    public void dump(PrintWriter pw, String[] args) {
        String powerSave = mAccessorizedDrawable == null ? null : mAccessorizedDrawable.getPowerSaveEnabled() + "";
        String displayShield = mAccessorizedDrawable == null ? null : mAccessorizedDrawable.getDisplayShield() + "";
        String charging = mAccessorizedDrawable == null ? null : mAccessorizedDrawable.getCharging() + "";
        CharSequence percent = mBatteryPercentView == null ? null : mBatteryPercentView.getText();
        pw.println("  BatteryMeterView:");
        pw.println("    getPowerSave: " + powerSave);
        pw.println("    mAccessorizedDrawable.getDisplayShield: " + displayShield);
        pw.println("    mAccessorizedDrawable.getCharging: " + charging);
        pw.println("    mBatteryPercentView.getText(): " + percent);
        pw.println("    mTextColor: #" + Integer.toHexString(mTextColor));
        pw.println("    mBatteryStateUnknown: " + mBatteryStateUnknown);
        pw.println("    mIsIncompatibleCharging: " + mIsIncompatibleCharging);
        pw.println("    mPluggedIn: " + mPluggedIn);
        pw.println("    mLevel: " + mLevel);
        pw.println("    mMode: " + mShowPercentMode);
    }

    @VisibleForTesting
    CharSequence getBatteryPercentViewText() {
        return mBatteryPercentView.getText();
    }

    /** An interface that will fetch the estimated time remaining for the user's battery. */
    public interface BatteryEstimateFetcher {
        void fetchBatteryTimeRemainingEstimate(
                BatteryController.EstimateFetchCompletion completion);
    }

    public interface BatteryMeterViewCallbacks {
        default void onHiddenBattery(boolean hidden) {}
    }

    public void addCallback(BatteryMeterViewCallbacks callback) {
        mCallbacks.add(callback);
    }

    public void removeCallback(BatteryMeterViewCallbacks callbacks) {
        mCallbacks.remove(callbacks);
    }
}

