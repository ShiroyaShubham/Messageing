package messenger.messages.messaging.sms.chat.meet.org.smil;

import org.w3c.dom.DOMException;

public interface ElementLayout {
    public String getTitle();
    public void setTitle(String title) throws DOMException;
    public String getBackgroundColor();
    public void setBackgroundColor(String backgroundColor)
                                      throws DOMException;

    public int getHeight();
    public void setHeight(int height)
                                      throws DOMException;

    public int getWidth();
    public void setWidth(int width)
                                      throws DOMException;

}

