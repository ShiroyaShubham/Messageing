package messenger.messages.messaging.sms.chat.meet.android.net;

import android.os.Parcel;
import android.os.Parcelable;

public class NetworkQuotaInfo implements Parcelable {
    private final long mEstimatedBytes;
    private final long mSoftLimitBytes;
    private final long mHardLimitBytes;
    public static final long NO_LIMIT = -1;
    public NetworkQuotaInfo(long estimatedBytes, long softLimitBytes, long hardLimitBytes) {
        mEstimatedBytes = estimatedBytes;
        mSoftLimitBytes = softLimitBytes;
        mHardLimitBytes = hardLimitBytes;
    }

    public NetworkQuotaInfo(Parcel in) {
        mEstimatedBytes = in.readLong();
        mSoftLimitBytes = in.readLong();
        mHardLimitBytes = in.readLong();
    }

    public long getEstimatedBytes() {
        return mEstimatedBytes;
    }

    public long getSoftLimitBytes() {
        return mSoftLimitBytes;
    }

    public long getHardLimitBytes() {
        return mHardLimitBytes;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel out, int flags) {
        out.writeLong(mEstimatedBytes);
        out.writeLong(mSoftLimitBytes);
        out.writeLong(mHardLimitBytes);
    }

    public static final Creator<NetworkQuotaInfo> CREATOR = new Creator<NetworkQuotaInfo>() {
        @Override
        public NetworkQuotaInfo createFromParcel(Parcel in) {
            return new NetworkQuotaInfo(in);
        }

        @Override
        public NetworkQuotaInfo[] newArray(int size) {
            return new NetworkQuotaInfo[size];
        }
    };
}
