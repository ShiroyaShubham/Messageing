package messenger.messages.messaging.sms.chat.meet.org.smil;

import org.w3c.dom.DOMException;
import org.w3c.dom.Element;

public interface SMILElement extends Element {
    public String getId();
    public void setId(String id)
                                      throws DOMException;

}

