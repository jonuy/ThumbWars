package com.jonuy.thumbwars.activities;

import android.app.Activity;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.CompoundButton;
import android.widget.ToggleButton;

import com.jonuy.thumbwars.R;

/**
 * Launcher activity for the app.
 */
public class MainActivity extends Activity implements CompoundButton.OnCheckedChangeListener {

    private final String PREFS_NAME = "ThumbWarsPrefs";
    private final String PREFS_IS_SMS_BLOCKED = "isSmsBlocked";

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
}
