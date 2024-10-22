package messenger.messages.messaging.sms.chat.meet.org.smil;

import org.w3c.dom.NodeList;

public interface ElementTimeContainer extends ElementTime {

    public NodeList getTimeChildren();
    public NodeList getActiveChildrenAt(float instant);

}

