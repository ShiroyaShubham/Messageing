package messenger.messages.messaging.sms.chat.meet.org.smil;

import org.w3c.dom.NodeList;

public interface SMILLayoutElement extends SMILElement {
    public String getType();
    public boolean getResolved();
    public SMILRootLayoutElement getRootLayout();
    public NodeList getRegions();
}

