package messenger.messages.messaging.sms.chat.meet.internal.telephony;

@SuppressWarnings("serial")
public class EncodeException extends Exception {
    public EncodeException() {
        super();
    }

    public EncodeException(String s) {
        super(s);
    }

    public EncodeException(char c) {
        super("Unencodable char: '" + c + "'");
    }
}

