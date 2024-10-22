package messenger.messages.messaging.sms.chat.meet.mms.dom.smil;

import org.w3c.dom.DOMException;
import messenger.messages.messaging.sms.chat.meet.org.smil.SMILElement;

import messenger.messages.messaging.sms.chat.meet.mms.dom.ElementImpl;

public class SmilElementImpl extends ElementImpl implements SMILElement {
    SmilElementImpl(SmilDocumentImpl owner, String tagName)
    {
        super(owner, tagName.toLowerCase());
    }

    public String getId() {
        // TODO Auto-generated method stub
        return null;
    }

    public void setId(String id) throws DOMException {
        // TODO Auto-generated method stub

    }

}
