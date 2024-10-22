package messenger.messages.messaging.sms.chat.meet.org.events;

public interface EventTarget {
    public void addEventListener(String type, 
                                 EventListener listener, 
                                 boolean useCapture);
    public void removeEventListener(String type, 
                                    EventListener listener, 
                                    boolean useCapture);

    public boolean dispatchEvent(Event evt)
                                 throws EventException;

}
