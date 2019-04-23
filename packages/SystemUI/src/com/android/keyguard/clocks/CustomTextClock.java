/*
**
** Copyright 2019, Pearl Project
** Copyright 2019, Descendant
**
** Licensed under the Apache License, Version 2.0 (the "License");
** you may not use this file except in compliance with the License.
** You may obtain a copy of the License at
**
**     http://www.apache.org/licenses/LICENSE-2.0
**
** Unless required by applicable law or agreed to in writing, software
** distributed under the License is distributed on an "AS IS" BASIS,
** WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
** See the License for the specific language governing permissions and
** limitations under the License.
*/

package com.android.keyguard.clocks;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.ContentResolver;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.text.format.DateUtils;
import android.text.format.DateFormat;
import android.text.format.Time;
import android.util.AttributeSet;
import android.util.Log;
import android.widget.TextView;
import android.provider.Settings;

import com.android.internal.util.ArrayUtils;

import java.lang.String;
import java.util.Locale;
import java.util.TimeZone;

import com.android.systemui.R;

public class CustomTextClock extends TextView {

    private final String[] TensString = getResources().getStringArray(R.array.TensString);
    private final String[] UnitsString = getResources().getStringArray(R.array.UnitsString);
    private final String[] TensStringH = getResources().getStringArray(R.array.TensStringH);
    private final String[] UnitsStringH = getResources().getStringArray(R.array.UnitsStringH);
    private final String[] langExceptions = getResources().getStringArray(R.array.langExceptions);

    private Time mCalendar;

    private boolean mAttached;

    private int handType;

    private boolean h24;

    public CustomTextClock(Context context) {
        this(context, null);
    }

    public CustomTextClock(Context context, AttributeSet attrs) {
        super(context, attrs);

        final TypedArray a = context.obtainStyledAttributes(
                attrs, R.styleable.CustomTextClock);

        handType = a.getInteger(R.styleable.CustomTextClock_HandType, 2);

        mCalendar = new Time();


    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();

        if (!mAttached) {
            mAttached = true;
            IntentFilter filter = new IntentFilter();

            filter.addAction(Intent.ACTION_TIME_TICK);
            filter.addAction(Intent.ACTION_TIME_CHANGED);
            filter.addAction(Intent.ACTION_TIMEZONE_CHANGED);

            // OK, this is gross but needed. This class is supported by the
            // remote views machanism and as a part of that the remote views
            // can be inflated by a context for another user without the app
            // having interact users permission - just for loading resources.
            // For exmaple, when adding widgets from a user profile to the
            // home screen. Therefore, we register the receiver as the current
            // user not the one the context is for.
            getContext().registerReceiverAsUser(mIntentReceiver,
                    android.os.Process.myUserHandle(), filter, null, getHandler());
        }

        // NOTE: It's safe to do these after registering the receiver since the receiver always runs
        // in the main thread, therefore the receiver can't run before this method returns.

        // The time zone may have changed while the receiver wasn't registered, so update the Time
        mCalendar = new Time();

        // Make sure we update to the current time
        onTimeChanged();
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        if (mAttached) {
            getContext().unregisterReceiver(mIntentReceiver);
            mAttached = false;
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
    }

    private void onTimeChanged() {
        mCalendar.setToNow();
        h24 = DateFormat.is24HourFormat(getContext());

        int hour = mCalendar.hour;
        int minute = mCalendar.minute;

        Log.d("CustomTextClock", ""+h24);

        if (!h24) {
            if (hour > 12) {
                hour = hour - 12;
            }
        }

        switch(handType){
            case 0:
                if (hour == 12 && minute == 0) {
                setText(R.string.high_noon_first_row);
                } else {
                setText(getIntStringHour(hour));
                }
                break;
            case 1:
                if (hour == 12 && minute == 0) {
                setText(R.string.high_noon_second_row);
                } else {
                setText(getIntStringMin(minute));
                }
                break;
            default:
                break;
        }


        updateContentDescription(mCalendar, getContext());
    }

    private final BroadcastReceiver mIntentReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(Intent.ACTION_TIMEZONE_CHANGED)) {
                String tz = intent.getStringExtra("time-zone");
                mCalendar = new Time(TimeZone.getTimeZone(tz).getID());
            }

            onTimeChanged();

            invalidate();
        }
    };

    private void updateContentDescription(Time time, Context mContext) {
        final int flags = DateUtils.FORMAT_SHOW_TIME | DateUtils.FORMAT_24HOUR;
        String contentDescription = DateUtils.formatDateTime(mContext,
                time.toMillis(false), flags);
        setContentDescription(contentDescription);
    }

    private String getIntStringHour (int num) {
        int tens, units;
        String NumString = "";
        if(num >= 20) {
            units = num % 10 ;
            tens =  num / 10;
            if ( units == 0 ) {
                NumString = TensStringH[tens];
            } else {
                // Guard exceptions for languages that don't do "number-to-text" typesetting
                // ex. Thirty One, it's composed by Thirty and One
                // ex. Trentuno (it), it's composed by Trenta (30) and Uno (1)
                // in a cutted form for Trenta (Trent) and merged with the Uno (1)
                if (ArrayUtils.contains(langExceptions, Locale.getDefault().getLanguage())) {
                    if (Locale.getDefault().getLanguage() == "it") {
                        if (units == 1) {
                            NumString = TensString[tens].substring(0, TensString.length - 1) + UnitsString[units].toLowerCase();
                        }
                        NumString = TensString[tens] + UnitsString[units].toLowerCase();
                    }
                } else {
                    NumString = TensString[tens]+" "+UnitsString[units];
                }
            }
        } else if (num < 20 ) {
            NumString = UnitsStringH[num];
        }

        return NumString;
    }

    private String getIntStringMin (int num) {
        int tens, units;
        String NumString = "";
        if(num >= 20) {
            units = num % 10 ;
            tens =  num / 10;
            if ( units == 0 ) {
                NumString = TensString[tens];
            } else {
                // Guard exceptions part 2 - same reason as before
                if (ArrayUtils.contains(langExceptions, Locale.getDefault().getLanguage())) { 
                    if (Locale.getDefault().getLanguage() == "it") {
                        if (units == 1) {
                            NumString = TensString[tens].substring(0, TensString.length - 1) + UnitsString[units].toLowerCase();
                        }
                        NumString = TensString[tens] + UnitsString[units].toLowerCase();
                    }
                } else {
                    NumString = TensString[tens]+" "+UnitsString[units];
                }
            }
        } else if (num < 10 ) {
            NumString = UnitsString[num];
        } else if (num >= 10 && num < 20) {
            NumString = UnitsString[num];
        }

        return NumString;
    }

}
