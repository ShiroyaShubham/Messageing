package messenger.messages.messaging.sms.chat.meet.mms.dom.smil;

import org.w3c.dom.DOMException;
import org.w3c.dom.Element;
import messenger.messages.messaging.sms.chat.meet.org.smil.Time;

public class TimeImpl implements Time {
    static final int ALLOW_INDEFINITE_VALUE = (1 << 0);
    static final int ALLOW_OFFSET_VALUE     = (1 << 1);
    static final int ALLOW_SYNCBASE_VALUE   = (1 << 2);
    static final int ALLOW_SYNCTOPREV_VALUE = (1 << 3);
    static final int ALLOW_EVENT_VALUE      = (1 << 4);
    static final int ALLOW_MARKER_VALUE     = (1 << 5);
    static final int ALLOW_WALLCLOCK_VALUE  = (1 << 6);
    static final int ALLOW_NEGATIVE_VALUE   = (1 << 7);
    static final int ALLOW_ALL              = 0xFF;

    short mTimeType;
    boolean mResolved;
    double mResolvedOffset;

    TimeImpl(String timeValue, int constraints) {
        // Will throw NullPointerException if timeValue is null
        if (timeValue.equals("indefinite")
                && ((constraints & ALLOW_INDEFINITE_VALUE) != 0) ) {
            mTimeType = SMIL_TIME_INDEFINITE;
        } else if ((constraints & ALLOW_OFFSET_VALUE) != 0) {
            int sign = 1;
            if (timeValue.startsWith("+")) {
                timeValue = timeValue.substring(1);
            } else if (timeValue.startsWith("-")) {
                timeValue = timeValue.substring(1);
                sign = -1;
            }
            mResolvedOffset = sign*parseClockValue(timeValue)/1000.0;
            mResolved = true;
            mTimeType = SMIL_TIME_OFFSET;
        } else {
            throw new IllegalArgumentException("Unsupported time value");
        }
    }

    public static float parseClockValue(String clockValue) {
        try {
            float result = 0;

            // Will throw NullPointerException if clockValue is null
            clockValue = clockValue.trim();

            // Handle first 'Timecount-val' cases with metric
            if (clockValue.endsWith("ms")) {
                result = parseFloat(clockValue, 2, true);
            } else if (clockValue.endsWith("s")) {
                result = 1000*parseFloat(clockValue, 1, true);
            } else if (clockValue.endsWith("min")) {
                result = 60000*parseFloat(clockValue, 3, true);
            } else if (clockValue.endsWith("h")) {
                result = 3600000*parseFloat(clockValue, 1, true);
            } else {
                // Handle Timecount-val without metric
                try {
                    return parseFloat(clockValue, 0, true) * 1000;
                } catch (NumberFormatException e) {
                    // Ignore
                }

                // Split in {[Hours], Minutes, Seconds}
                String[] timeValues = clockValue.split(":");

                int indexOfMinutes;
                if (timeValues.length == 2) {
                    indexOfMinutes = 0;
                } else if (timeValues.length == 3) {
                    result = 3600000*(int)parseFloat(timeValues[0], 0, false);
                    indexOfMinutes = 1;
                } else {
                    throw new IllegalArgumentException();
                }

                // Read Minutes
                int minutes = (int)parseFloat(timeValues[indexOfMinutes], 0, false);
                if ((minutes >= 00) && (minutes <= 59)) {
                    result += 60000*minutes;
                } else {
                    throw new IllegalArgumentException();
                }

                // Read Seconds
                float seconds = parseFloat(timeValues[indexOfMinutes + 1], 0, true);
                if ((seconds >= 00) && (seconds < 60)) {
                    result += 60000*seconds;
                } else {
                    throw new IllegalArgumentException();
                }

            }
            return result;
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException();
        }
    }

    private static float parseFloat(String value, int ignoreLast, boolean parseDecimal) {
        // Ignore last characters
        value = value.substring(0, value.length() - ignoreLast);

        float result;
        int indexOfComma = value.indexOf('.');
        if (indexOfComma != -1) {
            if (!parseDecimal) {
                throw new IllegalArgumentException("int value contains decimal");
            }
            // Ensure that there are at least 3 decimals
            value = value + "000";
            // Read value up to 3 decimals and cut the rest
            result = Float.parseFloat(value.substring(0, indexOfComma));
            result += Float.parseFloat(
                    value.substring(indexOfComma + 1, indexOfComma + 4))/1000;
        } else {
            result = Integer.parseInt(value);
        }

        return result;
    }


    public boolean getBaseBegin() {
        // TODO Auto-generated method stub
        return false;
    }

    public Element getBaseElement() {
        // TODO Auto-generated method stub
        return null;
    }

    public String getEvent() {
        // TODO Auto-generated method stub
        return null;
    }

    public String getMarker() {
        // TODO Auto-generated method stub
        return null;
    }

    public double getOffset() {
        // TODO Auto-generated method stub
        return 0;
    }

    public boolean getResolved() {
        return mResolved;
    }

    public double getResolvedOffset() {
        return mResolvedOffset;
    }

    public short getTimeType() {
        return mTimeType;
    }

    public void setBaseBegin(boolean baseBegin) throws DOMException {
        // TODO Auto-generated method stub

    }

    public void setBaseElement(Element baseElement) throws DOMException {
        // TODO Auto-generated method stub

    }

    public void setEvent(String event) throws DOMException {
        // TODO Auto-generated method stub

    }

    public void setMarker(String marker) throws DOMException {
        // TODO Auto-generated method stub

    }

    public void setOffset(double offset) throws DOMException {
        // TODO Auto-generated method stub

    }
}
