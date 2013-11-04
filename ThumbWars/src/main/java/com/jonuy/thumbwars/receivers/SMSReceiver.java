package com.jonuy.thumbwars.receivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;

import com.jonuy.thumbwars.datastore.SmsDataCache;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Receiver to capture SMS.
 */
public class SMSReceiver extends BroadcastReceiver {

    // Fields for SharedPreferences
    private final String PREFS_NAME = "ThumbWarsPrefs";
    private final String PREFS_IS_SMS_BLOCKED = "isSmsBlocked";

    // Filename for the cached messages
    private final String SMS_FILENAME = "ThumbWarsSMS";

    private boolean mSmsDisabled;

    @Override
    public void onReceive(Context context, Intent intent) {
        SharedPreferences sharedPrefs = context.getSharedPreferences(PREFS_NAME, 0);
        mSmsDisabled = sharedPrefs.getBoolean(PREFS_IS_SMS_BLOCKED, false);

        if (mSmsDisabled) {
            // Prevent other apps from receiving the SMS
            abortBroadcast();

            List<SmsDataCache> currBlockedMsgs = new ArrayList<SmsDataCache>();

            // Check if file exists with our cached messages
            if (context.getFileStreamPath(SMS_FILENAME).exists()) {
                // Build array of currently blocked messages from app's private file
                try {
                    FileInputStream fis = context.openFileInput(SMS_FILENAME);
                    ObjectInputStream ois = new ObjectInputStream(fis);

                    try {
                        // Read messages and updated currBlockedMsgs array.
                        ArrayList<SmsDataCache> cachedMsgs = (ArrayList<SmsDataCache>)ois.readObject();
                        for(Iterator<SmsDataCache> iter = cachedMsgs.iterator(); iter.hasNext();) {
                            SmsDataCache cachedMsg = iter.next();
                            currBlockedMsgs.add(cachedMsg);
                        }
                    }
                    catch (Exception e) {
                        e.printStackTrace();
                    }

                    ois.close();
                    fis.close();
                }
                catch (FileNotFoundException e) {
                    e.printStackTrace();
                }
                catch (IOException e) {
                    e.printStackTrace();
                }
            }

            // Update private file with the message that was just received
            try {
                // Update array of blocked messages with the last received message
                SmsDataCache receivedMsg = new SmsDataCache(intent);
                currBlockedMsgs.add(receivedMsg);

                FileOutputStream fos = context.openFileOutput(SMS_FILENAME, Context.MODE_PRIVATE);

                ObjectOutputStream oos = new ObjectOutputStream(fos);
                oos.writeObject(currBlockedMsgs);

                oos.flush();
                oos.close();
                fos.close();
            }
            catch (FileNotFoundException e) {
                e.printStackTrace();
            }
            catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
