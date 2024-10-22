package messenger.messages.messaging.sms.chat.meet.android.net;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.pm.Signature;
import android.os.RemoteException;
import android.text.format.Time;
import messenger.messages.messaging.sms.chat.meet.android.collect.Sets;

import java.io.PrintWriter;
import java.util.HashSet;

import static android.content.pm.PackageManager.GET_SIGNATURES;
import static android.text.format.Time.MONTH_DAY;

public class NetworkPolicyManager {

    public static final int POLICY_NONE = 0x0;
    public static final int POLICY_REJECT_METERED_BACKGROUND = 0x1;
    public static final int RULE_ALLOW_ALL = 0x0;
    public static final int RULE_REJECT_METERED = 0x1;
    private static final boolean ALLOW_PLATFORM_APP_POLICY = true;

    public static final String EXTRA_NETWORK_TEMPLATE = "android.net.NETWORK_TEMPLATE";

    private INetworkPolicyManager mService;

    public NetworkPolicyManager(INetworkPolicyManager service) {
        if (service == null) {
            throw new IllegalArgumentException("missing INetworkPolicyManager");
        }
        mService = service;
    }

    public void setUidPolicy(int uid, int policy) {
        try {
            mService.setUidPolicy(uid, policy);
        } catch (RemoteException e) {
        }
    }

    public int getUidPolicy(int uid) {
        try {
            return mService.getUidPolicy(uid);
        } catch (RemoteException e) {
            return POLICY_NONE;
        }
    }

    public int[] getUidsWithPolicy(int policy) {
        try {
            return mService.getUidsWithPolicy(policy);
        } catch (RemoteException e) {
            return new int[0];
        }
    }

    public void registerListener(INetworkPolicyListener listener) {
        try {
            mService.registerListener(listener);
        } catch (RemoteException e) {
        }
    }

    public void unregisterListener(INetworkPolicyListener listener) {
        try {
            mService.unregisterListener(listener);
        } catch (RemoteException e) {
        }
    }

    public void setNetworkPolicies(NetworkPolicy[] policies) {
        try {
            mService.setNetworkPolicies(policies);
        } catch (RemoteException e) {
        }
    }

    public NetworkPolicy[] getNetworkPolicies() {
        try {
            return mService.getNetworkPolicies();
        } catch (RemoteException e) {
            return null;
        }
    }

    public void setRestrictBackground(boolean restrictBackground) {
        try {
            mService.setRestrictBackground(restrictBackground);
        } catch (RemoteException e) {
        }
    }

    public boolean getRestrictBackground() {
        try {
            return mService.getRestrictBackground();
        } catch (RemoteException e) {
            return false;
        }
    }

    public static long computeLastCycleBoundary(long currentTime, NetworkPolicy policy) {
        if (policy.cycleDay == NetworkPolicy.CYCLE_NONE) {
            throw new IllegalArgumentException("Unable to compute boundary without cycleDay");
        }

        final Time now = new Time(policy.cycleTimezone);
        now.set(currentTime);

        // first, find cycle boundary for current month
        final Time cycle = new Time(now);
        cycle.hour = cycle.minute = cycle.second = 0;
        snapToCycleDay(cycle, policy.cycleDay);

        if (Time.compare(cycle, now) >= 0) {
            // cycle boundary is beyond now, use last cycle boundary; start by
            // pushing ourselves squarely into last month.
            final Time lastMonth = new Time(now);
            lastMonth.hour = lastMonth.minute = lastMonth.second = 0;
            lastMonth.monthDay = 1;
            lastMonth.month -= 1;
            lastMonth.normalize(true);

            cycle.set(lastMonth);
            snapToCycleDay(cycle, policy.cycleDay);
        }

        return cycle.toMillis(true);
    }

    public static long computeNextCycleBoundary(long currentTime, NetworkPolicy policy) {
        if (policy.cycleDay == NetworkPolicy.CYCLE_NONE) {
            throw new IllegalArgumentException("Unable to compute boundary without cycleDay");
        }

        final Time now = new Time(policy.cycleTimezone);
        now.set(currentTime);

        // first, find cycle boundary for current month
        final Time cycle = new Time(now);
        cycle.hour = cycle.minute = cycle.second = 0;
        snapToCycleDay(cycle, policy.cycleDay);

        if (Time.compare(cycle, now) <= 0) {
            // cycle boundary is before now, use next cycle boundary; start by
            // pushing ourselves squarely into next month.
            final Time nextMonth = new Time(now);
            nextMonth.hour = nextMonth.minute = nextMonth.second = 0;
            nextMonth.monthDay = 1;
            nextMonth.month += 1;
            nextMonth.normalize(true);

            cycle.set(nextMonth);
            snapToCycleDay(cycle, policy.cycleDay);
        }

        return cycle.toMillis(true);
    }

    public static void snapToCycleDay(Time time, int cycleDay) {
        if (cycleDay > time.getActualMaximum(MONTH_DAY)) {
            // cycle day isn't valid this month; snap to last second of month
            time.month += 1;
            time.monthDay = 1;
            time.second = -1;
        } else {
            time.monthDay = cycleDay;
        }
        time.normalize(true);
    }

    @Deprecated
    public static boolean isUidValidForPolicy(Context context, int uid) {

        if (!ALLOW_PLATFORM_APP_POLICY) {
            final PackageManager pm = context.getPackageManager();
            final HashSet<Signature> systemSignature;
            try {
                systemSignature = Sets.newHashSet(
                        pm.getPackageInfo("android", GET_SIGNATURES).signatures);
            } catch (NameNotFoundException e) {
                throw new RuntimeException("problem finding system signature", e);
            }

            try {
                // reject apps signed with platform cert
                for (String packageName : pm.getPackagesForUid(uid)) {
                    final HashSet<Signature> packageSignature = Sets.newHashSet(
                            pm.getPackageInfo(packageName, GET_SIGNATURES).signatures);
                    if (packageSignature.containsAll(systemSignature)) {
                        return false;
                    }
                }
            } catch (NameNotFoundException e) {
            }
        }

        // nothing found above; we can apply policy to UID
        return true;
    }

    public static void dumpPolicy(PrintWriter fout, int policy) {
        fout.write("[");
        if ((policy & POLICY_REJECT_METERED_BACKGROUND) != 0) {
            fout.write("REJECT_METERED_BACKGROUND");
        }
        fout.write("]");
    }


    public static void dumpRules(PrintWriter fout, int rules) {
        fout.write("[");
        if ((rules & RULE_REJECT_METERED) != 0) {
            fout.write("REJECT_METERED");
        }
        fout.write("]");
    }

}
