package messenger.messages.messaging.sms.chat.meet.android.net;

import android.os.Parcel;
import android.os.Parcelable;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.InterfaceAddress;
import java.net.UnknownHostException;


public class LinkAddress implements Parcelable {

    private final InetAddress address;

    private final int prefixLength;

    public LinkAddress(InetAddress address, int prefixLength) {
        if (address == null || prefixLength < 0 ||
                ((address instanceof Inet4Address) && prefixLength > 32) ||
                (prefixLength > 128)) {
            throw new IllegalArgumentException("Bad LinkAddress haloParams " + address +
                    prefixLength);
        }
        this.address = address;
        this.prefixLength = prefixLength;
    }

    public LinkAddress(InterfaceAddress interfaceAddress) {
        this.address = interfaceAddress.getAddress();
        this.prefixLength = interfaceAddress.getNetworkPrefixLength();
    }

    @Override
    public String toString() {
        return (address == null ? "" : (address.getHostAddress() + "/" + prefixLength));
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof LinkAddress)) {
            return false;
        }
        LinkAddress linkAddress = (LinkAddress) obj;
        return this.address.equals(linkAddress.address) &&
                this.prefixLength == linkAddress.prefixLength;
    }

    @Override

    public int hashCode() {
        return ((null == address) ? 0 : address.hashCode()) + prefixLength;
    }

    public InetAddress getAddress() {
        return address;
    }

    public int getNetworkPrefixLength() {
        return prefixLength;
    }

    public int describeContents() {
        return 0;
    }

    public void writeToParcel(Parcel dest, int flags) {
        if (address != null) {
            dest.writeByte((byte) 1);
            dest.writeByteArray(address.getAddress());
            dest.writeInt(prefixLength);
        } else {
            dest.writeByte((byte) 0);
        }
    }

    public static final Creator<LinkAddress> CREATOR =
            new Creator<LinkAddress>() {
                public LinkAddress createFromParcel(Parcel in) {
                    InetAddress address = null;
                    int prefixLength = 0;
                    if (in.readByte() == 1) {
                        try {
                            address = InetAddress.getByAddress(in.createByteArray());
                            prefixLength = in.readInt();
                        } catch (UnknownHostException e) {
                        }
                    }
                    return new LinkAddress(address, prefixLength);
                }

                public LinkAddress[] newArray(int size) {
                    return new LinkAddress[size];
                }
            };
}
