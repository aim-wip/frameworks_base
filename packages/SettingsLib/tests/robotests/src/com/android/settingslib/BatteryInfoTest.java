/*
 * Copyright (C) 2017 The Android Open Source Project
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

package com.android.settingslib;

import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.os.BatteryManager;
import android.os.BatteryStats;
import android.os.SystemClock;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.AdditionalMatchers;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.AdditionalMatchers.*;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.when;

@RunWith(RobolectricTestRunner.class)
@Config(manifest = TestConfig.MANIFEST_PATH, sdk = TestConfig.SDK_VERSION)
public class BatteryInfoTest {
    private static final String STATUS_FULL = "Full";
    private static final String STATUS_CHARGING_NO_TIME = "Charging";
    private static final String STATUS_CHARGING_TIME = "Charging - 2h left";
    private static final int PLUGGED_IN = 1;
    private static final long REMAINING_TIME_NULL = -1;
    private static final long REMAINING_TIME = 2;
    public static final String ENHANCED_STRING_SUFFIX = "left based on your usage";
    private Intent mDisChargingBatteryBroadcast;
    private Intent mChargingBatteryBroadcast;
    private Context mRealContext;

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private BatteryStats mBatteryStats;
    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private Context mContext;
    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private Resources mResources;


    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);

        mDisChargingBatteryBroadcast = new Intent();
        mDisChargingBatteryBroadcast.putExtra(BatteryManager.EXTRA_PLUGGED, 0);
        mDisChargingBatteryBroadcast.putExtra(BatteryManager.EXTRA_LEVEL, 0);
        mDisChargingBatteryBroadcast.putExtra(BatteryManager.EXTRA_SCALE, 100);
        mDisChargingBatteryBroadcast.putExtra(BatteryManager.EXTRA_STATUS,
                BatteryManager.BATTERY_STATUS_FULL);

        mChargingBatteryBroadcast = new Intent();
        mChargingBatteryBroadcast.putExtra(BatteryManager.EXTRA_PLUGGED,
                BatteryManager.BATTERY_PLUGGED_AC);
        mChargingBatteryBroadcast.putExtra(BatteryManager.EXTRA_LEVEL, 50);
        mChargingBatteryBroadcast.putExtra(BatteryManager.EXTRA_SCALE, 100);
        mChargingBatteryBroadcast.putExtra(BatteryManager.EXTRA_STATUS,
                BatteryManager.BATTERY_STATUS_UNKNOWN);

        when(mContext.getResources().getString(R.string.battery_info_status_full))
                .thenReturn(STATUS_FULL);
        when(mContext.getResources().getString(eq(R.string.power_charging), any(),
                any())).thenReturn(STATUS_CHARGING_NO_TIME);
        when(mContext.getResources().getString(eq(R.string.power_charging_duration), any(),
                any())).thenReturn(STATUS_CHARGING_TIME);
        mRealContext = RuntimeEnvironment.application;
    }

    @Test
    public void testGetBatteryInfo_hasStatusLabel() {
        doReturn(REMAINING_TIME_NULL).when(mBatteryStats).computeBatteryTimeRemaining(anyLong());
        BatteryInfo info = BatteryInfo.getBatteryInfo(mContext, mDisChargingBatteryBroadcast,
                mBatteryStats, SystemClock.elapsedRealtime() * 1000, true /* shortString */);

        assertThat(info.statusLabel).isEqualTo(STATUS_FULL);
    }

    @Test
    public void testGetBatteryInfo_doNotShowChargingMethod_hasRemainingTime() {
        doReturn(REMAINING_TIME).when(mBatteryStats).computeChargeTimeRemaining(anyLong());
        BatteryInfo info = BatteryInfo.getBatteryInfo(mContext, mChargingBatteryBroadcast,
                mBatteryStats, SystemClock.elapsedRealtime() * 1000, false /* shortString */);

        assertThat(info.chargeLabelString).isEqualTo(STATUS_CHARGING_TIME);
    }

    @Test
    public void testGetBatteryInfo_doNotShowChargingMethod_noRemainingTime() {
        doReturn(REMAINING_TIME_NULL).when(mBatteryStats).computeChargeTimeRemaining(anyLong());
        BatteryInfo info = BatteryInfo.getBatteryInfo(mContext, mChargingBatteryBroadcast,
                mBatteryStats, SystemClock.elapsedRealtime() * 1000, false /* shortString */);

        assertThat(info.chargeLabelString).isEqualTo(STATUS_CHARGING_NO_TIME);
    }

    @Test
    public void testGetBatteryInfo_pluggedIn_dischargingFalse() {
        BatteryInfo info = BatteryInfo.getBatteryInfo(mContext, mChargingBatteryBroadcast,
                mBatteryStats, SystemClock.elapsedRealtime() * 1000, true /* shortString */);

        assertThat(info.discharging).isEqualTo(false);
    }

    @Test
    public void testGetBatteryInfo_basedOnUsageTrue_usesUsageString() {
        doReturn(mResources).when(mContext).getResources();
        when(mResources.getString(eq(R.string.battery_info_status_full))).thenReturn("");
        when(mResources.getString(eq(R.string.power_remaining_duration_only_enhanced), any()))
                .thenReturn(ENHANCED_STRING_SUFFIX);
        when(mResources.getString(eq(R.string.power_remaining_duration_only_short_enhanced), any()))
                .thenReturn(ENHANCED_STRING_SUFFIX);
        BatteryInfo info = BatteryInfo.getBatteryInfo(mContext, mDisChargingBatteryBroadcast,
                mBatteryStats, SystemClock.elapsedRealtime() * 1000, false /* shortString */,
                1000, true /* basedOnUsage */);
        BatteryInfo info2 = BatteryInfo.getBatteryInfo(mContext, mDisChargingBatteryBroadcast,
                mBatteryStats, SystemClock.elapsedRealtime() * 1000, true /* shortString */,
                1000, true /* basedOnUsage */);

        assertThat(info.remainingLabel).contains(ENHANCED_STRING_SUFFIX);
        assertThat(info2.remainingLabel).contains(ENHANCED_STRING_SUFFIX);
    }

    @Test
    public void testGetBatteryInfo_basedOnUsageFalse_usesDefaultString() {
        doReturn(mResources).when(mContext).getResources();
        when(mResources.getString(eq(R.string.battery_info_status_full))).thenReturn("");
        when(mResources.getString(not(eq(R.string.power_remaining_duration_only_enhanced)), any()))
                .thenReturn(ENHANCED_STRING_SUFFIX);
        when(mResources.getString(not(eq(R.string.power_remaining_duration_only_short_enhanced)),
                any())).thenReturn("");
        BatteryInfo info = BatteryInfo.getBatteryInfo(mContext, mDisChargingBatteryBroadcast,
                mBatteryStats, SystemClock.elapsedRealtime() * 1000, false /* shortString */,
                1000, false /* basedOnUsage */);
        BatteryInfo info2 = BatteryInfo.getBatteryInfo(mContext, mDisChargingBatteryBroadcast,
                mBatteryStats, SystemClock.elapsedRealtime() * 1000, true /* shortString */,
                1000, false /* basedOnUsage */);

        assertThat(info.remainingLabel).doesNotContain(ENHANCED_STRING_SUFFIX);
        assertThat(info2.remainingLabel).doesNotContain(ENHANCED_STRING_SUFFIX);
    }
}
