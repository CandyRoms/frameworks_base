/*
 * Copyright (C) 2016 The ParanoidAndroid Project
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

package com.android.systemui.qs.tiles;

import android.content.Context;
import android.content.Intent;
import android.provider.Settings;
import android.provider.Settings.Secure;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.android.internal.logging.MetricsLogger;
import com.android.systemui.qs.SecureSetting;
import com.android.systemui.qs.QSTile;
import com.android.systemui.R;

public class FloatingTile extends QSTile<QSTile.BooleanState> {
    public static final String TAG = "FloatingTile";

    private final SecureSetting mSetting;
    private final FloatingDetailAdapter mDetailAdapter;

    public FloatingTile(Host host) {

        super(host);
        mDetailAdapter = new FloatingDetailAdapter();

        mSetting = new SecureSetting(mContext, mHandler, Secure.FLOATING_HEADSUP) {
            @Override
            protected void handleValueChanged(int value, boolean observedChange) {
                handleRefreshState(value);
            }
        };
    }

    @Override
    public DetailAdapter getDetailAdapter() {
        return mDetailAdapter;
    }

    @Override
    protected void handleUserSwitch(int newUserId) {
        mSetting.setUserId(newUserId);
        handleRefreshState(mSetting.getValue());
    }

    @Override
    protected BooleanState newTileState() {
        return new BooleanState();
    }

    @Override
    protected void handleClick() {
        setEnabled(!mState.value);
        refreshState();
    }

    // @Override
    protected void handleDetailClick() {
        showDetail(true);
    }

    @Override
    protected void handleLongClick() {
        // Do nothing
    }

    private void setEnabled(boolean enabled) {
        mSetting.setValue(enabled ? 1 : 0);
    }

    @Override
    protected void handleUpdateState(BooleanState state, Object arg) {
        final int value = arg instanceof Integer ? (Integer)arg : mSetting.getValue();
        final boolean enable = value != 0;
        state.value = enable;
        state.visible = true;
        state.label = mContext.getString(R.string.quick_settings_floating_label);
        if (enable) {
            state.icon = ResourceIcon.get(R.drawable.ic_qs_floating_peek);
        } else {
            state.icon = ResourceIcon.get(R.drawable.ic_qs_floating_peek_disabled);
        }
    }

    @Override
    public int getMetricsCategory() {
        return MetricsLogger.QS_FLOATING;
    }

    @Override
    public void setListening(boolean listening) {
        // Do nothing
    }

    private class FloatingDetailAdapter implements DetailAdapter {

        private ViewGroup mMessageContainer;
        private TextView mMessageText;

        @Override
        public int getMetricsCategory() {
            return MetricsLogger.QS_FLOATING_DETAILS;
        }

        @Override
        public int getTitle() {
            return R.string.quick_settings_floating_label;
        }

        @Override
        public Boolean getToggleState() {
            return mState.value;
        }

        @Override
        public void setToggleState(boolean state) {
            MetricsLogger.action(mContext, MetricsLogger.QS_FLOATING_TOGGLE, state);
            if (!state) {
                showDetail(false);
            }
            setEnabled(state);
            fireToggleStateChanged(state);
            refreshState();
        }

        @Override
        public Intent getSettingsIntent() {
            return null;
        }

        @Override
        public View createDetailView(Context context, View convertView, ViewGroup parent) {
            final LinearLayout mDetails = convertView != null ? (LinearLayout) convertView
                    : (LinearLayout) LayoutInflater.from(context).inflate(
                            R.layout.floating_mode_layout, parent, false);
            if (convertView == null) {
                mMessageContainer = (ViewGroup) mDetails.findViewById(R.id.floating_introduction);
                mMessageText = (TextView) mDetails.findViewById(R.id.floating_introduction_message);
                mMessageText.setText(mContext.getString(
                        R.string.quick_settings_floating_description));
                mMessageContainer.setVisibility(View.VISIBLE);
            }

            setToggleState(true);

            return mDetails;
        }
    }
}
