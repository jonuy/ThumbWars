package com.jonuy.thumbwars.activities;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.telephony.SmsMessage;
import android.widget.TextView;

import com.jonuy.thumbwars.R;

import java.util.ArrayList;

public class ResultsActivity extends Activity {

    // Keys for Intent extras
    private final String EXTRA_TIME_ELAPSED = "timeElapsed";
    private final String EXTRA_MESSAGES = "smsMessages";

    // View for the distance traveled
    private TextView mDistanceResults;

    // View for the number of messages blocked
    private TextView mMessagesResults;

    // View for the time elapsed
    private TextView mTimeResults;

    // Blocked SMS messages
    private ArrayList<SmsMessage> mSmsMessages;

        @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_results);

        getActionBar().setDisplayHomeAsUpEnabled(true);

        Intent callingIntent = getIntent();
        long timeElapsed = callingIntent.getLongExtra(EXTRA_TIME_ELAPSED, 0);
        mSmsMessages = (ArrayList<SmsMessage>)callingIntent.getSerializableExtra(EXTRA_MESSAGES);

        mTimeResults = (TextView)findViewById(R.id.timeResults);
        mDistanceResults = (TextView)findViewById(R.id.distResults);
        mMessagesResults = (TextView)findViewById(R.id.msgsResults);

        int seconds = (int)(timeElapsed / 1000);
        int minutes = seconds / 60;
        int hours = minutes / 60;

        minutes = minutes % 60;
        seconds = seconds % 60;

        String displayTime = String.format("%02d:%02d:%02d", hours, minutes, seconds);
        mTimeResults.setText(displayTime);
        mMessagesResults.setText(Integer.toString(mSmsMessages.size()));
    }

}
