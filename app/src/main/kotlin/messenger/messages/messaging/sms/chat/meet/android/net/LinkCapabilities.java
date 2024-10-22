package messenger.messages.messaging.sms.chat.meet.android.net;

import android.net.ConnectivityManager;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.Log;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

public class LinkCapabilities implements Parcelable {
    private static final String TAG = "LinkCapabilities";
    private static final boolean DBG = false;

    private HashMap<Integer, String> mCapabilities;


    public static final class Key {

        private Key() {
        }

        public final static int RO_NETWORK_TYPE = 1;

        public final static int RW_DESIRED_FWD_BW = 2;

        public final static int RW_REQUIRED_FWD_BW = 3;

        public final static int RO_AVAILABLE_FWD_BW = 4;

        public final static int RW_DESIRED_REV_BW = 5;

        public final static int RW_REQUIRED_REV_BW = 6;

        public final static int RO_AVAILABLE_REV_BW = 7;

        public final static int RW_MAX_ALLOWED_LATENCY = 8;

        public final static int RO_BOUND_INTERFACE = 9;

        public final static int RO_PHYSICAL_INTERFACE = 10;
    }

    public static final class Role {
        private Role() {
        }

        public static final String DEFAULT = "default";
        public static final String BULK_DOWNLOAD = "bulk.download";
        public static final String BULK_UPLOAD = "bulk.upload";
        public static final String VOIP_24KBPS = "voip.24k";
        public static final String VOIP_32KBPS = "voip.32k";
        public static final String VIDEO_STREAMING_480P = "video.streaming.480p";
        public static final String VIDEO_STREAMING_720I = "video.streaming.720i";
        public static final String VIDEO_CHAT_360P = "video.chat.360p";
        public static final String VIDEO_CHAT_480P = "video.chat.480i";
    }

    public LinkCapabilities() {
        mCapabilities = new HashMap<Integer, String>();
    }

    public LinkCapabilities(LinkCapabilities source) {
        if (source != null) {
            mCapabilities = new HashMap<Integer, String>(source.mCapabilities);
        } else {
            mCapabilities = new HashMap<Integer, String>();
        }
    }

    public static LinkCapabilities createNeedsMap(String applicationRole) {
        if (DBG) log("createNeededCapabilities(applicationRole) EX");
        return new LinkCapabilities();
    }

    public void clear() {
        mCapabilities.clear();
    }

    public boolean isEmpty() {
        return mCapabilities.isEmpty();
    }

    public int size() {
        return mCapabilities.size();
    }

    public String get(int key) {
        return mCapabilities.get(key);
    }

    public void put(int key, String value) {
        mCapabilities.put(key, value);
    }

    public boolean containsKey(int key) {
        return mCapabilities.containsKey(key);
    }

    public boolean containsValue(String value) {
        return mCapabilities.containsValue(value);
    }

    public Set<Entry<Integer, String>> entrySet() {
        return mCapabilities.entrySet();
    }

    public Set<Integer> keySet() {
        return mCapabilities.keySet();
    }

    public Collection<String> values() {
        return mCapabilities.values();
    }

    public int describeContents() {
        return 0;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("{");
        boolean firstTime = true;
        for (Entry<Integer, String> entry : mCapabilities.entrySet()) {
            if (firstTime) {
                firstTime = false;
            } else {
                sb.append(",");
            }
            sb.append(entry.getKey());
            sb.append(":\"");
            sb.append(entry.getValue());
            sb.append("\"");
            return mCapabilities.toString();
        }
        return sb.toString();
    }

    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(mCapabilities.size());
        for (Entry<Integer, String> entry : mCapabilities.entrySet()) {
            dest.writeInt(entry.getKey().intValue());
            dest.writeString(entry.getValue());
        }
    }

    public static final Creator<LinkCapabilities> CREATOR =
            new Creator<LinkCapabilities>() {
                public LinkCapabilities createFromParcel(Parcel in) {
                    LinkCapabilities capabilities = new LinkCapabilities();
                    int size = in.readInt();
                    while (size-- != 0) {
                        int key = in.readInt();
                        String value = in.readString();
                        capabilities.mCapabilities.put(key, value);
                    }
                    return capabilities;
                }

                public LinkCapabilities[] newArray(int size) {
                    return new LinkCapabilities[size];
                }
            };

    protected static void log(String s) {
        Log.d(TAG, s);
    }
}
