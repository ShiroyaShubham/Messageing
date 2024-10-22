package messenger.messages.messaging.sms.chat.meet.android.net;

import android.net.NetworkInfo;
import android.os.Parcel;
import android.os.Parcelable;

public class NetworkState implements Parcelable {

    public final NetworkInfo networkInfo;
    public final LinkProperties linkProperties;
    public final LinkCapabilities linkCapabilities;
    public final String subscriberId;
    public final String networkId;

    public NetworkState(NetworkInfo networkInfo, LinkProperties linkProperties,
                        LinkCapabilities linkCapabilities) {
        this(networkInfo, linkProperties, linkCapabilities, null, null);
    }

    public NetworkState(NetworkInfo networkInfo, LinkProperties linkProperties,
                        LinkCapabilities linkCapabilities, String subscriberId, String networkId) {
        this.networkInfo = networkInfo;
        this.linkProperties = linkProperties;
        this.linkCapabilities = linkCapabilities;
        this.subscriberId = subscriberId;
        this.networkId = networkId;
    }

    public NetworkState(Parcel in) {
        networkInfo = in.readParcelable(null);
        linkProperties = in.readParcelable(null);
        linkCapabilities = in.readParcelable(null);
        subscriberId = in.readString();
        networkId = in.readString();
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel out, int flags) {
        out.writeParcelable(networkInfo, flags);
        out.writeParcelable(linkProperties, flags);
        out.writeParcelable(linkCapabilities, flags);
        out.writeString(subscriberId);
        out.writeString(networkId);
    }

    public static final Creator<NetworkState> CREATOR = new Creator<NetworkState>() {
        @Override
        public NetworkState createFromParcel(Parcel in) {
            return new NetworkState(in);
        }

        @Override
        public NetworkState[] newArray(int size) {
            return new NetworkState[size];
        }
    };

}
