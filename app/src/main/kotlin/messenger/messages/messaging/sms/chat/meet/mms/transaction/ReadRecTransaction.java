package messenger.messages.messaging.sms.chat.meet.mms.transaction;

import android.content.Context;
import android.net.Uri;
import android.provider.Telephony.Mms.Sent;
import android.util.Log;


import messenger.messages.messaging.sms.chat.meet.android.mms.MmsException;
import messenger.messages.messaging.sms.chat.meet.android.mms.pdu_alt.EncodedStringValue;
import messenger.messages.messaging.sms.chat.meet.android.mms.pdu_alt.PduComposer;
import messenger.messages.messaging.sms.chat.meet.android.mms.pdu_alt.PduPersister;
import messenger.messages.messaging.sms.chat.meet.android.mms.pdu_alt.ReadRecInd;
import messenger.messages.messaging.sms.chat.meet.send_message.Utils;

public class ReadRecTransaction extends Transaction implements Runnable{
    private static final String TAG = "loggg";
    private static final boolean DEBUG = false;
    private static final boolean LOCAL_LOGV = false;

    private Thread mThread;
    private final Uri mReadReportURI;

    public ReadRecTransaction(Context context,
            int transId,
            TransactionSettings connectionSettings,
            String uri) {
        super(context, transId, connectionSettings);
        mReadReportURI = Uri.parse(uri);
        mId = uri;

        // Attach the transaction to the instance of RetryScheduler.
        attach(RetryScheduler.getInstance(context));
    }


    @Override
    public void process() {
        mThread = new Thread(this, "ReadRecTransaction");
        mThread.start();
    }

    public void run() {
        PduPersister persister = PduPersister.getPduPersister(mContext);

        try {
            // Load M-read-rec.ind from outbox
            ReadRecInd readRecInd = (ReadRecInd) persister.load(mReadReportURI);

            // insert the 'from' address per spec
            String lineNumber = Utils.getMyPhoneNumber(mContext);
            readRecInd.setFrom(new EncodedStringValue(lineNumber));

            // Pack M-read-rec.ind and send it
            byte[] postingData = new PduComposer(mContext, readRecInd).make();
//            sendPdu(postingData);

            Uri uri = persister.move(mReadReportURI, Sent.CONTENT_URI);
            mTransactionState.setState(TransactionState.SUCCESS);
            mTransactionState.setContentUri(uri);
        }/* catch (IOException e) {
            if (LOCAL_LOGV) {
                Log.v(TAG, "Failed to send M-Read-Rec.Ind.", e);
            }
        }*/ catch (MmsException e) {
            if (LOCAL_LOGV) {
                Log.v(TAG, "Failed to load message from Outbox.", e);
            }
        } catch (RuntimeException e) {
            if (LOCAL_LOGV) {
                Log.e(TAG, "Unexpected RuntimeException.", e);
            }
        } finally {
            if (mTransactionState.getState() != TransactionState.SUCCESS) {
                mTransactionState.setState(TransactionState.FAILED);
                mTransactionState.setContentUri(mReadReportURI);
            }
            notifyObservers();
        }
    }

    @Override
    public int getType() {
        return READREC_TRANSACTION;
    }
}
