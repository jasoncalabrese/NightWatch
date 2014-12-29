package com.dexdrip.stephenblack.nightwatch;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.os.BatteryManager;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.support.wearable.view.WatchViewStub;
import android.widget.TextView;

import com.google.android.gms.wearable.DataMap;

import java.text.SimpleDateFormat;
import java.util.Calendar;

public class Home extends Activity {
    private final static IntentFilter INTENT_FILTER;
    private static final int BRIGHT_GREEN = Color.parseColor("#4cff00");
    private TextView mTime, mBattery, mSgv, mBgDelta, mTrend, mDirection, mTimestamp, mUploaderBattery, mIOB;
    private final String TIME_FORMAT_DISPLAYED = "h:mm";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);
        final WatchViewStub stub = (WatchViewStub) findViewById(R.id.watch_view_stub);
        IntentFilter messageFilter = new IntentFilter(Intent.ACTION_SEND);

        MessageReceiver messageReceiver = new MessageReceiver();
        LocalBroadcastManager.getInstance(this).registerReceiver(messageReceiver, messageFilter);

        stub.setOnLayoutInflatedListener(new WatchViewStub.OnLayoutInflatedListener() {
            @Override
            public void onLayoutInflated(WatchViewStub stub) {
                mTime = (TextView) stub.findViewById(R.id.watch_time);
                mBattery = (TextView) stub.findViewById(R.id.watch_battery);
                mSgv = (TextView) stub.findViewById(R.id.sgv);
                mSgv.setText("--");
                mBgDelta = (TextView) stub.findViewById(R.id.bg_delta);
                mBgDelta.setText("--");
                mDirection = (TextView) stub.findViewById(R.id.direction);
                mDirection.setText("-");
                mUploaderBattery = (TextView) stub.findViewById(R.id.uploader_battery);
                mUploaderBattery.setText("-");
                mTimestamp = (TextView) stub.findViewById(R.id.read_ago);
                mTimestamp.setText("? mins ago");
                mIOB = (TextView) stub.findViewById(R.id.iob);
                mIOB.setText("--");
                mTimeInfoReceiver.onReceive(Home.this, registerReceiver(null, INTENT_FILTER));
                registerReceiver(mBatInfoReceiver, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
                registerReceiver(mTimeInfoReceiver, INTENT_FILTER);
            }
        });

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(mBatInfoReceiver);
        unregisterReceiver(mTimeInfoReceiver);
    }

    static {
        INTENT_FILTER = new IntentFilter();
        INTENT_FILTER.addAction(Intent.ACTION_TIME_TICK);
        INTENT_FILTER.addAction(Intent.ACTION_TIMEZONE_CHANGED);
        INTENT_FILTER.addAction(Intent.ACTION_TIME_CHANGED);
    }

    private BroadcastReceiver mBatInfoReceiver = new BroadcastReceiver(){
        @Override
        public void onReceive(Context arg0, Intent intent) {
            mBattery.setText(String.valueOf(intent.getIntExtra(BatteryManager.EXTRA_LEVEL, 0) + "%"));
        }
    };
    private BroadcastReceiver mTimeInfoReceiver = new BroadcastReceiver(){
        @Override
        public void onReceive(Context arg0, Intent intent) {
            mTime.setText(
                    new SimpleDateFormat(TIME_FORMAT_DISPLAYED)
                            .format(Calendar.getInstance().getTime()));
        }
    };

    public class MessageReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            DataMap dataMap = DataMap.fromBundle(intent.getBundleExtra("data"));

            mSgv.setText(dataMap.getString("sgvString"));

            int sgv = Integer.parseInt(dataMap.getString("sgv"));

            //TODO: get thresholds from somewhere

            if (sgv > 180) {
                mSgv.setTextColor(Color.YELLOW);
                mDirection.setTextColor(Color.YELLOW);
                mBgDelta.setTextColor(Color.YELLOW);
            } else if (sgv < 80) {
                mSgv.setTextColor(Color.RED);
                mDirection.setTextColor(Color.RED);
                mBgDelta.setTextColor(Color.RED);
            } else {
                mSgv.setTextColor(BRIGHT_GREEN);
                mDirection.setTextColor(BRIGHT_GREEN);
                mBgDelta.setTextColor(BRIGHT_GREEN);
            }

            double bgDelta = dataMap.getDouble("bgdelta");
            String bgDeltaDisplay = "";
            if (bgDelta >= 0) bgDeltaDisplay = "+";
            //TODO: mmol support
            bgDeltaDisplay += ((int) bgDelta);
            bgDeltaDisplay += " mg/dl";

            mBgDelta.setText(bgDeltaDisplay);
//            mTrend.setText(dataMap.getString("trend"));
            mDirection.setText(dataMap.getString("slopeArrow"));
            mTimestamp.setText(dataMap.getString("readingAge"));
            mUploaderBattery.setText(dataMap.getInt("battery_int") + "%");
            mIOB.setText(dataMap.getString("iob"));
        }
    }
}
