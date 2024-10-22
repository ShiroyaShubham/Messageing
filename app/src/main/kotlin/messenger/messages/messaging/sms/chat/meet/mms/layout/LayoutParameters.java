package messenger.messages.messaging.sms.chat.meet.mms.layout;

public interface LayoutParameters {
    public static final int UNKNOWN        = -1;
    public static final int HVGA_LANDSCAPE = 10;
    public static final int HVGA_PORTRAIT  = 11;
    public static final int HVGA_LANDSCAPE_WIDTH  = 480;
    public static final int HVGA_LANDSCAPE_HEIGHT = 320;
    public static final int HVGA_PORTRAIT_WIDTH   = 320;
    public static final int HVGA_PORTRAIT_HEIGHT  = 480;

    int getWidth();
    int getHeight();
    int getImageHeight();
    int getTextHeight();
    int getType();
    String getTypeDescription();
}
