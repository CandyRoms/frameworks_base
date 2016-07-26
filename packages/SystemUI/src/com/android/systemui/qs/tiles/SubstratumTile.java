/*
 * Copyright (c) 2016 Project Substratum
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
import android.content.IntentFilter;
import android.net.Uri;
import android.os.UserHandle;
import android.os.Handler;
import android.os.RemoteException;
import android.view.View;

import com.android.internal.logging.MetricsLogger;
import com.android.systemui.R;
import com.android.systemui.qs.QSTile;

public class SubstratumTile extends QSTile<QSTile.BooleanState> {

    private static final String CATEGORY_SUBSTRATUM = "projekt.substratum.MainActivity";
    private boolean mListening;

    public SubstratumTile(Host host) {
        super(host);
    }

    @Override
    protected BooleanState newTileState() {
        return new BooleanState();
    }

    public void setListening(boolean listening) {
    }

    @Override
    public void handleClick() {
        MetricsLogger.action(mContext, getMetricsCategory());
        mHost.collapsePanels();
        Intent intent = new Intent(Intent.ACTION_MAIN);
        intent.setClassName("projekt.substratum",
            "projekt.substratum.MainActivity");
        mHost.startActivityDismissingKeyguard(intent);
    }

    @Override
    public void handleLongClick() {
    }

    @Override
    public int getMetricsCategory() {
        return MetricsLogger.QS_SUBSTRATUM;
    }

    @Override
    protected void handleUpdateState(BooleanState state, Object arg) {
        state.visible = true;
        state.label = mContext.getString(R.string.quick_settings_substratum_label);
        state.contentDescription = mContext.getString(
                R.string.accessibility_quick_settings_substratum);
        state.icon = ResourceIcon.get(R.drawable.ic_qs_substratum);
    }
}
