package com.jonuy.thumbwars.datastore;

import android.telephony.SmsMessage;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Iterator;

/**
 * Wrapper for an SmsMessage List in order to transfer it from
 * one activity to another through the Intent bundle.
 */
public class SmsDataCacheTransfer implements Serializable {

    private static final long serialVersionUID = 1L;

    private ArrayList<SmsDataCache> mSmsData;

    public SmsDataCacheTransfer(ArrayList<SmsDataCache> smsData) {
        mSmsData = smsData;
    }

    public ArrayList<SmsMessage> getMessages() {
        ArrayList<SmsMessage> smsMessages = new ArrayList<SmsMessage>();

        for (Iterator<SmsDataCache> iter = mSmsData.iterator(); iter.hasNext();) {
            SmsDataCache data = iter.next();
            SmsMessage[] messages = data.getMessages();

            for (int i = 0; i < messages.length; i++) {
                smsMessages.add(messages[i]);
            }
        }

        return smsMessages;
    }

}
