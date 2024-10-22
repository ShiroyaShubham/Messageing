package messenger.messages.messaging.sms.chat.meet.org.smil;

import org.w3c.dom.DOMException;

public interface SMILMediaElement extends ElementTime, SMILElement {
    public String getAbstractAttr();
    public void setAbstractAttr(String abstractAttr)
                              throws DOMException;
    public String getAlt();
    public void setAlt(String alt)
                              throws DOMException;
    public String getAuthor();
    public void setAuthor(String author)
                              throws DOMException;
    public String getClipBegin();
    public void setClipBegin(String clipBegin)
                              throws DOMException;
    public String getClipEnd();
    public void setClipEnd(String clipEnd)
                              throws DOMException;
    public String getCopyright();
    public void setCopyright(String copyright)
                              throws DOMException;
    public String getLongdesc();
    public void setLongdesc(String longdesc)
                              throws DOMException;

    public String getPort();
    public void setPort(String port)
                              throws DOMException;

    public String getReadIndex();
    public void setReadIndex(String readIndex)
                              throws DOMException;

    public String getRtpformat();
    public void setRtpformat(String rtpformat)
                              throws DOMException;

    public String getSrc();
    public void setSrc(String src)
                              throws DOMException;

    public String getStripRepeat();
    public void setStripRepeat(String stripRepeat)
                              throws DOMException;

    public String getTitle();
    public void setTitle(String title)
                              throws DOMException;

    public String getTransport();
    public void setTransport(String transport)
                              throws DOMException;

    public String getType();
    public void setType(String type)
                              throws DOMException;

}

