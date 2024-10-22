package messenger.messages.messaging.sms.chat.meet.mms.dom.smil;

import messenger.messages.messaging.sms.chat.meet.org.smil.ElementTimeContainer;
import messenger.messages.messaging.sms.chat.meet.org.smil.SMILElement;

public abstract class ElementTimeContainerImpl extends ElementTimeImpl implements
        ElementTimeContainer {

    ElementTimeContainerImpl(SMILElement element) {
        super(element);
    }
}
