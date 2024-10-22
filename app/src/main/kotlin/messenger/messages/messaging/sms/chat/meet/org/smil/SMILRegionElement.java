package messenger.messages.messaging.sms.chat.meet.org.smil;

import org.w3c.dom.DOMException;

public interface SMILRegionElement extends SMILElement, ElementLayout {
    public String getFit();
    public void setFit(String fit) throws DOMException;

    public int getLeft();
    public void setLeft(int top) throws DOMException;

    public int getTop();
    public void setTop(int top) throws DOMException;

    public int getZIndex();
    public void setZIndex(int zIndex) throws DOMException;

}

