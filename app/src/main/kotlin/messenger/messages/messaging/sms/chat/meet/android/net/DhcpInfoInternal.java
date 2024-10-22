package messenger.messages.messaging.sms.chat.meet.android.net;

import android.net.DhcpInfo;
import android.text.TextUtils;
import android.util.Log;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;

public class DhcpInfoInternal {
    private final static String TAG = "DhcpInfoInternal";
    public String ipAddress;
    public int prefixLength;

    public String dns1;
    public String dns2;

    public String serverAddress;
    public int leaseDuration;

    public String vendorInfo;

    private Collection<RouteInfo> mRoutes;

    public DhcpInfoInternal() {
        mRoutes = new ArrayList<RouteInfo>();
    }

    public void addRoute(RouteInfo routeInfo) {
        mRoutes.add(routeInfo);
    }

    public Collection<RouteInfo> getRoutes() {
        return Collections.unmodifiableCollection(mRoutes);
    }

    private int convertToInt(String addr) {
        if (addr != null) {
            try {
                InetAddress inetAddress = NetworkUtilsHelper.numericToInetAddress(addr);
                if (inetAddress instanceof Inet4Address) {
                    return NetworkUtilsHelper.inetAddressToInt(inetAddress);
                }
            } catch (IllegalArgumentException e) {
            }
        }
        return 0;
    }

    public DhcpInfo makeDhcpInfo() {
        DhcpInfo info = new DhcpInfo();
        info.ipAddress = convertToInt(ipAddress);
        for (RouteInfo route : mRoutes) {
            if (route.isDefaultRoute()) {
                info.gateway = convertToInt(route.getGateway().getHostAddress());
                break;
            }
        }
        try {
            info.netmask = NetworkUtilsHelper.prefixLengthToNetmaskInt(prefixLength);
        } catch (IllegalArgumentException e) {
        }
        info.dns1 = convertToInt(dns1);
        info.dns2 = convertToInt(dns2);
        info.serverAddress = convertToInt(serverAddress);
        info.leaseDuration = leaseDuration;
        return info;
    }

    public LinkAddress makeLinkAddress() {
        if (TextUtils.isEmpty(ipAddress)) {
            Log.e(TAG, "makeLinkAddress with empty ipAddress");
            return null;
        }
        return new LinkAddress(NetworkUtilsHelper.numericToInetAddress(ipAddress), prefixLength);
    }

    public LinkProperties makeLinkProperties() {
        LinkProperties p = new LinkProperties();
        p.addLinkAddress(makeLinkAddress());
        for (RouteInfo route : mRoutes) {
            p.addRoute(route);
        }
        //if empty, connectivity configures default DNS
        if (TextUtils.isEmpty(dns1) == false) {
            p.addDns(NetworkUtilsHelper.numericToInetAddress(dns1));
        } else {
            Log.d(TAG, "makeLinkProperties with empty dns1!");
        }
        if (TextUtils.isEmpty(dns2) == false) {
            p.addDns(NetworkUtilsHelper.numericToInetAddress(dns2));
        } else {
            Log.d(TAG, "makeLinkProperties with empty dns2!");
        }
        return p;
    }

    public void updateFromDhcpRequest(DhcpInfoInternal orig) {
        if (orig == null) return;

        if (TextUtils.isEmpty(dns1)) {
            dns1 = orig.dns1;
        }

        if (TextUtils.isEmpty(dns2)) {
            dns2 = orig.dns2;
        }

        if (mRoutes.size() == 0) {
            for (RouteInfo route : orig.getRoutes()) {
                addRoute(route);
            }
        }
    }

    public boolean hasMeteredHint() {
        if (vendorInfo != null) {
            return vendorInfo.contains("ANDROID_METERED");
        } else {
            return false;
        }
    }

    public String toString() {
        String routeString = "";
        for (RouteInfo route : mRoutes) routeString += route.toString() + " | ";
        return "addr: " + ipAddress + "/" + prefixLength +
                " mRoutes: " + routeString +
                " dns: " + dns1 + "," + dns2 +
                " dhcpServer: " + serverAddress +
                " leaseDuration: " + leaseDuration;
    }
}
