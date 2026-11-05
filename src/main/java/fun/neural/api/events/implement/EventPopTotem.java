package fun.neural.api.events.implement;

import lombok.AllArgsConstructor;
import lombok.Getter;
import net.minecraft.entity.player.PlayerEntity;
import fun.neural.api.events.Event;

@AllArgsConstructor @Getter
public class EventPopTotem extends Event {
    private final PlayerEntity player;
}