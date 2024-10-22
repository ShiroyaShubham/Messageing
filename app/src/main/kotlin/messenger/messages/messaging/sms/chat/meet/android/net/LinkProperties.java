package messenger.messages.messaging.sms.chat.meet.android.net;

import android.os.Parcel;
import android.os.Parcelable;
import android.text.TextUtils;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;

public class LinkProperties implements Parcelable {

    String mIfaceName;
    private Collection<LinkAddress> mLinkAddresses = new ArrayList<LinkAddress>();
    private Collection<InetAddress> mDnses = new ArrayList<InetAddress>();
    private Collection<RouteInfo> mRoutes = new ArrayList<RouteInfo>();
    private ProxyProperties mHttpProxy;

    public static class CompareResult<T> {
        public Collection<T> removed = new ArrayList<T>();
        public Collection<T> added = new ArrayList<T>();

        @Override
        public String toString() {
            String retVal = "removed=[";
            for (T addr : removed) retVal += addr.toString() + ",";
            retVal += "] added=[";
            for (T addr : added) retVal += addr.toString() + ",";
            retVal += "]";
            return retVal;
        }
    }

    public LinkProperties() {
        clear();
    }

    // copy constructor instead of clone
    public LinkProperties(LinkProperties source) {
        if (source != null) {
            mIfaceName = source.getInterfaceName();
            for (LinkAddress l : source.getLinkAddresses()) mLinkAddresses.add(l);
            for (InetAddress i : source.getDnses()) mDnses.add(i);
            for (RouteInfo r : source.getRoutes()) mRoutes.add(r);
            mHttpProxy = (source.getHttpProxy() == null) ?
                    null : new ProxyProperties(source.getHttpProxy());
        }
    }

    public void setInterfaceName(String iface) {
        mIfaceName = iface;
    }

    public String getInterfaceName() {
        return mIfaceName;
    }

    public Collection<InetAddress> getAddresses() {
        Collection<InetAddress> addresses = new ArrayList<InetAddress>();
        for (LinkAddress linkAddress : mLinkAddresses) {
            addresses.add(linkAddress.getAddress());
        }
        return Collections.unmodifiableCollection(addresses);
    }

    public void addLinkAddress(LinkAddress address) {
        if (address != null) mLinkAddresses.add(address);
    }

    public Collection<LinkAddress> getLinkAddresses() {
        return Collections.unmodifiableCollection(mLinkAddresses);
    }

    public void addDns(InetAddress dns) {
        if (dns != null) mDnses.add(dns);
    }

    public Collection<InetAddress> getDnses() {
        return Collections.unmodifiableCollection(mDnses);
    }

    public void addRoute(RouteInfo route) {
        if (route != null) mRoutes.add(route);
    }

    public Collection<RouteInfo> getRoutes() {
        return Collections.unmodifiableCollection(mRoutes);
    }

    public void setHttpProxy(ProxyProperties proxy) {
        mHttpProxy = proxy;
    }

    public ProxyProperties getHttpProxy() {
        return mHttpProxy;
    }

    public void clear() {
        mIfaceName = null;
        mLinkAddresses.clear();
        mDnses.clear();
        mRoutes.clear();
        mHttpProxy = null;
    }

    public int describeContents() {
        return 0;
    }

    @Override
    public String toString() {
        String ifaceName = (mIfaceName == null ? "" : "InterfaceName: " + mIfaceName + " ");

        String linkAddresses = "LinkAddresses: [";
        for (LinkAddress addr : mLinkAddresses) linkAddresses += addr.toString() + ",";
        linkAddresses += "] ";

        String dns = "DnsAddresses: [";
        for (InetAddress addr : mDnses) dns += addr.getHostAddress() + ",";
        dns += "] ";

        String routes = "Routes: [";
        for (RouteInfo route : mRoutes) routes += route.toString() + ",";
        routes += "] ";
        String proxy = (mHttpProxy == null ? "" : "HttpProxy: " + mHttpProxy.toString() + " ");

        return ifaceName + linkAddresses + routes + dns + proxy;
    }


    public boolean isIdenticalInterfaceName(LinkProperties target) {
        return TextUtils.equals(getInterfaceName(), target.getInterfaceName());
    }

    public boolean isIdenticalAddresses(LinkProperties target) {
        Collection<InetAddress> targetAddresses = target.getAddresses();
        Collection<InetAddress> sourceAddresses = getAddresses();
        return (sourceAddresses.size() == targetAddresses.size()) ?
                sourceAddresses.containsAll(targetAddresses) : false;
    }


    public boolean isIdenticalDnses(LinkProperties target) {
        Collection<InetAddress> targetDnses = target.getDnses();
        return (mDnses.size() == targetDnses.size()) ?
                mDnses.containsAll(targetDnses) : false;
    }


    public boolean isIdenticalRoutes(LinkProperties target) {
        Collection<RouteInfo> targetRoutes = target.getRoutes();
        return (mRoutes.size() == targetRoutes.size()) ?
                mRoutes.containsAll(targetRoutes) : false;
    }


    public boolean isIdenticalHttpProxy(LinkProperties target) {
        return getHttpProxy() == null ? target.getHttpProxy() == null :
                getHttpProxy().equals(target.getHttpProxy());
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;

        if (!(obj instanceof LinkProperties)) return false;

        LinkProperties target = (LinkProperties) obj;

        return isIdenticalInterfaceName(target) &&
                isIdenticalAddresses(target) &&
                isIdenticalDnses(target) &&
                isIdenticalRoutes(target) &&
                isIdenticalHttpProxy(target);
    }

    public CompareResult<LinkAddress> compareAddresses(LinkProperties target) {
        CompareResult<LinkAddress> result = new CompareResult<LinkAddress>();
        result.removed = new ArrayList<LinkAddress>(mLinkAddresses);
        result.added.clear();
        if (target != null) {
            for (LinkAddress newAddress : target.getLinkAddresses()) {
                if (!result.removed.remove(newAddress)) {
                    result.added.add(newAddress);
                }
            }
        }
        return result;
    }

    public CompareResult<InetAddress> compareDnses(LinkProperties target) {
        CompareResult<InetAddress> result = new CompareResult<InetAddress>();

        result.removed = new ArrayList<InetAddress>(mDnses);
        result.added.clear();
        if (target != null) {
            for (InetAddress newAddress : target.getDnses()) {
                if (!result.removed.remove(newAddress)) {
                    result.added.add(newAddress);
                }
            }
        }
        return result;
    }

    public CompareResult<RouteInfo> compareRoutes(LinkProperties target) {
        CompareResult<RouteInfo> result = new CompareResult<RouteInfo>();

        result.removed = new ArrayList<RouteInfo>(mRoutes);
        result.added.clear();
        if (target != null) {
            for (RouteInfo r : target.getRoutes()) {
                if (!result.removed.remove(r)) {
                    result.added.add(r);
                }
            }
        }
        return result;
    }


    @Override
    public int hashCode() {
        return ((null == mIfaceName) ? 0 : mIfaceName.hashCode()
                + mLinkAddresses.size() * 31
                + mDnses.size() * 37
                + mRoutes.size() * 41
                + ((null == mHttpProxy) ? 0 : mHttpProxy.hashCode()));
    }

    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(getInterfaceName());
        dest.writeInt(mLinkAddresses.size());
        for (LinkAddress linkAddress : mLinkAddresses) {
            dest.writeParcelable(linkAddress, flags);
        }

        dest.writeInt(mDnses.size());
        for (InetAddress d : mDnses) {
            dest.writeByteArray(d.getAddress());
        }

        dest.writeInt(mRoutes.size());
        for (RouteInfo route : mRoutes) {
            dest.writeParcelable(route, flags);
        }

        if (mHttpProxy != null) {
            dest.writeByte((byte) 1);
            dest.writeParcelable(mHttpProxy, flags);
        } else {
            dest.writeByte((byte) 0);
        }
    }

    public static final Creator<LinkProperties> CREATOR =
            new Creator<LinkProperties>() {
                public LinkProperties createFromParcel(Parcel in) {
                    LinkProperties netProp = new LinkProperties();
                    String iface = in.readString();
                    if (iface != null) {
                        try {
                            netProp.setInterfaceName(iface);
                        } catch (Exception e) {
                            return null;
                        }
                    }
                    int addressCount = in.readInt();
                    for (int i = 0; i < addressCount; i++) {
                        netProp.addLinkAddress((LinkAddress) in.readParcelable(null));
                    }
                    addressCount = in.readInt();
                    for (int i = 0; i < addressCount; i++) {
                        try {
                            netProp.addDns(InetAddress.getByAddress(in.createByteArray()));
                        } catch (UnknownHostException e) {
                        }
                    }
                    addressCount = in.readInt();
                    for (int i = 0; i < addressCount; i++) {
                        netProp.addRoute((RouteInfo) in.readParcelable(null));
                    }
                    if (in.readByte() == 1) {
                        netProp.setHttpProxy((ProxyProperties) in.readParcelable(null));
                    }
                    return netProp;
                }

                public LinkProperties[] newArray(int size) {
                    return new LinkProperties[size];
                }
            };
}
