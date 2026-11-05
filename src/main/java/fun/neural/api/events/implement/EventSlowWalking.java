package fun.neural.api.events.implement;

import fun.neural.api.events.Event;

public class EventSlowWalking extends Event {

    private boolean cancelled;

    public boolean isCancelled() {
        return cancelled;
    }

    public void setCancelled(boolean cancelled) {
        this.cancelled = cancelled;
    }
}