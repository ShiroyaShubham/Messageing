package messenger.messages.messaging.sms.chat.meet.org.smil;

public interface ElementTargetAttributes {
    public String getAttributeName();
    public void setAttributeName(String attributeName);
    public static final short ATTRIBUTE_TYPE_AUTO       = 0;
    public static final short ATTRIBUTE_TYPE_CSS        = 1;
    public static final short ATTRIBUTE_TYPE_XML        = 2;
    public short getAttributeType();
    public void setAttributeType(short attributeType);

}

