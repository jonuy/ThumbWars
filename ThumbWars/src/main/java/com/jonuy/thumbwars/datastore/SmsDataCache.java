package com.jonuy.thumbwars.datastore;

import android.content.Intent;
import android.os.Bundle;
import android.telephony.SmsMessage;

import java.io.Serializable;

/**
 * Serializable object of data that can rebuild an Intent received from SMS.
 */
public class SmsDataCache implements Serializable {

    private static final long serialVersionUID = 1L;

    private String mUri; // uri to create Intent

    private String mExtra_format; // "format"

    private Object[] mExtra_pdus; // "pdus" messages

    public SmsDataCache(Intent intent) {
        mUri = intent.toUri(0);

        Bundle extras = intent.getExtras();
        for (String key : extras.keySet()) {
            if (key.equals("format")) {
                mExtra_format = extras.getString("format");
            }
            else if (key.equals("pdus")) {
                mExtra_pdus = (Object[])extras.getSerializable("pdus");
            }
        }
    }

    /**
     * Converts the Intent properties that populate the properties of this
     * object to an array of SmsMessages.
     *
     * @return SmsMessages[] representation of the object's data
     */
    public SmsMessage[] getMessages() {
        int pduCount = mExtra_pdus.length;

        SmsMessage[] messages = new SmsMessage[pduCount];
        for (int i = 0; i < pduCount; i++) {
            byte[] pdu = (byte[]) mExtra_pdus[i];
            messages[i] = SmsMessage.createFromPdu(pdu);
        }

        return messages;
    }

}
