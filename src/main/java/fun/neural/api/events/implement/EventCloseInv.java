package fun.neural.api.events.implement;

import lombok.AllArgsConstructor;
import fun.neural.api.events.Event;

@AllArgsConstructor
public class EventCloseInv extends Event {
    public int windowId;
}

