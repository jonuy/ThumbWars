package com.jonuy.thumbwars.activities;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.telephony.SmsMessage;
import android.text.format.DateFormat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

import com.jonuy.thumbwars.R;
import com.jonuy.thumbwars.datastore.SmsDataCacheTransfer;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

public class ResultsActivity extends Activity implements AdapterView.OnItemClickListener {

    // Keys for Intent extras
    private final String EXTRA_TIME_ELAPSED = "timeElapsed";
    private final String EXTRA_MESSAGES = "smsMessages";

    // View for the distance traveled
    private TextView mDistanceResults;

    // Adapter to bind message datastore to the UI ListView
    private MessagesAdapter mMessagesAdapter;

    // View to display list of blocked messages
    private ListView mMessagesList;

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
        SmsDataCacheTransfer msgTransfer = (SmsDataCacheTransfer)callingIntent.getSerializableExtra(EXTRA_MESSAGES);
        if (msgTransfer != null) {
            mSmsMessages = msgTransfer.getMessages();
        }

        mTimeResults = (TextView)findViewById(R.id.timeResults);
        mDistanceResults = (TextView)findViewById(R.id.distResults);
        mMessagesList = (ListView)findViewById(R.id.messagesList);
        mMessagesResults = (TextView)findViewById(R.id.msgsResults);

        // Convert milliseconds to readable time and display
        int seconds = (int)(timeElapsed / 1000);
        int minutes = seconds / 60;
        int hours = minutes / 60;

        minutes = minutes % 60;
        seconds = seconds % 60;

        String displayTime = String.format("%02d:%02d:%02d", hours, minutes, seconds);
        mTimeResults.setText(displayTime);

        // Display # of messages blocked
        int numMessagesBlocked = 0;
        if (mSmsMessages != null) {
            numMessagesBlocked = mSmsMessages.size();
        }
        mMessagesResults.setText(Integer.toString(numMessagesBlocked));

        // Set adapter for the list view
        mMessagesAdapter = new MessagesAdapter(this, mSmsMessages);
        mMessagesList.setAdapter(mMessagesAdapter);
        mMessagesList.setOnItemClickListener(this);
    }

    /**
     * When ListView item is clicked, start Intent for user to reply
     */
    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        SmsMessage msg = (SmsMessage)parent.getItemAtPosition(position);

        new AlertDialog.Builder(ResultsActivity.this)
                .setTitle(msg.getOriginatingAddress())
                .setMessage(msg.getMessageBody())
                .setCancelable(true)
                .setNegativeButton(R.string.results_dialog_msg_cancel, null)
                .setPositiveButton(R.string.results_dialog_msg_reply, new MessageDialogClickListener(msg.getOriginatingAddress()))
                .create()
                .show();
    }

    /**
     * Handles what to do when postive button is clicked on the message dialog box.
     */
    private class MessageDialogClickListener implements DialogInterface.OnClickListener {

        private String address;

        public MessageDialogClickListener(String address) {
            this.address = address;
        }

        @Override
        public void onClick(DialogInterface dialogInterface, int i) {
            Uri uri = Uri.parse("smsto:" + address);
            Intent intent = new Intent(Intent.ACTION_SENDTO, uri);
            startActivity(intent);
        }
    }

    /**
     * Adapter for tying messages datastore with the ListView UI
     */
    private class MessagesAdapter extends ArrayAdapter<SmsMessage> {
        private Context context;

        public MessagesAdapter(Context context, List<SmsMessage> messages) {
            super(context, android.R.layout.simple_list_item_1, messages);

            this.context = context;
        }

        public View getView(int position, View convertView, ViewGroup parent) {
            View v = convertView;

            if (v == null) {
                LayoutInflater inflater = LayoutInflater.from(context);
                v = inflater.inflate(R.layout.results_messages_row, parent, false);
            }

            SmsMessage msg = getItem(position);

            // Display address
            String address = msg.getOriginatingAddress();
            TextView addrView = (TextView)v.findViewById(R.id.address);
            addrView.setText(address);

            // Display timestamp
            long timestamp = msg.getTimestampMillis();
            Calendar cal = Calendar.getInstance();
            cal.setTimeInMillis(timestamp);
            String displayTime = DateFormat.format("hh:mm", cal).toString();

            TextView dateView = (TextView)v.findViewById(R.id.date);
            dateView.setText(displayTime);

            // Display message body
            String body = msg.getMessageBody().toString();
            TextView bodyView = (TextView)v.findViewById(R.id.body);
            bodyView.setText(body);

            return v;
        }
    }

}
