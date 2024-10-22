package messenger.messages.messaging.sms.chat.meet.send_message;

import android.os.Build;

import android.util.Log;

public class Settings {

    public static final int DEFAULT_SUBSCRIPTION_ID = -1;

    // MMS options
    private String mmsc;
    private String proxy;
    private String port;
    private String userAgent;
    private String uaProfUrl;
    private String uaProfTagName;
    private boolean group;
    private boolean useSystemSending;

    // SMS options
    private boolean deliveryReports;
    private boolean split;
    private boolean splitCounter;
    private boolean stripUnicode;
    private String signature;
    private String preText;
    private boolean sendLongAsMms;
    private int sendLongAsMmsAfter;
    private int subscriptionId = DEFAULT_SUBSCRIPTION_ID;
    public Settings() {
        this("", "", "0", true, false, false, false, false, "", "", true, 3, true, DEFAULT_SUBSCRIPTION_ID);
    }

    public Settings(Settings s) {
        this.mmsc = s.getMmsc();
        this.proxy = s.getProxy();
        this.port = s.getPort();
        this.userAgent = s.getAgent();
        this.uaProfUrl = s.getUserProfileUrl();
        this.uaProfTagName = s.getUaProfTagName();
        this.group = s.getGroup();
        this.deliveryReports = s.getDeliveryReports();
        this.split = s.getSplit();
        this.splitCounter = s.getSplitCounter();
        this.stripUnicode = s.getStripUnicode();
        this.signature = s.getSignature();
        this.preText = s.getPreText();
        this.sendLongAsMms = s.getSendLongAsMms();
        this.sendLongAsMmsAfter = s.getSendLongAsMmsAfter();
        this.subscriptionId = s.getSubscriptionId();
    }

    public Settings(String mmsc, String proxy, String port, boolean group,
                    boolean deliveryReports, boolean split, boolean splitCounter,
                    boolean stripUnicode, String signature, String preText, boolean sendLongAsMms,
                    int sendLongAsMmsAfter, boolean useSystemSending, Integer subscriptionId) {
        this.mmsc = mmsc;
        this.proxy = proxy;
        this.port = port;
        this.userAgent = "";
        this.uaProfUrl = "";
        this.uaProfTagName = "";
        this.group = group;
        this.deliveryReports = deliveryReports;
        this.split = split;
        this.splitCounter = splitCounter;
        this.stripUnicode = stripUnicode;
        this.signature = signature;
        this.preText = preText;
        this.sendLongAsMms = sendLongAsMms;
        this.sendLongAsMmsAfter = sendLongAsMmsAfter;
        setUseSystemSending(useSystemSending);

        this.subscriptionId = subscriptionId != null ? subscriptionId : DEFAULT_SUBSCRIPTION_ID;
    }

    public void setMmsc(String mmsc) {
        this.mmsc = mmsc;
    }
    public void setProxy(String proxy) {
        this.proxy = proxy;
    }
    public void setPort(String port) {
        this.port = port;
    }
    public void setAgent(String agent) { this.userAgent = agent; }
    public void setUserProfileUrl(String userProfileUrl) { this.uaProfUrl = userProfileUrl; }
    public void setUaProfTagName(String tagName) { this.uaProfTagName = tagName; }
    public void setGroup(boolean group) {
        this.group = group;
    }
    public void setDeliveryReports(boolean deliveryReports) {
        this.deliveryReports = deliveryReports;
    }
    public void setSplit(boolean split) {
        this.split = split;
    }
    public void setSplitCounter(boolean splitCounter) {
        this.splitCounter = splitCounter;
    }
    public void setStripUnicode(boolean stripUnicode) {
        this.stripUnicode = stripUnicode;
    }

    public void setSignature(String signature) {
        this.signature = signature;
    }
    public void setPreText(String preText) {
        this.preText = preText;
    }
    public void setSendLongAsMms(boolean sendLongAsMms) {
        this.sendLongAsMms = sendLongAsMms;
    }
    public void setSendLongAsMmsAfter(int sendLongAsMmsAfter) {
        this.sendLongAsMmsAfter = sendLongAsMmsAfter;
    }

    public void setUseSystemSending(boolean useSystemSending) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            this.useSystemSending = useSystemSending;
        } else {
            this.useSystemSending = false;
            Log.e("Settings", "System sending only available on Lollipop+ devices");
        }
    }

    public void setSubscriptionId(Integer subscriptionId) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP_MR1 || subscriptionId == null) {
            // we won't allow you to go away from the default if your device doesn't support it
            this.subscriptionId = DEFAULT_SUBSCRIPTION_ID;
        } else {
            this.subscriptionId = subscriptionId;
        }
    }

    public String getMmsc() {
        return this.mmsc;
    }
    public String getProxy() {
        return this.proxy;
    }
    public String getPort() {
        return this.port;
    }
    public String getAgent() { return this.userAgent; }
    public String getUserProfileUrl() { return this.uaProfUrl; }
    public String getUaProfTagName() { return this.uaProfTagName; }
    public boolean getGroup() {
        return this.group;
    }
    public boolean getDeliveryReports() {
        return this.deliveryReports;
    }
    public boolean getSplit() {
        return this.split;
    }
    public boolean getSplitCounter() {
        return this.splitCounter;
    }
    public boolean getStripUnicode() {
        return this.stripUnicode;
    }
    public String getSignature() {
        return this.signature;
    }
    public String getPreText() {
        return this.preText;
    }
    public boolean getSendLongAsMms() {
        return this.sendLongAsMms;
    }
    public int getSendLongAsMmsAfter() {
        return this.sendLongAsMmsAfter;
    }
    public boolean getUseSystemSending() {
        return useSystemSending;
    }
    public int getSubscriptionId() {
        return subscriptionId;
    }

}
