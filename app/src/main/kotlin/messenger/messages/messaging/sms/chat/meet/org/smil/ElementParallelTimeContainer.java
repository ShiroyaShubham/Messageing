package messenger.messages.messaging.sms.chat.meet.org.smil;

import org.w3c.dom.DOMException;

public interface ElementParallelTimeContainer extends ElementTimeContainer {
    public String getEndSync();
    public void setEndSync(String endSync)
                                        throws DOMException;

    public float getImplicitDuration();

}

