package messenger.messages.messaging.sms.chat.meet.mms.transaction;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import messenger.messages.messaging.sms.chat.meet.android.database.SqliteWrapper;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.PowerManager;
import android.preference.PreferenceManager;
import android.provider.Telephony.Mms;
import android.provider.Telephony.MmsSms;
import android.provider.Telephony.MmsSms.PendingMessages;
import android.text.TextUtils;
import android.widget.Toast;


import messenger.messages.messaging.sms.chat.meet.mms.util.DownloadManager;
import messenger.messages.messaging.sms.chat.meet.mms.util.RateController;
import messenger.messages.messaging.sms.chat.meet.android.mms.MmsException;
import messenger.messages.messaging.sms.chat.meet.android.mms.pdu_alt.GenericPdu;
import messenger.messages.messaging.sms.chat.meet.android.mms.pdu_alt.NotificationInd;
import messenger.messages.messaging.sms.chat.meet.android.mms.pdu_alt.PduHeaders;
import messenger.messages.messaging.sms.chat.meet.android.mms.pdu_alt.PduParser;
import messenger.messages.messaging.sms.chat.meet.android.mms.pdu_alt.PduPersister;
import android.util.Log;
import messenger.messages.messaging.sms.chat.meet.send_message.BroadcastUtils;
import messenger.messages.messaging.sms.chat.meet.R;
import messenger.messages.messaging.sms.chat.meet.send_message.Settings;
import messenger.messages.messaging.sms.chat.meet.send_message.Utils;

import java.io.IOException;
import java.util.ArrayList;

public class TransactionService extends Service implements Observer {
    private static final String TAG = "loggg";
    public static final String TRANSACTION_COMPLETED_ACTION =
            "android.intent.action.TRANSACTION_COMPLETED_ACTION";

    public static final String ACTION_ONALARM = "android.intent.action.ACTION_ONALARM";

    public static final String ACTION_ENABLE_AUTO_RETRIEVE
            = "android.intent.action.ACTION_ENABLE_AUTO_RETRIEVE";

    public static final String STATE = "state";

    public static final String STATE_URI = "uri";

    private static final int EVENT_TRANSACTION_REQUEST = 1;
    private static final int EVENT_CONTINUE_MMS_CONNECTIVITY = 3;
    private static final int EVENT_HANDLE_NEXT_PENDING_TRANSACTION = 4;
    private static final int EVENT_NEW_INTENT = 5;
    private static final int EVENT_QUIT = 100;

    private static final int TOAST_MSG_QUEUED = 1;
    private static final int TOAST_DOWNLOAD_LATER = 2;
    private static final int TOAST_NO_APN = 3;
    private static final int TOAST_NONE = -1;
    private static final int APN_EXTENSION_WAIT = 30 * 1000;

    private ServiceHandler mServiceHandler;
    private Looper mServiceLooper;
    private final ArrayList<Transaction> mProcessing  = new ArrayList<Transaction>();
    private final ArrayList<Transaction> mPending  = new ArrayList<Transaction>();
    private ConnectivityManager mConnMgr;
    private ConnectivityBroadcastReceiver mReceiver;
    private boolean mobileDataEnabled;
    private boolean lollipopReceiving = false;

    private PowerManager.WakeLock mWakeLock;

    public Handler mToastHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            String str = null;

            if (msg.what == TOAST_MSG_QUEUED) {
                str = getString(R.string.message_queued);
            } else if (msg.what == TOAST_DOWNLOAD_LATER) {
                str = getString(R.string.download_later);
            } else if (msg.what == TOAST_NO_APN) {
                str = getString(R.string.no_apn);
            }

            if (str != null) {
                Toast.makeText(TransactionService.this, str,
                        Toast.LENGTH_LONG).show();
            }
        }
    };

    @Override
    public void onCreate() {
        if (!Utils.isDefaultSmsApp(this)) {
            Log.v(TAG, "not default app, so exiting");
            stopSelf();
            return;
        }

        initServiceHandler();

        mReceiver = new ConnectivityBroadcastReceiver();
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(ConnectivityManager.CONNECTIVITY_ACTION);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            registerReceiver(mReceiver, intentFilter,RECEIVER_EXPORTED);
        }else {
            registerReceiver(mReceiver, intentFilter);
        }
    }

    private void initServiceHandler() {
        HandlerThread thread = new HandlerThread("TransactionService");
        thread.start();

        mServiceLooper = thread.getLooper();
        mServiceHandler = new ServiceHandler(mServiceLooper);
    }

    @Override
    public int onStartCommand(final Intent intent, int flags, int startId) {
        if (intent != null) {
            if (mServiceHandler == null) {
                initServiceHandler();
            }

            Message msg = mServiceHandler.obtainMessage(EVENT_NEW_INTENT);
            msg.arg1 = startId;
            msg.obj = intent;
            mServiceHandler.sendMessage(msg);
        }
        return Service.START_NOT_STICKY;
    }

    private boolean isNetworkAvailable() {
        if (mConnMgr == null) {
            return false;
        } else if (Utils.isMmsOverWifiEnabled(this)) {
            NetworkInfo niWF = mConnMgr.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
            return (niWF == null ? false : niWF.isConnected());
        } else {
            NetworkInfo ni = mConnMgr.getNetworkInfo(ConnectivityManager.TYPE_MOBILE_MMS);
            return (ni == null ? false : ni.isAvailable());
        }
    }

    public void onNewIntent(Intent intent, int serviceId) {
        try {
            mobileDataEnabled = Utils.isMobileDataEnabled(this);
        } catch (Exception e) {
            mobileDataEnabled = true;
        }

        mConnMgr = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        if (!mobileDataEnabled) {
            Utils.setMobileDataEnabled(this, true);
        }

        if (mConnMgr == null) {
            endMmsConnectivity();
            stopSelf(serviceId);
            return;
        }

        boolean noNetwork = !isNetworkAvailable();



        String action = intent.getAction();
        if (ACTION_ONALARM.equals(action) || ACTION_ENABLE_AUTO_RETRIEVE.equals(action) ||
                (intent.getExtras() == null)) {
            // Scan database to find all pending operations.
            Cursor cursor = PduPersister.getPduPersister(this).getPendingMessages(
                    System.currentTimeMillis());
            if (cursor != null) {
                try {
                    int count = cursor.getCount();



                    if (count == 0) {

                        RetryScheduler.setRetryAlarm(this);
                        stopSelfIfIdle(serviceId);
                        return;
                    }

                    int columnIndexOfMsgId = cursor.getColumnIndexOrThrow(PendingMessages.MSG_ID);
                    int columnIndexOfMsgType = cursor.getColumnIndexOrThrow(
                            PendingMessages.MSG_TYPE);

                    while (cursor.moveToNext()) {
                        int msgType = cursor.getInt(columnIndexOfMsgType);
                        int transactionType = getTransactionType(msgType);

                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                            boolean useSystem = true;
                            int subId = Settings.DEFAULT_SUBSCRIPTION_ID;
                            if (messenger.messages.messaging.sms.chat.meet.send_message.Transaction.settings != null) {
                                useSystem = messenger.messages.messaging.sms.chat.meet.send_message.Transaction.settings
                                        .getUseSystemSending();
                                subId = messenger.messages.messaging.sms.chat.meet.send_message.Transaction.settings.getSubscriptionId();
                            } else {
                                useSystem = PreferenceManager.getDefaultSharedPreferences(this)
                                        .getBoolean("system_mms_sending", useSystem);
                            }

                            if (useSystem) {
                                try {
                                    Uri uri = ContentUris.withAppendedId(Mms.CONTENT_URI,
                                            cursor.getLong(columnIndexOfMsgId));
                                    messenger.messages.messaging.sms.chat.meet.mms.transaction.DownloadManager.getInstance().
                                            downloadMultimediaMessage(this, PushReceiver.getContentLocation(this, uri), uri, false, subId);

                                    // can't handle many messages at once.
                                    break;
                                } catch (MmsException e) {
                                    e.printStackTrace();
                                }
                            } else {

                            }
                            continue;
                        }


                        if (noNetwork) {
                            onNetworkUnavailable(serviceId, transactionType);
                            return;
                        }
                        switch (transactionType) {
                            case -1:
                                break;
                            case Transaction.RETRIEVE_TRANSACTION:
                                int failureType = cursor.getInt(
                                        cursor.getColumnIndexOrThrow(
                                                PendingMessages.ERROR_TYPE));
                                try {
                                    DownloadManager.init(this);
                                    DownloadManager downloadManager = DownloadManager.getInstance();
                                    boolean autoDownload = downloadManager.isAuto();
                                    if (!autoDownload) {

                                        Uri uri = ContentUris.withAppendedId(Mms.CONTENT_URI,
                                                cursor.getLong(columnIndexOfMsgId));
                                        downloadManager.markState(uri,
                                                DownloadManager.STATE_SKIP_RETRYING);
                                        break;
                                    }
                                } catch (Exception e) {
                                    e.printStackTrace();
                                }

                                if (!(failureType == MmsSms.NO_ERROR ||
                                        isTransientFailure(failureType))) {

                                    break;
                                }

                            default:
                                Uri uri = ContentUris.withAppendedId(
                                        Mms.CONTENT_URI,
                                        cursor.getLong(columnIndexOfMsgId));
                                TransactionBundle args = new TransactionBundle(
                                        transactionType, uri.toString());
                                // FIXME: We use the same startId for all MMs.

                                launchTransaction(serviceId, args, false);
                                break;
                        }
                    }
                } finally {
                    cursor.close();
                }
            } else {

                RetryScheduler.setRetryAlarm(this);
                stopSelfIfIdle(serviceId);
            }
        } else {

            TransactionBundle args = new TransactionBundle(intent.getExtras());
            launchTransaction(serviceId, args, noNetwork);
        }
    }

    private void stopSelfIfIdle(int startId) {
        synchronized (mProcessing) {
            if (mProcessing.isEmpty() && mPending.isEmpty()) {

                stopSelf(startId);
            }
        }
    }

    private static boolean isTransientFailure(int type) {
        return type > MmsSms.NO_ERROR && type < MmsSms.ERR_TYPE_GENERIC_PERMANENT;
    }

    private int getTransactionType(int msgType) {
        switch (msgType) {
            case PduHeaders.MESSAGE_TYPE_NOTIFICATION_IND:
                return Transaction.RETRIEVE_TRANSACTION;
            case PduHeaders.MESSAGE_TYPE_READ_REC_IND:
                return Transaction.READREC_TRANSACTION;
            case PduHeaders.MESSAGE_TYPE_SEND_REQ:
                return Transaction.SEND_TRANSACTION;
            default:
                Log.w(TAG, "Unrecognized MESSAGE_TYPE: " + msgType);
                return -1;
        }
    }

    private void launchTransaction(int serviceId, TransactionBundle txnBundle, boolean noNetwork) {
        if (noNetwork) {
            Log.w(TAG, "launchTransaction: no network error!");
            onNetworkUnavailable(serviceId, txnBundle.getTransactionType());
            return;
        }
        Message msg = mServiceHandler.obtainMessage(EVENT_TRANSACTION_REQUEST);
        msg.arg1 = serviceId;
        msg.obj = txnBundle;


        mServiceHandler.sendMessage(msg);
    }

    private void onNetworkUnavailable(int serviceId, int transactionType) {

        int toastType = TOAST_NONE;
        if (transactionType == Transaction.RETRIEVE_TRANSACTION) {
            toastType = TOAST_DOWNLOAD_LATER;
        } else if (transactionType == Transaction.SEND_TRANSACTION) {
            toastType = TOAST_MSG_QUEUED;
        }
        if (toastType != TOAST_NONE) {
            mToastHandler.sendEmptyMessage(toastType);
        }
        stopSelf(serviceId);
    }

    @Override
    public void onDestroy() {

        if (!mPending.isEmpty()) {
            Log.w(TAG, "TransactionService exiting with transaction still pending");
        }

        releaseWakeLock();

        try {
            unregisterReceiver(mReceiver);
        } catch (Exception e) {
        }

        mServiceHandler.sendEmptyMessage(EVENT_QUIT);

        if (!mobileDataEnabled && !lollipopReceiving) {
            Log.v(TAG, "disabling mobile data");
            Utils.setMobileDataEnabled(TransactionService.this, false);
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }


    public void update(Observable observable) {
        Transaction transaction = (Transaction) observable;
        int serviceId = transaction.getServiceId();

        try {
            synchronized (mProcessing) {
                mProcessing.remove(transaction);
                if (mPending.size() > 0) {

                    Message msg = mServiceHandler.obtainMessage(
                            EVENT_HANDLE_NEXT_PENDING_TRANSACTION,
                            transaction.getConnectionSettings());
                    mServiceHandler.sendMessage(msg);
                }
                else if (mProcessing.isEmpty()) {

                    endMmsConnectivity();
                } else {

                }
            }

            Intent intent = new Intent(TRANSACTION_COMPLETED_ACTION);
            TransactionState state = transaction.getState();
            int result = state.getState();
            intent.putExtra(STATE, result);

            switch (result) {
                case TransactionState.SUCCESS:


                    intent.putExtra(STATE_URI, state.getContentUri());

                    switch (transaction.getType()) {
                        case Transaction.NOTIFICATION_TRANSACTION:
                        case Transaction.RETRIEVE_TRANSACTION:

                            break;
                        case Transaction.SEND_TRANSACTION:
                            RateController.init(getApplicationContext());
                            RateController.getInstance().update();
                            break;
                    }
                    break;
                case TransactionState.FAILED:

                    break;
                default:

                    break;
            }


            BroadcastUtils.sendExplicitBroadcast(this, intent, TRANSACTION_COMPLETED_ACTION);
        } finally {
            transaction.detach(this);
            stopSelfIfIdle(serviceId);
        }
    }

    private synchronized void createWakeLock() {
        // Create a new wake lock if we haven't made one yet.
        if (mWakeLock == null) {
            PowerManager pm = (PowerManager)getSystemService(Context.POWER_SERVICE);
            mWakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "MMS Connectivity");
            mWakeLock.setReferenceCounted(false);
        }
    }

    private void acquireWakeLock() {
        Log.v(TAG, "mms acquireWakeLock");
        mWakeLock.acquire();
    }

    private void releaseWakeLock() {
        if (mWakeLock != null && mWakeLock.isHeld()) {
            Log.v(TAG, "mms releaseWakeLock");
            mWakeLock.release();
        }
    }

    protected int beginMmsConnectivity() /*throws IOException*/ {
        createWakeLock();

        if (Utils.isMmsOverWifiEnabled(this)) {
            NetworkInfo niWF = mConnMgr.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
            if ((niWF != null) && (niWF.isConnected())) {
                Log.v(TAG, "beginMmsConnectivity: Wifi active");
                return 0;
            }
        }
        return 0;
    }

    protected void endMmsConnectivity() {
        try {
            mServiceHandler.removeMessages(EVENT_CONTINUE_MMS_CONNECTIVITY);
            if (mConnMgr != null && Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            }
        } finally {
            releaseWakeLock();
        }
    }

    private final class ServiceHandler extends Handler {
        public ServiceHandler(Looper looper) {
            super(looper);
        }

        private String decodeMessage(Message msg) {
            if (msg.what == EVENT_QUIT) {
                return "EVENT_QUIT";
            } else if (msg.what == EVENT_CONTINUE_MMS_CONNECTIVITY) {
                return "EVENT_CONTINUE_MMS_CONNECTIVITY";
            } else if (msg.what == EVENT_TRANSACTION_REQUEST) {
                return "EVENT_TRANSACTION_REQUEST";
            } else if (msg.what == EVENT_HANDLE_NEXT_PENDING_TRANSACTION) {
                return "EVENT_HANDLE_NEXT_PENDING_TRANSACTION";
            } else if (msg.what == EVENT_NEW_INTENT) {
                return "EVENT_NEW_INTENT";
            }
            return "unknown message.what";
        }

        private String decodeTransactionType(int transactionType) {
            if (transactionType == Transaction.NOTIFICATION_TRANSACTION) {
                return "NOTIFICATION_TRANSACTION";
            } else if (transactionType == Transaction.RETRIEVE_TRANSACTION) {
                return "RETRIEVE_TRANSACTION";
            } else if (transactionType == Transaction.SEND_TRANSACTION) {
                return "SEND_TRANSACTION";
            } else if (transactionType == Transaction.READREC_TRANSACTION) {
                return "READREC_TRANSACTION";
            }
            return "invalid transaction type";
        }

        @Override
        public void handleMessage(Message msg) {
            Transaction transaction = null;

            switch (msg.what) {
                case EVENT_NEW_INTENT:
                    onNewIntent((Intent)msg.obj, msg.arg1);
                    break;

                case EVENT_QUIT:
                    getLooper().quit();
                    return;

                case EVENT_CONTINUE_MMS_CONNECTIVITY:
                    synchronized (mProcessing) {
                        if (mProcessing.isEmpty()) {
                            return;
                        }
                    }

//                    try {
                        int result = beginMmsConnectivity();
                        if (result != 0) {
                            Log.v(TAG, "Extending MMS connectivity returned " + result +
                                    " instead of APN_ALREADY_ACTIVE");
                            // Just wait for connectivity startup without
                            // any new request of APN switch.
                            return;
                        }


                    // Restart timer
                    renewMmsConnectivity();
                    return;

                case EVENT_TRANSACTION_REQUEST:
                    int serviceId = msg.arg1;
                    try {
                        TransactionBundle args = (TransactionBundle) msg.obj;
                        TransactionSettings transactionSettings;


                        String mmsc = args.getMmscUrl();
                        if (mmsc != null) {
                            transactionSettings = new TransactionSettings(
                                    mmsc, args.getProxyAddress(), args.getProxyPort());
                        } else {
                            transactionSettings = new TransactionSettings(
                                                    TransactionService.this, null);
                        }

                        int transactionType = args.getTransactionType();


                        switch (transactionType) {
                            case Transaction.NOTIFICATION_TRANSACTION:
                                String uri = args.getUri();
                                if (uri != null) {
                                    transaction = new NotificationTransaction(
                                            TransactionService.this, serviceId,
                                            transactionSettings, uri);
                                } else {
                                    // Now it's only used for test purpose.
                                    byte[] pushData = args.getPushData();
                                    PduParser parser = new PduParser(pushData);
                                    GenericPdu ind = parser.parse();

                                    int type = PduHeaders.MESSAGE_TYPE_NOTIFICATION_IND;
                                    if ((ind != null) && (ind.getMessageType() == type)) {
                                        transaction = new NotificationTransaction(
                                                TransactionService.this, serviceId,
                                                transactionSettings, (NotificationInd) ind);
                                    } else {
                                        Log.e(TAG, "Invalid PUSH data.");
                                        transaction = null;
                                        return;
                                    }
                                }
                                break;
                            case Transaction.RETRIEVE_TRANSACTION:
                                transaction = new RetrieveTransaction(
                                        TransactionService.this, serviceId,
                                        transactionSettings, args.getUri());

                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                                    Uri u = Uri.parse(args.getUri());
                                    messenger.messages.messaging.sms.chat.meet.mms.transaction.DownloadManager.getInstance().
                                            downloadMultimediaMessage(TransactionService.this,
                                                    ((RetrieveTransaction) transaction).getContentLocation(TransactionService.this, u), u, false, Settings.DEFAULT_SUBSCRIPTION_ID);
                                    return;
                                }

                                break;
                            case Transaction.SEND_TRANSACTION:
                                transaction = new SendTransaction(
                                        TransactionService.this, serviceId,
                                        transactionSettings, args.getUri());
                                break;
                            case Transaction.READREC_TRANSACTION:
                                transaction = new ReadRecTransaction(
                                        TransactionService.this, serviceId,
                                        transactionSettings, args.getUri());
                                break;
                            default:
                                Log.w(TAG, "Invalid transaction type: " + serviceId);
                                transaction = null;
                                return;
                        }

                        if (!processTransaction(transaction)) {
                            transaction = null;
                            return;
                        }

                    } catch (Exception ex) {
                        Log.w(TAG, "Exception occurred while handling message: " + msg, ex);

                        if (transaction != null) {
                            try {
                                transaction.detach(TransactionService.this);
                                if (mProcessing.contains(transaction)) {
                                    synchronized (mProcessing) {
                                        mProcessing.remove(transaction);
                                    }
                                }
                            } catch (Throwable t) {
                                Log.e(TAG, "Unexpected Throwable.", t);
                            } finally {
                                // Set transaction to null to allow stopping the
                                // transaction service.
                                transaction = null;
                            }
                        }
                    } finally {
                        if (transaction == null) {
                          /*  if (Log.isLoggable(LogTag.TRANSACTION, Log.VERBOSE)) {
                                Log.v(TAG, "Transaction was null. Stopping self: " + serviceId);
                            }*/
                            endMmsConnectivity();
                            stopSelf(serviceId);
                        }
                    }
                    return;
                case EVENT_HANDLE_NEXT_PENDING_TRANSACTION:
                    processPendingTransaction(transaction, (TransactionSettings) msg.obj);
                    return;
                default:
                    Log.w(TAG, "what=" + msg.what);
                    return;
            }
        }

        public void markAllPendingTransactionsAsFailed() {
            synchronized (mProcessing) {
                while (mPending.size() != 0) {
                    Transaction transaction = mPending.remove(0);
                    transaction.mTransactionState.setState(TransactionState.FAILED);
                    if (transaction instanceof SendTransaction) {
                        Uri uri = ((SendTransaction)transaction).mSendReqURI;
                        transaction.mTransactionState.setContentUri(uri);
                        int respStatus = PduHeaders.RESPONSE_STATUS_ERROR_NETWORK_PROBLEM;
                        ContentValues values = new ContentValues(1);
                        values.put(Mms.RESPONSE_STATUS, respStatus);

                        SqliteWrapper.update(TransactionService.this,
                                TransactionService.this.getContentResolver(),
                                uri, values, null, null);
                    }
                    transaction.notifyObservers();
                }
            }
        }

        public void processPendingTransaction(Transaction transaction,
                                               TransactionSettings settings) {
            int numProcessTransaction = 0;
            synchronized (mProcessing) {
                if (mPending.size() != 0) {
                    transaction = mPending.remove(0);
                }
                numProcessTransaction = mProcessing.size();
            }

            if (transaction != null) {
                if (settings != null) {
                    transaction.setConnectionSettings(settings);
                }

                try {
                    int serviceId = transaction.getServiceId();

                    if (processTransaction(transaction)) {
                    } else {
                        transaction = null;
                        stopSelf(serviceId);
                    }
                } catch (IOException e) {
                    Log.w(TAG, e.getMessage(), e);
                }
            } else {
                if (numProcessTransaction == 0) {
                    endMmsConnectivity();
                }
            }
        }

        private boolean processTransaction(Transaction transaction) throws IOException {
            // Check if transaction already processing
            synchronized (mProcessing) {
                for (Transaction t : mPending) {
                    if (t.isEquivalent(transaction)) {
                        return true;
                    }
                }
                for (Transaction t : mProcessing) {
                    if (t.isEquivalent(transaction)) {
                        return true;
                    }
                }

                int connectivityResult = beginMmsConnectivity();
                if (connectivityResult == 1) {
                    mPending.add(transaction);
                    return true;
                }
                if (mProcessing.size() > 0) {
                    mPending.add(transaction);
                    return true;
                } else {
                    mProcessing.add(transaction);
               }
            }

            sendMessageDelayed(obtainMessage(EVENT_CONTINUE_MMS_CONNECTIVITY),
                               APN_EXTENSION_WAIT);

            transaction.attach(TransactionService.this);
            transaction.process();
            return true;
        }
    }

    private void renewMmsConnectivity() {
        // Set a timer to keep renewing our "lease" on the MMS connection
        mServiceHandler.sendMessageDelayed(
                mServiceHandler.obtainMessage(EVENT_CONTINUE_MMS_CONNECTIVITY),
                           APN_EXTENSION_WAIT);
    }

    private class ConnectivityBroadcastReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (!action.equals(ConnectivityManager.CONNECTIVITY_ACTION)) {
                return;
            }

            NetworkInfo mmsNetworkInfo = null;

            if (mConnMgr != null && Utils.isMobileDataEnabled(context)) {
                mmsNetworkInfo = mConnMgr.getNetworkInfo(ConnectivityManager.TYPE_MOBILE_MMS);
            } else {
            }

            // Check availability of the mobile network.
            if (mmsNetworkInfo == null) {

            } else {
                // This is a very specific fix to handle the case where the phone receives an
                // incoming call during the time we're trying to setup the mms connection.
                // When the call ends, restart the process of mms connectivity.
                if ("2GVoiceCallEnded".equals(mmsNetworkInfo.getReason())) {
                   /* if (Log.isLoggable(LogTag.TRANSACTION, Log.VERBOSE)) {
                        Log.v(TAG, "   reason is " + "2GVoiceCallEnded" +
                                ", retrying mms connectivity");
                    }*/
                    renewMmsConnectivity();
                    return;
                }

                if (mmsNetworkInfo.isConnected()) {
                    TransactionSettings settings = new TransactionSettings(
                            TransactionService.this, mmsNetworkInfo.getExtraInfo());
                    // If this APN doesn't have an MMSC, mark everything as failed and bail.
                    if (TextUtils.isEmpty(settings.getMmscUrl())) {
                        Log.v(TAG, "   empty MMSC url, bail");
                        BroadcastUtils.sendExplicitBroadcast(
                                TransactionService.this,
                                new Intent(),
                                messenger.messages.messaging.sms.chat.meet.send_message.Transaction.MMS_ERROR);
                        mServiceHandler.markAllPendingTransactionsAsFailed();
                        endMmsConnectivity();
                        stopSelf();
                        return;
                    }
                    mServiceHandler.processPendingTransaction(null, settings);
                } else {
                   /* if (Log.isLoggable("TRANSACTION", Log.VERBOSE)) {
                        Log.v(TAG, "   TYPE_MOBILE_MMS not connected, bail");
                    }*/

                    // Retry mms connectivity once it's possible to connect
                    if (mmsNetworkInfo.isAvailable()) {
                        /*if (Log.isLoggable("TRANSACTION", Log.VERBOSE)) {
                            Log.v(TAG, "   retrying mms connectivity for it's available");
                        }*/
                        renewMmsConnectivity();
                    }
                }
            }
        }
    }
}
