package messenger.messages.messaging.sms.chat.meet.mms.dom.smil;

import java.util.ArrayList;

import messenger.messages.messaging.sms.chat.meet.org.smil.Time;
import messenger.messages.messaging.sms.chat.meet.org.smil.TimeList;

public class TimeListImpl implements TimeList {
    private final ArrayList<Time> mTimes;
    TimeListImpl(ArrayList<Time> times) {
        mTimes = times;
    }

    public int getLength() {
        return mTimes.size();
    }

    public Time item(int index) {
        Time time = null;
        try {
            time = mTimes.get(index);
        } catch (IndexOutOfBoundsException e) {
            // Do nothing and return null
        }
        return time;
    }

}
