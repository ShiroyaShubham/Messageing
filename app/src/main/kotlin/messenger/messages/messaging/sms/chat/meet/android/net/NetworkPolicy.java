package messenger.messages.messaging.sms.chat.meet.android.net;

import android.os.Parcel;
import android.os.Parcelable;
import messenger.messages.messaging.sms.chat.meet.internal.util.Objects;

import static messenger.messages.messaging.sms.chat.meet.internal.util.Preconditions.checkNotNull;

public class NetworkPolicy implements Parcelable, Comparable<NetworkPolicy> {
    public static final int CYCLE_NONE = -1;
    public static final long WARNING_DISABLED = -1;
    public static final long LIMIT_DISABLED = -1;
    public static final long SNOOZE_NEVER = -1;

    public final NetworkTemplate template;
    public int cycleDay;
    public String cycleTimezone;
    public long warningBytes;
    public long limitBytes;
    public long lastWarningSnooze;
    public long lastLimitSnooze;
    public boolean metered;
    public boolean inferred;

    private static final long DEFAULT_MTU = 1500;

    @Deprecated
    public NetworkPolicy(NetworkTemplate template, int cycleDay, String cycleTimezone,
                         long warningBytes, long limitBytes, boolean metered) {
        this(template, cycleDay, cycleTimezone, warningBytes, limitBytes, SNOOZE_NEVER,
                SNOOZE_NEVER, metered, false);
    }

    public NetworkPolicy(NetworkTemplate template, int cycleDay, String cycleTimezone,
                         long warningBytes, long limitBytes, long lastWarningSnooze, long lastLimitSnooze,
                         boolean metered, boolean inferred) {
        this.template = checkNotNull(template, "missing NetworkTemplate");
        this.cycleDay = cycleDay;
        this.cycleTimezone = checkNotNull(cycleTimezone, "missing cycleTimezone");
        this.warningBytes = warningBytes;
        this.limitBytes = limitBytes;
        this.lastWarningSnooze = lastWarningSnooze;
        this.lastLimitSnooze = lastLimitSnooze;
        this.metered = metered;
        this.inferred = inferred;
    }

    public NetworkPolicy(Parcel in) {
        template = in.readParcelable(null);
        cycleDay = in.readInt();
        cycleTimezone = in.readString();
        warningBytes = in.readLong();
        limitBytes = in.readLong();
        lastWarningSnooze = in.readLong();
        lastLimitSnooze = in.readLong();
        metered = in.readInt() != 0;
        inferred = in.readInt() != 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeParcelable(template, flags);
        dest.writeInt(cycleDay);
        dest.writeString(cycleTimezone);
        dest.writeLong(warningBytes);
        dest.writeLong(limitBytes);
        dest.writeLong(lastWarningSnooze);
        dest.writeLong(lastLimitSnooze);
        dest.writeInt(metered ? 1 : 0);
        dest.writeInt(inferred ? 1 : 0);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public boolean isOverWarning(long totalBytes) {
        return warningBytes != WARNING_DISABLED && totalBytes >= warningBytes;
    }

    public boolean isOverLimit(long totalBytes) {
        // over-estimate, since kernel will trigger limit once first packet
        // trips over limit.
        totalBytes += 2 * DEFAULT_MTU;
        return limitBytes != LIMIT_DISABLED && totalBytes >= limitBytes;
    }

    public void clearSnooze() {
        lastWarningSnooze = SNOOZE_NEVER;
        lastLimitSnooze = SNOOZE_NEVER;
    }

    public boolean hasCycle() {
        return cycleDay != CYCLE_NONE;
    }

    @Override
    public int compareTo(NetworkPolicy another) {
        if (another == null || another.limitBytes == LIMIT_DISABLED) {
            // other value is missing or disabled; we win
            return -1;
        }
        if (limitBytes == LIMIT_DISABLED || another.limitBytes < limitBytes) {
            // we're disabled or other limit is smaller; they win
            return 1;
        }
        return 0;
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(template, cycleDay, cycleTimezone, warningBytes, limitBytes,
                lastWarningSnooze, lastLimitSnooze, metered, inferred);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof NetworkPolicy) {
            final NetworkPolicy other = (NetworkPolicy) obj;
            return cycleDay == other.cycleDay && warningBytes == other.warningBytes
                    && limitBytes == other.limitBytes
                    && lastWarningSnooze == other.lastWarningSnooze
                    && lastLimitSnooze == other.lastLimitSnooze && metered == other.metered
                    && inferred == other.inferred
                    && Objects.equal(cycleTimezone, other.cycleTimezone)
                    && Objects.equal(template, other.template);
        }
        return false;
    }

    @Override
    public String toString() {
        final StringBuilder builder = new StringBuilder("NetworkPolicy");
        builder.append("[").append(template).append("]:");
        builder.append(" cycleDay=").append(cycleDay);
        builder.append(", cycleTimezone=").append(cycleTimezone);
        builder.append(", warningBytes=").append(warningBytes);
        builder.append(", limitBytes=").append(limitBytes);
        builder.append(", lastWarningSnooze=").append(lastWarningSnooze);
        builder.append(", lastLimitSnooze=").append(lastLimitSnooze);
        builder.append(", metered=").append(metered);
        builder.append(", inferred=").append(inferred);
        return builder.toString();
    }

    public static final Creator<NetworkPolicy> CREATOR = new Creator<NetworkPolicy>() {
        @Override
        public NetworkPolicy createFromParcel(Parcel in) {
            return new NetworkPolicy(in);
        }

        @Override
        public NetworkPolicy[] newArray(int size) {
            return new NetworkPolicy[size];
        }
    };
}
