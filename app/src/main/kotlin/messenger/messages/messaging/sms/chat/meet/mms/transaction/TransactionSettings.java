package messenger.messages.messaging.sms.chat.meet.mms.transaction;

import android.content.Context;
import messenger.messages.messaging.sms.chat.meet.android.net.NetworkUtilsHelper;
import android.provider.Telephony;
import android.text.TextUtils;
import android.util.Log;

import messenger.messages.messaging.sms.chat.meet.send_message.Transaction;
import messenger.messages.messaging.sms.chat.meet.send_message.Utils;

public class TransactionSettings {
    private static final String TAG = "loggg";
    private static final boolean DEBUG = true;
    private static final boolean LOCAL_LOGV = false;

    private String mServiceCenter;
    private String mProxyAddress;
    private int mProxyPort = -1;

    private static final String[] APN_PROJECTION = {
            Telephony.Carriers.TYPE,            // 0
            Telephony.Carriers.MMSC,            // 1
            Telephony.Carriers.MMSPROXY,        // 2
            Telephony.Carriers.MMSPORT          // 3
    };
    private static final int COLUMN_TYPE         = 0;
    private static final int COLUMN_MMSC         = 1;
    private static final int COLUMN_MMSPROXY     = 2;
    private static final int COLUMN_MMSPORT      = 3;

    public TransactionSettings(Context context, String apnName) {
        Log.v(TAG, "TransactionSettings: apnName: " + apnName);
        if (Transaction.settings == null) {
            Transaction.settings = Utils.getDefaultSendSettings(context);
        }

        mServiceCenter = NetworkUtilsHelper.trimV4AddrZeros(Transaction.settings.getMmsc());
        mProxyAddress = NetworkUtilsHelper.trimV4AddrZeros(Transaction.settings.getProxy());

        // Set up the agent, profile url and tag name to be used in the mms request if they are attached in settings
        String agent = Transaction.settings.getAgent();
        if (agent != null && !agent.trim().equals("")) {
//            MmsConfig.setUserAgent(agent);
            Log.v(TAG, "set user agent");
        }

        String uaProfUrl = Transaction.settings.getUserProfileUrl();
        if (uaProfUrl != null && !uaProfUrl.trim().equals("")) {
//            MmsConfig.setUaProfUrl(uaProfUrl);
            Log.v(TAG, "set user agent profile url");
        }

        String uaProfTagName = Transaction.settings.getUaProfTagName();
        if (uaProfTagName != null && !uaProfTagName.trim().equals("")) {
//            MmsConfig.setUaProfTagName(uaProfTagName);
            Log.v(TAG, "set user agent profile tag name");
        }

        if (isProxySet()) {
            try {
                mProxyPort = Integer.parseInt(Transaction.settings.getPort());
            } catch (NumberFormatException e) {
                Log.e(TAG, "could not get proxy: " + Transaction.settings.getPort(), e);
            }
        }
//        }
    }

    public TransactionSettings(String mmscUrl, String proxyAddr, int proxyPort) {
        mServiceCenter = mmscUrl != null ? mmscUrl.trim() : null;
        mProxyAddress = proxyAddr;
        mProxyPort = proxyPort;
   }

    public String getMmscUrl() {
        return mServiceCenter;
    }

    public String getProxyAddress() {
        return mProxyAddress;
    }

    public int getProxyPort() {
        return mProxyPort;
    }

    public boolean isProxySet() {
        return (mProxyAddress != null) && (mProxyAddress.trim().length() != 0);
    }

    static private boolean isValidApnType(String types, String requestType) {
        // If APN type is unspecified, assume APN_TYPE_ALL.
        if (TextUtils.isEmpty(types)) {
            return true;
        }

        for (String t : types.split(",")) {
            if (t.equals(requestType) || t.equals("*")) {
                return true;
            }
        }
        return false;
    }
}
