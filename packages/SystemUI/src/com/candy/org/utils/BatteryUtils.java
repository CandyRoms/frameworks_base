package com.candy.org.utils;

import android.content.res.Resources;
import android.util.TypedValue;

public class BatteryUtils {

    public static int dpToPx(float dp) {
        return Math.round(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp,
                                                    Resources.getSystem().getDisplayMetrics()));
    }
}
