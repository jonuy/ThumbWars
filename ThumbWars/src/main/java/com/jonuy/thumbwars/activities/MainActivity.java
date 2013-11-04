package com.jonuy.thumbwars.activities;

import android.app.Activity;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.TaskStackBuilder;
import android.telephony.SmsMessage;
import android.widget.CompoundButton;
import android.widget.TextView;
import android.widget.ToggleButton;

import com.jonuy.thumbwars.R;
import com.jonuy.thumbwars.datastore.SmsDataCache;
import com.jonuy.thumbwars.datastore.SmsDataCacheTransfer;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.ArrayList;
import java.util.Iterator;

/**
 * Launcher activity for the app.
 */
public class MainActivity extends Activity implements CompoundButton.OnCheckedChangeListener {

    // Keys for Intent extras
    private final String EXTRA_TIME_ELAPSED = "timeElapsed";
    private final String EXTRA_MESSAGES = "smsMessages";

    // Fields for SharedPreferences
    private final String PREFS_NAME = "ThumbWarsPrefs";
    private final String PREFS_IS_SMS_BLOCKED = "isSmsBlocked";
    private final String PREFS_TIME_ELAPSED = "timeElapsed";
    private final String PREFS_START_TIME = "startTime";

    // Filename for the cached messages
    private final String SMS_FILENAME = "ThumbWarsSMS";

    // Handler to update the timer
    private Handler mHandlerTimer;

    // TextView containing the timer text
    private TextView mTimerText;

    // ToggleButton to enable/disable SMS blocking
    private ToggleButton mSmsToggleButton;

    // Time in milliseconds the timer was started
    private long mStartTime;

    // Indicates whether or not blocking is on
    private boolean mIsBlocking;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mHandlerTimer = new Handler();

        mSmsToggleButton = (ToggleButton)findViewById(R.id.toggleBlock);
        mSmsToggleButton.setOnCheckedChangeListener(this);

        mTimerText = (TextView)findViewById(R.id.timer);
    }

    @Override
    public void onPause() {
        super.onPause();

        // Check if user is leaving the activity with SMS blocking still enabled
        SharedPreferences sharedPrefs = getSharedPreferences(PREFS_NAME, 0);
        if (sharedPrefs.getBoolean(PREFS_IS_SMS_BLOCKED, false)) {
            // Save the start time to reference again when the user returns to the app
            SharedPreferences.Editor editor = sharedPrefs.edit();
            editor.putLong(PREFS_START_TIME, mStartTime);
            editor.commit();
        }

        // Remove Runnable from the Handler. Will be started up again in onResume() when the user
        // returns to the Activity.
        mHandlerTimer.removeCallbacks(updateTimeRunnable);
    }

    @Override
    public void onResume() {
        super.onResume();

        if (isSmsBlocked()) {
            // Set state of the ToggleButton
            mSmsToggleButton.setChecked(true);

            // Set the start time based on SharedPreferences if saved
            SharedPreferences sharedPrefs = getSharedPreferences(PREFS_NAME, 0);
            mStartTime = sharedPrefs.getLong(PREFS_START_TIME, System.currentTimeMillis());
        }
    }

    @Override
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        if (buttonView.getId() == R.id.toggleBlock) {
            if (isChecked) {
                enableSmsBlocking();
            }
            else if (mIsBlocking) {
                disableSmsBlocking();
            }
        }
    }

    /**
     * Saves value to SharedPreferences indicating received SMS messages should be suppressed.
     */
    private void disableSmsBlocking() {
        SharedPreferences sharedPrefs = getSharedPreferences(PREFS_NAME, 0);
        SharedPreferences.Editor editor = sharedPrefs.edit();
        editor.putBoolean(PREFS_IS_SMS_BLOCKED, false);

        mIsBlocking = false;

        // Get the time elapsed
        long timeElapsed = System.currentTimeMillis() - mStartTime;

        // Update the total time SMS has been blocked all time
        long cachedTimeElapsed = System.currentTimeMillis();
        sharedPrefs.getLong(PREFS_TIME_ELAPSED, cachedTimeElapsed);

        cachedTimeElapsed += timeElapsed;
        editor.putLong(PREFS_TIME_ELAPSED, cachedTimeElapsed);
        editor.commit();

        // Stop the timer
        mHandlerTimer.removeCallbacks(updateTimeRunnable);

        // Remove the notification
        NotificationManager notificationManager = (NotificationManager)getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.cancelAll();

        // Wrapper for list of messages to send to results screen
        SmsDataCacheTransfer smsDataCacheTransfer = null;

        File smsFile = getFileStreamPath(SMS_FILENAME);
        if (smsFile.exists()) {
            ArrayList<SmsDataCache> messages = null;

            // Get any blocked messages out of the private file
            try {
                FileInputStream fis = openFileInput(SMS_FILENAME);
                ObjectInputStream ois = new ObjectInputStream(fis);

                try {
                    messages = (ArrayList<SmsDataCache>)ois.readObject();
                    smsDataCacheTransfer = new SmsDataCacheTransfer(messages);
                }
                catch (Exception e) {
                    e.printStackTrace();
                }

                ois.close();
                fis.close();

                // Write messages to the native inbox database
                for (Iterator<SmsDataCache> iter = messages.iterator(); iter.hasNext();) {
                    SmsDataCache smsMsg = iter.next();
                    SmsMessage[] smsMsgs = smsMsg.getMessages();
                    putSmsToDatabase(smsMsgs);
                }

                // Clear the file
                deleteFile(SMS_FILENAME);
            }
            catch (FileNotFoundException e) {
                e.printStackTrace();

                // File of cached messages not found. This is expected for cases where the text blocking
                // is either disabled or enabled and no messages have yet been received.
            }
            catch (IOException e) {
                e.printStackTrace();
            }
        }

        // Go to Results Activity
        Intent intent = new Intent(this, ResultsActivity.class);
        intent.putExtra(EXTRA_TIME_ELAPSED, timeElapsed);
        intent.putExtra(EXTRA_MESSAGES, smsDataCacheTransfer);
        startActivity(intent);
    }

    /**
     * Saves value to SharedPreferences indicating SMS messages can pass through.
     */
    private void enableSmsBlocking() {
        SharedPreferences sharedPrefs = getSharedPreferences(PREFS_NAME, 0);
        SharedPreferences.Editor editor = sharedPrefs.edit();
        editor.putBoolean(PREFS_IS_SMS_BLOCKED, true);
        editor.commit();

        mIsBlocking = true;

        // Start the timer
        mStartTime = System.currentTimeMillis();
        mHandlerTimer.postDelayed(updateTimeRunnable, 0);

        // Create notification
        Notification.Builder notifBuilder = new Notification.Builder(this)
                .setSmallIcon(R.drawable.ic_launcher)
                .setContentTitle(getString(R.string.notification_title))
                .setContentText(getString(R.string.notification_content))
                .setOngoing(true);

        Intent resultIntent = new Intent(this, MainActivity.class);

        TaskStackBuilder stackBuilder = TaskStackBuilder.create(this);
        stackBuilder.addParentStack(MainActivity.class);
        stackBuilder.addNextIntent(resultIntent);

        PendingIntent resultPendingIntent = stackBuilder.getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT);
        notifBuilder.setContentIntent(resultPendingIntent);

        NotificationManager notificationManager = (NotificationManager)getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.notify(0, notifBuilder.build());

    }

    /**
     * Get value from SharedPreferences to see whether or not SMS is suppressed.
     */
    private boolean isSmsBlocked() {
        SharedPreferences sharedPrefs = getSharedPreferences(PREFS_NAME, 0);
        return sharedPrefs.getBoolean(PREFS_IS_SMS_BLOCKED, false);
    }

    /**
     * Submit SMS messages into the native SMS inbox.
     *
     * @param messages SmsMessage[] array of messages
     */
    private void putSmsToDatabase(SmsMessage[] messages) {
        for (int i = 0; i < messages.length; i++) {
            SmsMessage msg = messages[i];

            ContentValues values = new ContentValues();
            values.put("address", msg.getOriginatingAddress());
            values.put("date", msg.getTimestampMillis());
            values.put("read", 0); // Indicate that the message has not yet been read.
            values.put("status", msg.getStatus());
            values.put("type", 1); // 1 indicates inbox message
            values.put("seen", 0);
            values.put("body", msg.getMessageBody().toString());

            getContentResolver().insert(Uri.parse("content://sms"), values);
        }
    }

    /**
     * Runnable to update the timer
     */
    private Runnable updateTimeRunnable = new Runnable() {
        public void run() {
            long timeInMillis = System.currentTimeMillis() - mStartTime;
            int seconds = (int)(timeInMillis / 1000);
            int minutes = seconds / 60;
            int hours = minutes / 60;

            minutes = minutes % 60;
            seconds = seconds % 60;

            String displayTime = String.format("%02d:%02d:%02d", hours, minutes, seconds);
            if (mTimerText != null)
                mTimerText.setText(displayTime);

            if (mHandlerTimer != null)
                mHandlerTimer.postDelayed(updateTimeRunnable, 0);
        }
    };
}
