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

import com.android.internal.logging.MetricsLogger;
import com.android.systemui.qs.SecureSetting;
import com.android.systemui.qs.QSTile;
import com.android.systemui.R;

public class FloatingTile extends QSTile<QSTile.BooleanState> {
    public static final String TAG = "FloatingTile";

    private final SecureSetting mSetting;

    public FloatingTile(Host host) {
        super(host);

        mSetting = new SecureSetting(mContext, mHandler, Secure.FLOATING_HEADSUP) {
            @Override
            protected void handleValueChanged(int value, boolean observedChange) {
                handleRefreshState(value);
            }
        };
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
        // TODO: Add proper detail view
        handleClick();
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
        state.label = mContext.getString(R.string.qs_floating_headsup_label);
        if (enable) {
            state.icon = ResourceIcon.get(R.drawable.ic_qs_floating_on);
        } else {
            state.icon = ResourceIcon.get(R.drawable.ic_qs_floating_off);
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
}
