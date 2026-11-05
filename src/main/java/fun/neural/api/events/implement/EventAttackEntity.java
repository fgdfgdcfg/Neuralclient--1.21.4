package fun.neural.api.events.implement;

import lombok.AllArgsConstructor;
import lombok.Getter;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import fun.neural.api.events.Event;

@AllArgsConstructor @Getter
public class EventAttackEntity extends Event {
    private final PlayerEntity player;
    private final Entity target;
}