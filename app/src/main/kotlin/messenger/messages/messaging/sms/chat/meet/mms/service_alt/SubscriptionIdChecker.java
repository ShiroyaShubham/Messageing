
package messenger.messages.messaging.sms.chat.meet.mms.service_alt;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteException;
import android.os.Build;
import android.provider.Telephony;
import android.util.Log;

import messenger.messages.messaging.sms.chat.meet.android.mms.util_alt.SqliteWrapper;

public class SubscriptionIdChecker {
    private static final String TAG = "SubscriptionIdChecker";

    private static SubscriptionIdChecker sInstance;
    private boolean mCanUseSubscriptionId = false;

    private void check(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP_MR1) {
            Cursor c = null;
            try {
                c = SqliteWrapper.query(context, context.getContentResolver(),
                        Telephony.Mms.CONTENT_URI,
                        new String[]{Telephony.Mms.SUBSCRIPTION_ID}, null, null, null);
                if (c != null) {
                    mCanUseSubscriptionId = true;
                }
            } catch (SQLiteException e) {
                Log.e(TAG, "SubscriptionIdChecker.check() fail");
            } finally {
                if (c != null) {
                    c.close();
                }
            }
        }
    }

    public static synchronized SubscriptionIdChecker getInstance(Context context) {
        if (sInstance == null) {
            sInstance = new SubscriptionIdChecker();
            sInstance.check(context);
        }
        return sInstance;
    }

    public boolean canUseSubscriptionId() {
        return mCanUseSubscriptionId;
    }
}
