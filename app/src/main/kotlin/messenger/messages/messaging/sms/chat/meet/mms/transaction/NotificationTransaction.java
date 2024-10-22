package messenger.messages.messaging.sms.chat.meet.mms.transaction;

import android.app.Service;
import android.content.Context;
import android.net.Uri;
import android.os.Looper;
import android.preference.PreferenceManager;
import android.provider.Telephony.Mms.Inbox;
import android.telephony.TelephonyManager;


import messenger.messages.messaging.sms.chat.meet.mms.util.DownloadManager;
import messenger.messages.messaging.sms.chat.meet.android.mms.MmsException;
import messenger.messages.messaging.sms.chat.meet.android.mms.pdu_alt.NotificationInd;
import messenger.messages.messaging.sms.chat.meet.android.mms.pdu_alt.NotifyRespInd;
import messenger.messages.messaging.sms.chat.meet.android.mms.pdu_alt.PduHeaders;
import messenger.messages.messaging.sms.chat.meet.android.mms.pdu_alt.PduPersister;

import android.util.Log;

import messenger.messages.messaging.sms.chat.meet.send_message.Settings;

import java.io.IOException;

import static messenger.messages.messaging.sms.chat.meet.android.mms.pdu_alt.PduHeaders.STATUS_DEFERRED;

public class NotificationTransaction extends Transaction implements Runnable {
    private static final String TAG = "loggg";
    private static final boolean DEBUG = false;
    private static final boolean LOCAL_LOGV = false;

    private Uri mUri;
    private NotificationInd mNotificationInd;
    private String mContentLocation;

    public NotificationTransaction(
            Context context, int serviceId,
            TransactionSettings connectionSettings, String uriString) {
        super(context, serviceId, connectionSettings);

        mUri = Uri.parse(uriString);

        try {
            mNotificationInd = (NotificationInd)
                    PduPersister.getPduPersister(context).load(mUri);
        } catch (MmsException e) {
            Log.e(TAG, "Failed to load NotificationInd from: " + uriString, e);
            throw new IllegalArgumentException();
        }

        mContentLocation = new String(mNotificationInd.getContentLocation());
        mId = mContentLocation;

        // Attach the transaction to the instance of RetryScheduler.
        attach(RetryScheduler.getInstance(context));
    }

    public NotificationTransaction(
            Context context, int serviceId,
            TransactionSettings connectionSettings, NotificationInd ind) {
        super(context, serviceId, connectionSettings);

        try {
            // Save the pdu. If we can start downloading the real pdu immediately, don't allow
            // persist() to create a thread for the notificationInd because it causes UI jank.
            boolean group;
            int subId = Settings.DEFAULT_SUBSCRIPTION_ID;

            try {
                group = messenger.messages.messaging.sms.chat.meet.send_message.Transaction.settings.getGroup();
                subId = messenger.messages.messaging.sms.chat.meet.send_message.Transaction.settings.getSubscriptionId();
            } catch (Exception e) {
                group = PreferenceManager.getDefaultSharedPreferences(context).getBoolean("group_message", true);
            }
            mUri = PduPersister.getPduPersister(context).persist(
                        ind, Inbox.CONTENT_URI, !allowAutoDownload(mContext),
                        group, null, subId);
        } catch (MmsException e) {
            Log.e(TAG, "Failed to save NotificationInd in constructor.", e);
            throw new IllegalArgumentException();
        }

        mNotificationInd = ind;
        mId = new String(mNotificationInd.getContentLocation());
    }

    @Override
    public void process() {
        new Thread(this, "NotificationTransaction").start();
    }

    public static boolean allowAutoDownload(Context context) {
        try { Looper.prepare(); } catch (Exception e) { }
        boolean autoDownload = PreferenceManager.getDefaultSharedPreferences(context).getBoolean("auto_download_mms", true);
        boolean dataSuspended = (((TelephonyManager) context.getSystemService(Service.TELEPHONY_SERVICE)).getDataState() ==
                TelephonyManager.DATA_SUSPENDED);
        return autoDownload && !dataSuspended;
    }

    public void run() {
        try { Looper.prepare(); } catch (Exception e) {}
        DownloadManager.init(mContext);
        DownloadManager downloadManager = DownloadManager.getInstance();
        boolean autoDownload = allowAutoDownload(mContext);
        try {
            if (LOCAL_LOGV) {
                Log.v(TAG, "Notification transaction launched: " + this);
            }
            int status = STATUS_DEFERRED;
            if (!autoDownload) {
                downloadManager.markState(mUri, DownloadManager.STATE_UNSTARTED);
                sendNotifyRespInd(status);
                return;
            }

            downloadManager.markState(mUri, DownloadManager.STATE_DOWNLOADING);

            if (LOCAL_LOGV) {
                Log.v(TAG, "Content-Location: " + mContentLocation);
            }

            mTransactionState.setState(TransactionState.FAILED);
        } catch (Throwable t) {
            Log.e(TAG, "error", t);
        } finally {
            mTransactionState.setContentUri(mUri);
            if (!autoDownload) {
                // Always mark the transaction successful for deferred
                // download since any error here doesn't make sense.
                mTransactionState.setState(TransactionState.SUCCESS);
            }
            if (mTransactionState.getState() != TransactionState.SUCCESS) {
                mTransactionState.setState(TransactionState.FAILED);
                Log.e(TAG, "NotificationTransaction failed.");
            }
            notifyObservers();
        }
    }

    private void sendNotifyRespInd(int status) throws MmsException, IOException {
        // Create the M-NotifyResp.ind
        NotifyRespInd notifyRespInd = new NotifyRespInd(
                PduHeaders.CURRENT_MMS_VERSION,
                mNotificationInd.getTransactionId(),
                status);

        // Pack M-NotifyResp.ind and send it
    }

    @Override
    public int getType() {
        return NOTIFICATION_TRANSACTION;
    }
}
