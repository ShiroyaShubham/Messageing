package messenger.messages.messaging.sms.chat.meet.org.smil;

import org.w3c.dom.DOMException;

public interface ElementTime {
    public TimeList getBegin();
    public void setBegin(TimeList begin)
                     throws DOMException;
    public TimeList getEnd();
    public void setEnd(TimeList end)
                     throws DOMException;

    public float getDur();
    public void setDur(float dur)
                     throws DOMException;

    // restartTypes
    public static final short RESTART_ALWAYS            = 0;
    public static final short RESTART_NEVER             = 1;
    public static final short RESTART_WHEN_NOT_ACTIVE   = 2;

    public short getRestart();
    public void setRestart(short restart)
                     throws DOMException;

    // fillTypes
    public static final short FILL_REMOVE               = 0;
    public static final short FILL_FREEZE               = 1;
    public static final short FILL_AUTO                 = 2;

    public short getFill();
    public void setFill(short fill)
                     throws DOMException;

    public float getRepeatCount();
    public void setRepeatCount(float repeatCount)
                     throws DOMException;
    public float getRepeatDur();
    public void setRepeatDur(float repeatDur)
                     throws DOMException;

    public boolean beginElement();

    public boolean endElement();

    public void pauseElement();

    public void resumeElement();

    public void seekElement(float seekTo);

    public short getFillDefault();
    public void setFillDefault(short fillDefault)
                     throws DOMException;

}

