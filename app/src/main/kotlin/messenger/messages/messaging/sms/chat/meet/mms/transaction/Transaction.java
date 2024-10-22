package messenger.messages.messaging.sms.chat.meet.mms.transaction;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;

import messenger.messages.messaging.sms.chat.meet.android.mms.MmsException;
import messenger.messages.messaging.sms.chat.meet.send_message.Utils;

public abstract class Transaction extends Observable {
    private final int mServiceId;

    protected Context mContext;
    protected String mId;
    protected TransactionState mTransactionState;
    protected TransactionSettings mTransactionSettings;
    public static final int NOTIFICATION_TRANSACTION = 0;
    public static final int RETRIEVE_TRANSACTION     = 1;
    public static final int SEND_TRANSACTION         = 2;
    public static final int READREC_TRANSACTION      = 3;

    public Transaction(Context context, int serviceId,
            TransactionSettings settings) {
        mContext = context;
        mTransactionState = new TransactionState();
        mServiceId = serviceId;
        mTransactionSettings = settings;
    }
    @Override
    public TransactionState getState() {
        return mTransactionState;
    }

    public abstract void process();

    public boolean isEquivalent(Transaction transaction) {
        return mId.equals(transaction.mId);
    }

    public int getServiceId() {
        return mServiceId;
    }

    public TransactionSettings getConnectionSettings() {
        return mTransactionSettings;
    }
    public void setConnectionSettings(TransactionSettings settings) {
        mTransactionSettings = settings;
    }

    public static boolean useWifi(Context context) {
        if (Utils.isMmsOverWifiEnabled(context)) {
            ConnectivityManager mConnMgr = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
            if (mConnMgr != null) {
                NetworkInfo niWF = mConnMgr.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
                if ((niWF != null) && (niWF.isConnected())) {
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public String toString() {
        return getClass().getName() + ": serviceId=" + mServiceId;
    }


    abstract public int getType();
}
