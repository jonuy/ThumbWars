package com.jonuy.thumbwars.activities;

import android.app.Activity;
import android.content.ContentValues;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.telephony.SmsMessage;
import android.widget.CompoundButton;
import android.widget.ToggleButton;

import com.jonuy.thumbwars.R;
import com.jonuy.thumbwars.datastore.SmsDataCache;

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

    private final String PREFS_NAME = "ThumbWarsPrefs";
    private final String PREFS_IS_SMS_BLOCKED = "isSmsBlocked";
    private final String SMS_FILENAME = "ThumbWarsSMS";

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        ToggleButton toggleSmsBlock = (ToggleButton)findViewById(R.id.toggleBlock);
        toggleSmsBlock.setOnCheckedChangeListener(this);

        // Set state of the ToggleButton
        toggleSmsBlock.setChecked(isSmsBlocked());
    }

    @Override
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        if (buttonView.getId() == R.id.toggleBlock) {
            if (isChecked) {
                enableSmsBlocking();
            }
            else {
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
        editor.commit();

        ArrayList<SmsDataCache> smsMessages = null;

        // Get any blocked messages out of the private file
        try {
            FileInputStream fis = openFileInput(SMS_FILENAME);
            ObjectInputStream ois = new ObjectInputStream(fis);

            try {
                smsMessages = (ArrayList<SmsDataCache>)ois.readObject();
            }
            catch (Exception e) {
                e.printStackTrace();
            }

            ois.close();
            fis.close();

            // Write messages to the native inbox database
            for (Iterator<SmsDataCache> iter = smsMessages.iterator(); iter.hasNext();) {
                SmsDataCache smsMsg = iter.next();
                putSmsToDatabase(smsMsg.getMessages());
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

    /**
     * Saves value to SharedPreferences indicating SMS messages can pass through.
     */
    private void enableSmsBlocking() {
        SharedPreferences sharedPrefs = getSharedPreferences(PREFS_NAME, 0);
        SharedPreferences.Editor editor = sharedPrefs.edit();
        editor.putBoolean(PREFS_IS_SMS_BLOCKED, true);
        editor.commit();
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
            values.put("seen", 2);
            values.put("body", msg.getMessageBody().toString());

            getContentResolver().insert(Uri.parse("content://sms"), values);
        }
    }
}
