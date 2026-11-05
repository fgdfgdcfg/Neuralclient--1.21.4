package fun.neural.client.modules.settings;

import lombok.Getter;
import lombok.experimental.Accessors;
import fun.neural.Neural;
import fun.neural.api.QClient;

import java.awt.*;
import java.util.function.Supplier;

@Getter
@Accessors(fluent = true)
public abstract class Setting implements QClient {

    private final String name;
    public Supplier<Boolean> visible = () -> true;
    public Color color = Color.WHITE;

    public Setting(String name) {
        this.name = name;
    }

    public Boolean visible() {
        return visible.get();
    }

    public String displayName() {
        return Neural.INSTANCE.localizationStorage == null ? name : Neural.INSTANCE.localizationStorage.translate(name);
    }
}
