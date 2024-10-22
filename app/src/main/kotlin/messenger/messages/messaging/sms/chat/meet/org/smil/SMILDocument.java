package messenger.messages.messaging.sms.chat.meet.org.smil;

import org.w3c.dom.Document;

public interface SMILDocument extends Document, ElementSequentialTimeContainer {

    public SMILElement getHead();
    public SMILElement getBody();

    public SMILLayoutElement getLayout();
}

