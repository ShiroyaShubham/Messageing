package messenger.messages.messaging.sms.chat.meet.org.smil;

import org.w3c.dom.DOMException;
import org.w3c.dom.Element;

public interface Time {
    public boolean getResolved();
    public double getResolvedOffset();

    // TimeTypes
    public static final short SMIL_TIME_INDEFINITE      = 0;
    public static final short SMIL_TIME_OFFSET          = 1;
    public static final short SMIL_TIME_SYNC_BASED      = 2;
    public static final short SMIL_TIME_EVENT_BASED     = 3;
    public static final short SMIL_TIME_WALLCLOCK       = 4;
    public static final short SMIL_TIME_MEDIA_MARKER    = 5;

    public short getTimeType();

    public double getOffset();
    public void setOffset(double offset)
                                      throws DOMException;

    public Element getBaseElement();
    public void setBaseElement(Element baseElement)
                                      throws DOMException;

    public boolean getBaseBegin();
    public void setBaseBegin(boolean baseBegin)
                                      throws DOMException;

    public String getEvent();
    public void setEvent(String event)
                                      throws DOMException;

    public String getMarker();
    public void setMarker(String marker)
                                      throws DOMException;

}

