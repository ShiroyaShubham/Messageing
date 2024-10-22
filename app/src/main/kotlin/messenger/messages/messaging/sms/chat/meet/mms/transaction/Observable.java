package messenger.messages.messaging.sms.chat.meet.mms.transaction;

import java.util.ArrayList;
import java.util.Iterator;

public abstract class Observable {
    private final ArrayList<Observer> mObservers;
    private Iterator<Observer> mIterator;

    public Observable() {
        mObservers = new ArrayList<Observer>();
    }

    abstract public TransactionState getState();
    public void attach(Observer observer) {
        mObservers.add(observer);
    }

    public void detach(Observer observer) {
        if (mIterator != null) {
            mIterator.remove();
        } else {
            mObservers.remove(observer);
        }
    }

    public void notifyObservers() {
        mIterator = mObservers.iterator();
        try {
            while (mIterator.hasNext()) {
                mIterator.next().update(this);
            }
        } finally {
            mIterator = null;
        }
    }
}
