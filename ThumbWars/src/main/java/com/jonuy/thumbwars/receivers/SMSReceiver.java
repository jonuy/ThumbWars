package com.jonuy.thumbwars.receivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;

/**
 * Receiver to capture SMS.
 */
public class SMSReceiver extends BroadcastReceiver {

    private final String PREFS_NAME = "ThumbWarsPrefs";
    private final String PREFS_IS_SMS_BLOCKED = "isSmsBlocked";

    private Context mContext;
    private Intent mIntent;

    private boolean mSmsDisabled;

    @Override
    public void onReceive(Context context, Intent intent) {
        mContext = context;
        mIntent = intent;

        Bundle extras = intent.getExtras();

        // can just do new Intent().putExtras(intent) to copy over all extras?
        // or save the intent as a Uri string, and then create new Intent later with Intent.parseUri(uri, 0)
        String uri = intent.toUri(0);

        SharedPreferences sharedPrefs = mContext.getSharedPreferences(PREFS_NAME, 0);
        mSmsDisabled = sharedPrefs.getBoolean(PREFS_IS_SMS_BLOCKED, false);

        if (mSmsDisabled) {
            // Prevent other apps from receiving the SMS
            abortBroadcast();

            // TODO: save messages that came in to then broadcast out later once SMS is re-enabled
        }
    }
}
