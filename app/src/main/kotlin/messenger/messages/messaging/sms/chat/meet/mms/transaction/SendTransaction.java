package messenger.messages.messaging.sms.chat.meet.mms.transaction;

import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import messenger.messages.messaging.sms.chat.meet.android.database.SqliteWrapper;
import android.net.Uri;
import android.provider.Telephony.Mms;
import android.text.TextUtils;


import messenger.messages.messaging.sms.chat.meet.mms.util.RateController;
import messenger.messages.messaging.sms.chat.meet.android.mms.pdu_alt.EncodedStringValue;
import messenger.messages.messaging.sms.chat.meet.android.mms.pdu_alt.PduPersister;
import messenger.messages.messaging.sms.chat.meet.android.mms.pdu_alt.SendReq;
import android.util.Log;
import messenger.messages.messaging.sms.chat.meet.send_message.BroadcastUtils;
import messenger.messages.messaging.sms.chat.meet.send_message.Utils;

public class SendTransaction extends Transaction implements Runnable {
    private static final String TAG = "loggg";

    private Thread mThread;
    public final Uri mSendReqURI;

    public SendTransaction(Context context,
            int transId, TransactionSettings connectionSettings, String uri) {
        super(context, transId, connectionSettings);
        mSendReqURI = Uri.parse(uri);
        mId = uri;

        // Attach the transaction to the instance of RetryScheduler.
        attach(RetryScheduler.getInstance(context));
    }

    @Override
    public void process() {
        mThread = new Thread(this, "SendTransaction");
        mThread.start();
    }

    public void run() {
        StringBuilder builder = new StringBuilder();
        try {
            RateController.init(mContext);
            RateController rateCtlr = RateController.getInstance();
            if (rateCtlr.isLimitSurpassed() && !rateCtlr.isAllowedByUser()) {
                Log.e(TAG, "Sending rate limit surpassed.");
                return;
            }

            PduPersister persister = PduPersister.getPduPersister(mContext);
            SendReq sendReq = (SendReq) persister.load(mSendReqURI);

            long date = System.currentTimeMillis() / 1000L;
            sendReq.setDate(date);

            ContentValues values = new ContentValues(1);
            values.put(Mms.DATE, date);
            SqliteWrapper.update(mContext, mContext.getContentResolver(),
                                 mSendReqURI, values, null, null);

            String lineNumber = Utils.getMyPhoneNumber(mContext);
            if (!TextUtils.isEmpty(lineNumber)) {
                sendReq.setFrom(new EncodedStringValue(lineNumber));
            }

            long tokenKey = ContentUris.parseId(mSendReqURI);

        } catch (Throwable t) {
            Log.e(TAG, "error", t);
        } finally {
            if (mTransactionState.getState() != TransactionState.SUCCESS) {
                mTransactionState.setState(TransactionState.FAILED);
                mTransactionState.setContentUri(mSendReqURI);
                Log.e(TAG, "Delivery failed.");
                builder.append("Delivery failed\n");

                Intent intent = new Intent(messenger.messages.messaging.sms.chat.meet.send_message.Transaction.MMS_ERROR);
                intent.putExtra("stack", builder.toString());
                BroadcastUtils.sendExplicitBroadcast(
                        mContext, intent, messenger.messages.messaging.sms.chat.meet.send_message.Transaction.MMS_ERROR);
            }
            notifyObservers();
        }
    }

    @Override
    public int getType() {
        return SEND_TRANSACTION;
    }
}
