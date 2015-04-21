/*
 * Copyright (C) 2015 CyanideL
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

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.database.ContentObserver;
import android.os.Handler;
import android.provider.Settings;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.CheckedTextView;
import android.widget.ListView;

import com.android.systemui.R;
import com.android.systemui.qs.QSTile;
import com.android.internal.logging.MetricsLogger;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class CandyTile extends QSTile<QSTile.BooleanState> {
    private boolean mListening;
    private CandyObserver mObserver;
    private static final Intent CANDY_SETTINGS = new Intent().setComponent(new ComponentName(
            "com.android.settings", "com.android.settings.Settings$MainSettingsActivity"));
    private static final Intent CANDY_INT = new Intent().setComponent(new ComponentName(
            "com.android.settings", "com.android.settings.Settings$CandyCentralActivity"));

    public CandyTile(Host host) {
        super(host);
        mObserver = new CandyObserver(mHandler);
    }

    @Override
    protected BooleanState newTileState() {
        return new BooleanState();
    }

    @Override
    public int getMetricsCategory() {
        return MetricsLogger.DONT_TRACK_ME_BRO;
    }

    @Override
    protected void handleClick() {
      mHost.startActivityDismissingKeyguard(CANDY_SETTINGS);
    }

     @Override
    protected void handleSecondaryClick() {
      mHost.startActivityDismissingKeyguard(CANDY_SETTINGS);
    }

    @Override
    public void handleLongClick() {
      mHost.startActivityDismissingKeyguard(CANDY_SETTINGS);
    }

    @Override
    protected void handleUpdateState(BooleanState state, Object arg) {
        state.visible = true;
	state.icon = ResourceIcon.get(R.drawable.ic_qs_candy_on);
        state.label = mContext.getString(R.string.quick_settings_candy_on);

	}

    @Override
    public void setListening(boolean listening) {
        if (mListening == listening) return;
        mListening = listening;
    }

    private class CandyObserver extends ContentObserver {
        public CandyObserver(Handler handler) {
            super(handler);
        }
    }
}

