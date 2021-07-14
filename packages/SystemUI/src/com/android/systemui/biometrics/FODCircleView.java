/**
 * Copyright (C) 2019 The Android Open Source Project
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

package com.android.systemui.biometrics;

import static android.app.WindowConfiguration.ACTIVITY_TYPE_ASSISTANT;
import static android.app.WindowConfiguration.WINDOWING_MODE_UNDEFINED;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.annotation.SuppressLint;
import android.app.ActivityManager.StackInfo;
import android.app.ActivityTaskManager;
import android.app.admin.DevicePolicyManager;
import android.app.WallpaperColors;
import android.app.WallpaperManager;
import android.content.ContentResolver;
import android.content.Context;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.database.ContentObserver;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.Point;
import android.hardware.biometrics.BiometricSourceType;
import android.graphics.PorterDuff;
import android.graphics.drawable.AnimationDrawable;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Handler;
import android.os.UserHandle;
import android.os.Looper;
import android.os.RemoteException;
import android.os.UserHandle;
import android.provider.Settings;
import android.util.DisplayMetrics;
import android.util.Spline;
import android.view.Display;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.Surface;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;
import androidx.palette.graphics.Palette;

import com.android.internal.widget.LockPatternUtils;
import com.android.keyguard.KeyguardUpdateMonitor;
import com.android.keyguard.KeyguardUpdateMonitorCallback;
import com.android.settingslib.utils.ThreadUtils;
import com.android.systemui.Dependency;
import com.android.systemui.keyguard.ScreenLifecycle;
import com.android.systemui.keyguard.WakefulnessLifecycle;
import com.android.systemui.R;
import com.android.systemui.shared.system.ActivityManagerWrapper;
import com.android.systemui.shared.system.TaskStackChangeListener;
import com.android.systemui.statusbar.policy.ConfigurationController;
import com.android.systemui.statusbar.policy.ConfigurationController.ConfigurationListener;

import vendor.lineage.biometrics.fingerprint.inscreen.V1_0.IFingerprintInscreen;
import vendor.lineage.biometrics.fingerprint.inscreen.V1_0.IFingerprintInscreenCallback;

import java.util.NoSuchElementException;
import java.util.Timer;
import java.util.TimerTask;

public class FODCircleView extends ImageView implements ConfigurationListener {
    private static final int FADE_ANIM_DURATION = 125;
    private static final String SCREEN_BRIGHTNESS = Settings.System.SCREEN_BRIGHTNESS;
    private final int mPositionX;
    private final int mPositionY;
    private final int mSize;
    private final int mDreamingMaxOffset;
    private final int mNavigationBarSize;
    private final boolean mHideFodCircleGoingToSleep;
    private final boolean mShouldBoostBrightness;
    private final boolean mTargetUsesInKernelDimming;
    private final Paint mPaintFingerprintBackground = new Paint();
    private final Paint mPaintFingerprint = new Paint();
    private final WindowManager.LayoutParams mParams = new WindowManager.LayoutParams();
    private final WindowManager.LayoutParams mPressedParams = new WindowManager.LayoutParams();
    private final WindowManager mWindowManager;

    private IFingerprintInscreen mFingerprintInscreenDaemon;
    private final Context mContext;

    private int mCurrentBrightness;
    private int mDreamingOffsetY;
    private int mColorBackground;

    private boolean mFading;
    private boolean mIsBouncer;
    private boolean mIsBiometricRunning;
    private boolean mIsCircleShowing;
    private boolean mIsDreaming;
    private boolean mIsKeyguard;
    private boolean mTouchedOutside;
    private boolean mIsScreenTurnedOn;
    private boolean mIsAnimating = false;
    private boolean mIsAssistantVisible = false;

    private final Handler mHandler;

    private final ImageView mPressedView;

    private final LockPatternUtils mLockPatternUtils;

    private Timer mBurnInProtectionTimer;
    private WallpaperManager mWallManager;
    private int iconcolor = 0xFF3980FF;

    private final Spline mFODiconBrightnessToDimAmountSpline;


    private int mSelectedIcon;
    private final int[] ICON_STYLES = {
        R.drawable.fod_icon_default,
        R.drawable.fod_icon_default_1,
        R.drawable.fod_icon_candy_2,
        R.drawable.fod_icon_default_2,
        R.drawable.fod_icon_default_3,
        R.drawable.fod_icon_default_4,
        R.drawable.fod_icon_default_5,
        R.drawable.fod_icon_arc_reactor,
        R.drawable.fod_icon_cpt_america_flat,
        R.drawable.fod_icon_broncos,
        R.drawable.fod_icon_cowboys,
        R.drawable.fod_icon_patriots,
        R.drawable.fod_icon_bruins,
        R.drawable.fod_icon_bucs,
        R.drawable.fod_icon_dragon_black_flat,
        R.drawable.fod_icon_future,
        R.drawable.fod_icon_glow_circle,
        R.drawable.fod_icon_paint_splash_circle,
        R.drawable.fod_icon_rainbow_horn,
        R.drawable.fod_icon_shooky,
        R.drawable.fod_icon_spiral_blue,
        R.drawable.fod_icon_default_1
    };

    private int mDefaultPressedColor;
    private int mPressedColor;
    private final int[] PRESSED_COLOR = {
        R.drawable.fod_icon_pressed,
        R.drawable.fod_icon_pressed_cyan,
        R.drawable.fod_icon_pressed_green,
        R.drawable.fod_icon_pressed_yellow
    };

    private final IFingerprintInscreenCallback mFingerprintInscreenCallback =
            new IFingerprintInscreenCallback.Stub() {
        @Override
        public void onFingerDown() {
            if (mUpdateMonitor.userNeedsStrongAuth()) {
                // Keyguard requires strong authentication (not biometrics)
                return;
            }

            if (!mUpdateMonitor.isScreenOn()) {
                // Keyguard is shown just after screen turning off
                return;
            }

            if (mIsBouncer && !isPinOrPattern(mUpdateMonitor.getCurrentUser())) {
                // Ignore show calls when Keyguard password screen is being shown
                return;
            }

            if (mIsKeyguard && mUpdateMonitor.getUserCanSkipBouncer(mUpdateMonitor.getCurrentUser())) {
                // Ignore show calls if user can skip bouncer
                return;
            }

            if (mIsKeyguard && !mIsBiometricRunning) {
                return;
            }
            mHandler.post(() -> showCircle());
        }

        @Override
        public void onFingerUp() {
            mHandler.post(() -> hideCircle());
        }
    };

    private final KeyguardUpdateMonitor mUpdateMonitor;
    private final KeyguardUpdateMonitorCallback mMonitorCallback = new KeyguardUpdateMonitorCallback() {
        @Override
        public void onBiometricAuthenticated(int userId, BiometricSourceType biometricSourceType,
                boolean isStrongBiometric) {
            // We assume that if biometricSourceType matches Fingerprint it will be
            // handled here, so we hide only when other biometric types authenticate
            if (biometricSourceType != BiometricSourceType.FINGERPRINT) {
                hide();
            }
        }

        @Override
        public void onBiometricRunningStateChanged(boolean running,
                BiometricSourceType biometricSourceType) {
            if (biometricSourceType == BiometricSourceType.FINGERPRINT) {
                mIsBiometricRunning = running;
            }
        }

        @Override
        public void onDreamingStateChanged(boolean dreaming) {
            mIsDreaming = dreaming;
            updateIconDim(false);

            if (mIsKeyguard && mUpdateMonitor.isFingerprintDetectionRunning()) {
                show();
                updateIconDim(false);
            } else {
                hide();
            }

            if (dreaming) {
                if (shouldShowOnDoze()) {
                    mBurnInProtectionTimer = new Timer();
                    mBurnInProtectionTimer.schedule(new BurnInProtectionTask(), 0, 60 * 1000);
                } else {
                    setImageDrawable(null);
                    invalidate();
                }
            } else {
                if (mBurnInProtectionTimer != null) {
                    mBurnInProtectionTimer.cancel();
                    mBurnInProtectionTimer = null;
                    updatePosition();
                }
                if (!shouldShowOnDoze()) {
                    setImageResource(R.drawable.fod_icon_default);
                    invalidate();
                }
            }
        }

        @Override
        public void onKeyguardVisibilityChanged(boolean showing) {
            mIsKeyguard = showing;
            if (!showing) {
                hide();
            } else {
                updateIconDim(false);
            }
        }

        @Override
        public void onKeyguardBouncerChanged(boolean isBouncer) {
            mIsBouncer = isBouncer;
            if (mUpdateMonitor.isFingerprintDetectionRunning() && !mUpdateMonitor.userNeedsStrongAuth()) {
                if (isPinOrPattern(KeyguardUpdateMonitor.getCurrentUser()) || !isBouncer) {
                    mIsAssistantVisible = false;
                    show();
                } else {
                    hide();
                }
            } else {
                hide();
            }
        }
    };

    private final ScreenLifecycle mScreenMonitor;
    private final ScreenLifecycle.Observer mScreenObserver = new ScreenLifecycle.Observer() {

        @Override
        public void onScreenTurnedOn() {
            mIsScreenTurnedOn = true;
            if (mUpdateMonitor.isFingerprintDetectionRunning()) {
                show();
            }
        }

        @Override
        public void onScreenTurningOff() {
            hide();
        }

        @Override
        public void onScreenTurnedOff() {
            mIsScreenTurnedOn = false;
        }

    };

    private final WakefulnessLifecycle mWakefulnessMonitor;
    private final WakefulnessLifecycle.Observer mWakefulnessObserver = new WakefulnessLifecycle.Observer() {
        @Override
        public void onStartedWakingUp() {
            if (!mIsScreenTurnedOn &&
                    mUpdateMonitor.isFingerprintDetectionRunning()) {
                show();
            }
        }

        @Override
        public void onFinishedGoingToSleep() {
            updateIconDim(true);
        }
    };

    private final TaskStackChangeListener
            mTaskStackChangeListener = new TaskStackChangeListener() {
        @Override
        public void onTaskStackChangedBackground() {
            try {
                StackInfo stackInfo = ActivityTaskManager.getService().getStackInfo(
                        WINDOWING_MODE_UNDEFINED, ACTIVITY_TYPE_ASSISTANT);
                if (stackInfo == null && mIsAssistantVisible) {
                        mIsAssistantVisible = false;
                        if (mUpdateMonitor.isFingerprintDetectionRunning()) {
                            mHandler.post(() -> show());
                    }
                    return;
                }
                if (stackInfo != null) mIsAssistantVisible = stackInfo.visible;
                if (mIsAssistantVisible) {
                    mHandler.post(() -> hide());
                }
            } catch (RemoteException ignored) { }
        }
    };

    private class CustomSettingsObserver extends ContentObserver {

        CustomSettingsObserver(Handler handler) {
            super(handler);
        }

        void observe() {
            ContentResolver resolver = mContext.getContentResolver();
            resolver.registerContentObserver(Settings.System.getUriFor(
                    SCREEN_BRIGHTNESS), false, this, UserHandle.USER_ALL);
            resolver.registerContentObserver(Settings.System.getUriFor(
                    Settings.System.FOD_COLOR),
                    false, this, UserHandle.USER_ALL);
        }

        void unobserve() {
            mContext.getContentResolver().unregisterContentObserver(this);
        }

        @Override
        public void onChange(boolean selfChange, Uri uri) {
            if (uri.equals(Settings.System.getUriFor(Settings.System.FOD_COLOR))) {
                updateStyle();
            } if (uri.equals(Settings.System.getUriFor(SCREEN_BRIGHTNESS))) {
                update();
            }
        }

        public void update() {
            int brightness = Settings.System.getInt(
                    mContext.getContentResolver(), SCREEN_BRIGHTNESS, 100);
            if (mCurrentBrightness != brightness) {
                mCurrentBrightness = brightness;
                updateIconDim(false);
            }
            updateStyle();
        }
    }

    private boolean mCutoutMasked;
    private int mStatusbarHeight;

    private final CustomSettingsObserver mCustomSettingsObserver;

    @SuppressLint("RtlHardcoded")
    public FODCircleView(Context context) {
        super(context);
        mContext = context;

        IFingerprintInscreen daemon = getFingerprintInScreenDaemon();
        if (daemon == null) {
            throw new RuntimeException("Unable to get IFingerprintInscreen");
        }

        try {
            mShouldBoostBrightness = daemon.shouldBoostBrightness();
            mPositionX = daemon.getPositionX();
            mPositionY = daemon.getPositionY();
            mSize = daemon.getSize();
        } catch (RemoteException e) {
            throw new RuntimeException("Failed to retrieve FOD circle position or size");
        }

        Resources res = context.getResources();

        mPaintFingerprint.setColor(res.getColor(R.color.config_fodColor, null));
        mPaintFingerprint.setAntiAlias(true);
        mPaintFingerprintBackground.setColor(res.getColor(R.color.config_fodColorBackground, null));
        mPaintFingerprintBackground.setAntiAlias(true);

        float[] icon_dim_amount =
                getFloatArray(res.obtainTypedArray(R.array.config_FODiconDimAmount));
        float[] display_brightness =
                getFloatArray(res.obtainTypedArray(R.array.config_FODiconDisplayBrightness));
        mFODiconBrightnessToDimAmountSpline =
                Spline.createSpline(display_brightness, icon_dim_amount);

        mTargetUsesInKernelDimming = res.getBoolean(com.android.internal.R.bool.config_targetUsesInKernelDimming);

        mHideFodCircleGoingToSleep = mContext.getResources().getBoolean(
                com.android.internal.R.bool.config_hideFodCircleGoingToSleep);

        mWindowManager = context.getSystemService(WindowManager.class);

        mNavigationBarSize = res.getDimensionPixelSize(R.dimen.navigation_bar_size);

        mDreamingMaxOffset = (int) (mSize * 0.1f);

        mHandler = new Handler(Looper.getMainLooper());

        mCustomSettingsObserver = new CustomSettingsObserver(mHandler);
        mCustomSettingsObserver.update();

        mParams.height = mSize;
        mParams.width = mSize;
        mParams.format = PixelFormat.TRANSLUCENT;

        mParams.packageName = "android";
        mParams.type = WindowManager.LayoutParams.TYPE_DISPLAY_OVERLAY;
        mParams.flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE |
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN |
                WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED |
                WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH;
        mParams.gravity = Gravity.TOP | Gravity.LEFT;

        mPressedParams.copyFrom(mParams);
        mPressedParams.flags |= WindowManager.LayoutParams.FLAG_DIM_BEHIND |
                WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH;

        mParams.setTitle("Fingerprint on display");
        mPressedParams.setTitle("Fingerprint on display.touched");

        mPressedView = new ImageView(context)  {
            @Override
            protected void onDraw(Canvas canvas) {
                if (mIsCircleShowing) {
                    setImageResource(PRESSED_COLOR[mPressedColor]);
                }
                super.onDraw(canvas);
            }
        };

        mWindowManager.addView(this, mParams);

        updatePosition();
        hide();

        mLockPatternUtils = new LockPatternUtils(mContext);

        mWakefulnessMonitor = Dependency.get(WakefulnessLifecycle.class);
        mScreenMonitor = Dependency.get(ScreenLifecycle.class);
        mUpdateMonitor = Dependency.get(KeyguardUpdateMonitor.class);

        updateCutoutFlags();
        Dependency.get(ConfigurationController.class).addCallback(this);
    }


    private int getDimAlpha() {
        return Math.round(mFODiconBrightnessToDimAmountSpline.interpolate(mCurrentBrightness));
    }

    public void updateIconDim(boolean animate) {
        if (!mIsCircleShowing && mTargetUsesInKernelDimming) {
            if (animate && !mIsAnimating) {
                ValueAnimator anim = new ValueAnimator();
                anim.setIntValues(0, getDimAlpha());
                anim.addUpdateListener(valueAnimator -> {
                    int progress = (Integer) valueAnimator.getAnimatedValue();
                    setColorFilter(Color.argb(progress, 0, 0, 0),
                            PorterDuff.Mode.SRC_ATOP);
                });
                anim.addListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        mIsAnimating = false;
                    }
                });
                anim.setDuration(250);
                mIsAnimating = true;
                mHandler.post(anim::start);
            } else if (!mIsAnimating) {
                mHandler.post(() ->
                        setColorFilter(Color.argb(getDimAlpha(), 0, 0, 0),
                        PorterDuff.Mode.SRC_ATOP));
            }
        }
    }

    @Override
    protected void onAttachedToWindow() {
        mWakefulnessMonitor.addObserver(mWakefulnessObserver);
        mScreenMonitor.addObserver(mScreenObserver);
        mUpdateMonitor.registerCallback(mMonitorCallback);
        ActivityManagerWrapper.getInstance().registerTaskStackListener(
                mTaskStackChangeListener);
    }

    @Override
    protected void onDetachedFromWindow() {
        mWakefulnessMonitor.removeObserver(mWakefulnessObserver);
        mScreenMonitor.removeObserver(mScreenObserver);
        mUpdateMonitor.removeCallback(mMonitorCallback);
        ActivityManagerWrapper.getInstance().unregisterTaskStackListener(
                mTaskStackChangeListener);
        super.onDetachedFromWindow();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        if (!mIsCircleShowing) {
            canvas.drawCircle(mSize / 2, mSize / 2, mSize / 2.0f, mPaintFingerprintBackground);
        }
        super.onDraw(canvas);
    }

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        float x = event.getAxisValue(MotionEvent.AXIS_X);
        float y = event.getAxisValue(MotionEvent.AXIS_Y);

        boolean newIsInside = (x > 0 && x < mSize) && (y > 0 && y < mSize);
        mTouchedOutside = false;

        if (event.getAction() == MotionEvent.ACTION_OUTSIDE) {
            mTouchedOutside = true;
            return true;
        }

        if (event.getAction() == MotionEvent.ACTION_DOWN && newIsInside) {
            showCircle();
            return true;
        } else if (event.getAction() == MotionEvent.ACTION_UP) {
            hideCircle();
            return true;
        } else return event.getAction() == MotionEvent.ACTION_MOVE;
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        updatePosition();
    }

    public IFingerprintInscreen getFingerprintInScreenDaemon() {
        if (mFingerprintInscreenDaemon == null) {
            try {
                mFingerprintInscreenDaemon = IFingerprintInscreen.getService();
                if (mFingerprintInscreenDaemon != null) {
                    mFingerprintInscreenDaemon.setCallback(mFingerprintInscreenCallback);
                    mFingerprintInscreenDaemon.asBinder().linkToDeath((cookie) -> mFingerprintInscreenDaemon = null, 0);
                }
            } catch (NoSuchElementException | RemoteException e) {
                // do nothing
            }
        }
        return mFingerprintInscreenDaemon;
    }

    public void dispatchPress() {
        if (mFading) return;
        IFingerprintInscreen daemon = getFingerprintInScreenDaemon();
        try {
            daemon.onPress();
        } catch (RemoteException e) {
            // do nothing
        }
    }

    public void dispatchRelease() {
        IFingerprintInscreen daemon = getFingerprintInScreenDaemon();
        try {
            daemon.onRelease();
        } catch (RemoteException e) {
            // do nothing
        }
    }

    public void dispatchShow() {
        IFingerprintInscreen daemon = getFingerprintInScreenDaemon();
        try {
            daemon.onShowFODView();
        } catch (RemoteException e) {
            // do nothing
        }
    }

    public void dispatchHide() {
        IFingerprintInscreen daemon = getFingerprintInScreenDaemon();
        try {
            daemon.onHideFODView();
        } catch (RemoteException e) {
            // do nothing
        }
    }

    public void showCircle() {
        if (mFading || mTouchedOutside) return;

        mIsCircleShowing = true;

        setKeepScreenOn(true);

        setDim(true);
        ThreadUtils.postOnBackgroundThread(this::dispatchPress);

        mPressedView.setImageResource(R.drawable.fod_icon_pressed);

        setImageDrawable(null);
        updateIconDim(false);
        updatePosition();
        invalidate();
    }

    public void hideCircle() {
        mIsCircleShowing = false;

        if (mIsDreaming && !shouldShowOnDoze()) {
            setImageDrawable(null);
        } else {
            setImageResource(R.drawable.fod_icon_default);
        }
        invalidate();

        ThreadUtils.postOnBackgroundThread(this::dispatchRelease);

        setFODIcon();

        setDim(false);

        setKeepScreenOn(false);
    }

    private boolean useWallpaperColor() {
        return Settings.System.getInt(mContext.getContentResolver(),
                Settings.System.FOD_ICON_WALLPAPER_COLOR, 0) != 0;
    }

    private int getFODIcon() {
        return Settings.System.getInt(mContext.getContentResolver(),
                Settings.System.FOD_ICON, 2);
    }

    private void setFODIcon() {
        int fodicon = getFODIcon();

        if (fodicon == 0) {
            this.setImageResource(R.drawable.fod_icon_default);
        } else if (fodicon == 1) {
            this.setImageResource(R.drawable.fod_icon_default_1);
        } else if (fodicon == 2) {
            this.setImageResource(R.drawable.fod_icon_candy_2);
        } else if (fodicon == 3) {
            this.setImageResource(R.drawable.fod_icon_default_2);
        } else if (fodicon == 4) {
            this.setImageResource(R.drawable.fod_icon_default_3);
        } else if (fodicon == 5) {
            this.setImageResource(R.drawable.fod_icon_default_4);
        } else if (fodicon == 6) {
            this.setImageResource(R.drawable.fod_icon_default_5);
        } else if (fodicon == 7) {
            this.setImageResource(R.drawable.fod_icon_arc_reactor);
        } else if (fodicon == 8) {
            this.setImageResource(R.drawable.fod_icon_cpt_america_flat);
        } else if (fodicon == 9) {
            this.setImageResource(R.drawable.fod_icon_broncos);
        } else if (fodicon == 10) {
            this.setImageResource(R.drawable.fod_icon_cowboys);
        } else if (fodicon == 11) {
            this.setImageResource(R.drawable.fod_icon_patriots);
        } else if (fodicon == 12) {
            this.setImageResource(R.drawable.fod_icon_bruins);
        } else if (fodicon == 13) {
            this.setImageResource(R.drawable.fod_icon_bucs);
        } else if (fodicon == 14) {
            this.setImageResource(R.drawable.fod_icon_dragon_black_flat);
        } else if (fodicon == 15) {
            this.setImageResource(R.drawable.fod_icon_future);
        } else if (fodicon == 16) {
            this.setImageResource(R.drawable.fod_icon_glow_circle);
        } else if (fodicon == 17) {
            this.setImageResource(R.drawable.fod_icon_paint_splash_circle);
        } else if (fodicon == 18) {
            this.setImageResource(R.drawable.fod_icon_rainbow_horn);
        } else if (fodicon == 19) {
            this.setImageResource(R.drawable.fod_icon_shooky);
        } else if (fodicon == 20) {
            this.setImageResource(R.drawable.fod_icon_spiral_blue);
        } else if (fodicon == 21) {
            this.setImageResource(R.drawable.fod_icon_default_1);
        }

        if (useWallpaperColor()) {
            try {
                WallpaperManager wallpaperManager = WallpaperManager.getInstance(mContext);
                Drawable wallpaperDrawable = wallpaperManager.getDrawable();
                Bitmap bitmap = ((BitmapDrawable)wallpaperDrawable).getBitmap();
                if (bitmap != null) {
                    Palette p = Palette.from(bitmap).generate();
                    int wallColor = p.getDominantColor(iconcolor);
                    if (iconcolor != wallColor) {
                        iconcolor = wallColor;
                    }
                    this.setColorFilter(lighter(iconcolor, 3));
                }
            } catch (Exception e) {
                // Nothing to do
            }
        } else {
            this.setColorFilter(null);
        }
    }

    private static int lighter(int color, int factor) {
        int red = Color.red(color);
        int green = Color.green(color);
        int blue = Color.blue(color);

        blue = blue * factor;
        green = green * factor;
        blue = blue * factor;

        blue = blue > 255 ? 255 : blue;
        green = green > 255 ? 255 : green;
        red = red > 255 ? 255 : red;

        return Color.argb(Color.alpha(color), red, green, blue);	
    }

    public void show() {
        if (mUpdateMonitor.userNeedsStrongAuth()) {
            // Keyguard requires strong authentication (not biometrics)
            return;
        }

        if (!mUpdateMonitor.isScreenOn()) {
            // Keyguard is shown just after screen turning off
            return;
        }

        if (mIsBouncer && !isPinOrPattern(KeyguardUpdateMonitor.getCurrentUser())) {
            // Ignore show calls when Keyguard password screen is being shown
            return;
        }

        if (mIsKeyguard && mUpdateMonitor.getUserCanSkipBouncer(mUpdateMonitor.getCurrentUser())) {
            // Ignore show calls if user can skip bouncer
            return;
        }

        if (mIsKeyguard && !mIsBiometricRunning) {
            return;
        }

        if (mIsAssistantVisible) {
            // Don't show when assistant UI is visible
            return;
        }

        updatePosition();
        mCustomSettingsObserver.observe();
        mCustomSettingsObserver.update();

        setVisibility(View.VISIBLE);
        animate().withStartAction(() -> mFading = true)
                .alpha(mIsDreaming ? 0.5f : 1.0f)
                .setDuration(FADE_ANIM_DURATION)
                .withEndAction(() -> mFading = false)
                .start();
        ThreadUtils.postOnBackgroundThread(this::dispatchShow);
    }

    public void hide() {
        animate().withStartAction(() -> mFading = true)
                .alpha(0)
                .setDuration(FADE_ANIM_DURATION)
                .withEndAction(() -> {
                    setVisibility(View.GONE);
                    mFading = false;
                    dispatchHide();
                    hideCircle();
                })
                .start();
        mCustomSettingsObserver.unobserve();
        hideCircle();
        ThreadUtils.postOnBackgroundThread(this::dispatchHide);

    }

    public int getHeight(boolean includeDecor) {
        DisplayMetrics dm = new DisplayMetrics();
        if (includeDecor) {
            mWindowManager.getDefaultDisplay().getMetrics(dm);
        } else {
            mWindowManager.getDefaultDisplay().getRealMetrics(dm);
        }
        return dm.heightPixels - mPositionY + mSize / 2;
    }

    private void updateAlpha() {
        setAlpha(mIsDreaming ? 0.5f : 1.0f);
    }

    private void updateStyle() {
        mPressedColor = Settings.System.getInt(mContext.getContentResolver(),
                Settings.System.FOD_COLOR, mDefaultPressedColor);
    }

    @SuppressWarnings("SuspiciousNameCombination")
    private void updatePosition() {
        Display defaultDisplay = mContext.getDisplay();

        Point size = new Point();
        defaultDisplay.getRealSize(size);

        int rotation = defaultDisplay.getRotation();
        int cutoutMaskedExtra = mCutoutMasked ? mStatusbarHeight : 0;
        int x, y;
        switch (rotation) {
            case Surface.ROTATION_0:
                x = mPositionX;
                y = mPositionY - cutoutMaskedExtra;
                break;
            case Surface.ROTATION_90:
                x = mPositionY;
                y = mPositionX - cutoutMaskedExtra;
                break;
            case Surface.ROTATION_180:
                x = mPositionX;
                y = size.y - mPositionY - mSize - cutoutMaskedExtra;
                break;
            case Surface.ROTATION_270:
                x = size.x - mPositionY - mSize - mNavigationBarSize - cutoutMaskedExtra;
                y = mPositionX;
                break;
            default:
                throw new IllegalArgumentException("Unknown rotation: " + rotation);
        }

        mPressedParams.x = mParams.x = x;
        mPressedParams.y = mParams.y = y;

        if (mIsDreaming && !mIsCircleShowing) {
            mParams.y += mDreamingOffsetY;
        }

        mWindowManager.updateViewLayout(this, mParams);

        if (mPressedView.getParent() != null) {
            mWindowManager.updateViewLayout(mPressedView, mPressedParams);
        }
    }

    private void setDim(boolean dim) {
        if (dim) {
            int curBrightness = Settings.System.getInt(getContext().getContentResolver(),
                    Settings.System.SCREEN_BRIGHTNESS, 100);
            int dimAmount = 0;

            IFingerprintInscreen daemon = getFingerprintInScreenDaemon();
            try {
                dimAmount = daemon.getDimAmount(curBrightness);
            } catch (RemoteException e) {
                // do nothing
            }

            if (mShouldBoostBrightness) {
                mPressedParams.screenBrightness = 1.0f;
            }

            mPressedParams.dimAmount = dimAmount / 255.0f;
            if (mPressedView.getParent() == null) {
                mWindowManager.addView(mPressedView, mPressedParams);
            } else {
                mWindowManager.updateViewLayout(mPressedView, mPressedParams);
            }
        } else {
            if (mShouldBoostBrightness) {
                mPressedParams.screenBrightness = 0.0f;
            }
            mPressedParams.dimAmount = 0.0f;
            if (mPressedView.getParent() != null) {
                mWindowManager.removeView(mPressedView);
            }
            updateIconDim(true);
        }
    }

    private boolean isPinOrPattern(int userId) {
        int passwordQuality = mLockPatternUtils.getActivePasswordQuality(userId);
        switch (passwordQuality) {
            // PIN
            case DevicePolicyManager.PASSWORD_QUALITY_NUMERIC:
            case DevicePolicyManager.PASSWORD_QUALITY_NUMERIC_COMPLEX:
            // Pattern
            case DevicePolicyManager.PASSWORD_QUALITY_SOMETHING:
                return true;
        }

        return false;
    }

    private boolean shouldShowOnDoze() {
        return Settings.Secure.getInt(mContext.getContentResolver(),
                Settings.Secure.FOD_ON_DOZE, 1) == 1;
    }

    private class BurnInProtectionTask extends TimerTask {
        @Override
        public void run() {
            long now = System.currentTimeMillis() / 1000 / 60;

            // Let y to be not synchronized with x, so that we get maximum movement
            mDreamingOffsetY = (int) ((now + mDreamingMaxOffset / 3) % (mDreamingMaxOffset * 2));
            mDreamingOffsetY -= mDreamingMaxOffset;

            mHandler.post(FODCircleView.this::updatePosition);
        }
    };

    @Override
    public void onOverlayChanged() {
        updateCutoutFlags();
    }

    private void updateCutoutFlags() {
        mStatusbarHeight = getContext().getResources().getDimensionPixelSize(
                com.android.internal.R.dimen.status_bar_height_portrait);
        boolean cutoutMasked = getContext().getResources().getBoolean(
                com.android.internal.R.bool.config_maskMainBuiltInDisplayCutout);
        if (mCutoutMasked != cutoutMasked){
            mCutoutMasked = cutoutMasked;
            updatePosition();
        }
    }

    private static float[] getFloatArray(TypedArray array) {
        int length = array.length();
        float[] floatArray = new float[length];
        for (int i = 0; i < length; i++) {
            floatArray[i] = array.getFloat(i, Float.NaN);
        }
        array.recycle();
        return floatArray;
    }
}
