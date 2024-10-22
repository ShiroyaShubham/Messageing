package messenger.messages.messaging.sms.chat.meet.android.net;

import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Parcel;
import android.os.Parcelable;
import messenger.messages.messaging.sms.chat.meet.internal.annotations.VisibleForTesting;
import messenger.messages.messaging.sms.chat.meet.internal.util.Objects;

import static android.net.ConnectivityManager.*;
import static messenger.messages.messaging.sms.chat.meet.internal.util.ArrayUtils.contains;

public class NetworkTemplate implements Parcelable {

    public static final int MATCH_MOBILE_ALL = 1;
    public static final int MATCH_MOBILE_3G_LOWER = 2;
    public static final int MATCH_MOBILE_4G = 3;
    public static final int MATCH_WIFI = 4;
    public static final int MATCH_ETHERNET = 5;
    public static final int MATCH_MOBILE_WILDCARD = 6;
    public static final int MATCH_WIFI_WILDCARD = 7;
    public static final int NETWORK_TYPE_UNKNOWN = 0;
    public static final int NETWORK_TYPE_GPRS = 1;
    public static final int NETWORK_TYPE_EDGE = 2;
    public static final int NETWORK_TYPE_UMTS = 3;
    public static final int NETWORK_TYPE_CDMA = 4;
    public static final int NETWORK_TYPE_EVDO_0 = 5;
    public static final int NETWORK_TYPE_EVDO_A = 6;
    public static final int NETWORK_TYPE_1xRTT = 7;
    public static final int NETWORK_TYPE_HSDPA = 8;
    public static final int NETWORK_TYPE_HSUPA = 9;
    public static final int NETWORK_TYPE_HSPA = 10;
    public static final int NETWORK_TYPE_IDEN = 11;
    public static final int NETWORK_TYPE_EVDO_B = 12;
    public static final int NETWORK_TYPE_LTE = 13;
    public static final int NETWORK_TYPE_EHRPD = 14;
    public static final int NETWORK_TYPE_HSPAP = 15;
    public static final int NETWORK_CLASS_UNKNOWN = 0;
    public static final int NETWORK_CLASS_2_G = 1;
    public static final int NETWORK_CLASS_3_G = 2;
    public static final int NETWORK_CLASS_4_G = 3;

    public static int getNetworkClass(int networkType) {
        switch (networkType) {
            case NETWORK_TYPE_GPRS:
            case NETWORK_TYPE_EDGE:
            case NETWORK_TYPE_CDMA:
            case NETWORK_TYPE_1xRTT:
            case NETWORK_TYPE_IDEN:
                return NETWORK_CLASS_2_G;
            case NETWORK_TYPE_UMTS:
            case NETWORK_TYPE_EVDO_0:
            case NETWORK_TYPE_EVDO_A:
            case NETWORK_TYPE_HSDPA:
            case NETWORK_TYPE_HSUPA:
            case NETWORK_TYPE_HSPA:
            case NETWORK_TYPE_EVDO_B:
            case NETWORK_TYPE_EHRPD:
            case NETWORK_TYPE_HSPAP:
                return NETWORK_CLASS_3_G;
            case NETWORK_TYPE_LTE:
                return NETWORK_CLASS_4_G;
            default:
                return NETWORK_CLASS_UNKNOWN;
        }
    }

    private static final int[] DATA_USAGE_NETWORK_TYPES = {0};

    private static boolean sForceAllNetworkTypes = false;

    @VisibleForTesting
    public static void forceAllNetworkTypes() {
        sForceAllNetworkTypes = true;
    }
    public static NetworkTemplate buildTemplateMobileAll(String subscriberId) {
        return new NetworkTemplate(MATCH_MOBILE_ALL, subscriberId, null);
    }

    @Deprecated
    public static NetworkTemplate buildTemplateMobile3gLower(String subscriberId) {
        return new NetworkTemplate(MATCH_MOBILE_3G_LOWER, subscriberId, null);
    }

    @Deprecated
    public static NetworkTemplate buildTemplateMobile4g(String subscriberId) {
        return new NetworkTemplate(MATCH_MOBILE_4G, subscriberId, null);
    }

    public static NetworkTemplate buildTemplateMobileWildcard() {
        return new NetworkTemplate(MATCH_MOBILE_WILDCARD, null, null);
    }

    public static NetworkTemplate buildTemplateWifiWildcard() {
        return new NetworkTemplate(MATCH_WIFI_WILDCARD, null, null);
    }
    @Deprecated
    public static NetworkTemplate buildTemplateWifi() {
        return buildTemplateWifiWildcard();
    }

    public static NetworkTemplate buildTemplateWifi(String networkId) {
        return new NetworkTemplate(MATCH_WIFI, null, networkId);
    }

    public static NetworkTemplate buildTemplateEthernet() {
        return new NetworkTemplate(MATCH_ETHERNET, null, null);
    }

    private final int mMatchRule;
    private final String mSubscriberId;
    private final String mNetworkId;

    public NetworkTemplate(int matchRule, String subscriberId, String networkId) {
        mMatchRule = matchRule;
        mSubscriberId = subscriberId;
        mNetworkId = networkId;
    }

    private NetworkTemplate(Parcel in) {
        mMatchRule = in.readInt();
        mSubscriberId = in.readString();
        mNetworkId = in.readString();
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(mMatchRule);
        dest.writeString(mSubscriberId);
        dest.writeString(mNetworkId);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public String toString() {
        final StringBuilder builder = new StringBuilder("NetworkTemplate: ");
        builder.append("matchRule=").append(getMatchRuleName(mMatchRule));
        if (mSubscriberId != null) {
            builder.append(", subscriberId=").append(NetworkIdentity.scrubSubscriberId(mSubscriberId));
        }
        if (mNetworkId != null) {
            builder.append(", networkId=").append(mNetworkId);
        }
        return builder.toString();
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(mMatchRule, mSubscriberId, mNetworkId);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof NetworkTemplate) {
            final NetworkTemplate other = (NetworkTemplate) obj;
            return mMatchRule == other.mMatchRule
                    && Objects.equal(mSubscriberId, other.mSubscriberId)
                    && Objects.equal(mNetworkId, other.mNetworkId);
        }
        return false;
    }

    public int getMatchRule() {
        return mMatchRule;
    }

    public String getSubscriberId() {
        return mSubscriberId;
    }

    public String getNetworkId() {
        return mNetworkId;
    }

    public boolean matches(NetworkIdentity ident) {
        switch (mMatchRule) {
            case MATCH_MOBILE_ALL:
                return matchesMobile(ident);
            case MATCH_MOBILE_3G_LOWER:
                return matchesMobile3gLower(ident);
            case MATCH_MOBILE_4G:
                return matchesMobile4g(ident);
            case MATCH_WIFI:
                return matchesWifi(ident);
            case MATCH_ETHERNET:
                return matchesEthernet(ident);
            case MATCH_MOBILE_WILDCARD:
                return matchesMobileWildcard(ident);
            case MATCH_WIFI_WILDCARD:
                return matchesWifiWildcard(ident);
            default:
                throw new IllegalArgumentException("unknown network template");
        }
    }

    private boolean matchesMobile(NetworkIdentity ident) {
        if (ident.mType == TYPE_WIMAX) {
            // TODO: consider matching against WiMAX subscriber identity
            return true;
        } else {
            return ((sForceAllNetworkTypes || contains(DATA_USAGE_NETWORK_TYPES, ident.mType))
                    && Objects.equal(mSubscriberId, ident.mSubscriberId));
        }
    }

    private boolean matchesMobile3gLower(NetworkIdentity ident) {
        ensureSubtypeAvailable();
        if (ident.mType == TYPE_WIMAX) {
            return false;
        } else if (matchesMobile(ident)) {
            switch (getNetworkClass(ident.mSubType)) {
                case 0:
                case 1:
                case 2:
                    return true;
            }
        }
        return false;
    }

    private boolean matchesMobile4g(NetworkIdentity ident) {
        ensureSubtypeAvailable();
        if (ident.mType == TYPE_WIMAX) {
            // TODO: consider matching against WiMAX subscriber identity
            return true;
        } else if (matchesMobile(ident)) {
            switch (getNetworkClass(ident.mSubType)) {
                case 3:
                    return true;
            }
        }
        return false;
    }

    private boolean matchesWifi(NetworkIdentity ident) {
        switch (ident.mType) {
            case TYPE_WIFI:
                return Objects.equal(mNetworkId, ident.mNetworkId);
            default:
                return false;
        }
    }

    private boolean matchesEthernet(NetworkIdentity ident) {
        if (ident.mType == TYPE_ETHERNET) {
            return true;
        }
        return false;
    }

    private boolean matchesMobileWildcard(NetworkIdentity ident) {
        if (ident.mType == TYPE_WIMAX) {
            return true;
        } else {
            return sForceAllNetworkTypes || contains(DATA_USAGE_NETWORK_TYPES, ident.mType);
        }
    }

    private boolean matchesWifiWildcard(NetworkIdentity ident) {
        switch (ident.mType) {
            case TYPE_WIFI:
            case 13:
                return true;
            default:
                return false;
        }
    }

    private static String getMatchRuleName(int matchRule) {
        switch (matchRule) {
            case MATCH_MOBILE_3G_LOWER:
                return "MOBILE_3G_LOWER";
            case MATCH_MOBILE_4G:
                return "MOBILE_4G";
            case MATCH_MOBILE_ALL:
                return "MOBILE_ALL";
            case MATCH_WIFI:
                return "WIFI";
            case MATCH_ETHERNET:
                return "ETHERNET";
            case MATCH_MOBILE_WILDCARD:
                return "MOBILE_WILDCARD";
            case MATCH_WIFI_WILDCARD:
                return "WIFI_WILDCARD";
            default:
                return "UNKNOWN";
        }
    }

    private static void ensureSubtypeAvailable() {
        if (NetworkIdentity.COMBINE_SUBTYPE_ENABLED) {
            throw new IllegalArgumentException(
                    "Unable to enforce 3G_LOWER template on combined data.");
        }
    }

    public static final Creator<NetworkTemplate> CREATOR = new Creator<NetworkTemplate>() {
        @Override
        public NetworkTemplate createFromParcel(Parcel in) {
            return new NetworkTemplate(in);
        }

        @Override
        public NetworkTemplate[] newArray(int size) {
            return new NetworkTemplate[size];
        }
    };
}
