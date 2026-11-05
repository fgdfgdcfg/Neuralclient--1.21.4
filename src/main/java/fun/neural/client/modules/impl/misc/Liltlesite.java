package fun.neural.client.modules.impl.misc;

import fun.neural.client.modules.Module;
import fun.neural.api.storages.implement.helpertstorages.enumvar.ModuleClass;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Liltlesite extends Module {

    public static Liltlesite INSTANCE = new Liltlesite();

    private static final List<String> ALLOWED = List.of(
            "Interface", "ClientSounds", "FullBright", "NoVignette"
    );

    private final List<Module> hooked = new ArrayList<>();
    private final List<Integer> hookedIndices = new ArrayList<>();
    private final List<Boolean> wereEnabled = new ArrayList<>();

    public Liltlesite() {
        super("liltlesite", "Скрывает и отключает все функции, бинд возвращает всё обратно", ModuleCategory.MISC);
        setKey(GLFW.GLFW_KEY_RIGHT_CONTROL);
    }

    @Override
    public void onEnable() {
        super.onEnable();
        hooked.clear();
        hookedIndices.clear();
        wereEnabled.clear();

        List<Module> all = ModuleClass.INSTANCE.getObject();
        for (int i = all.size() - 1; i >= 0; i--) {
            Module module = all.get(i);
            if (module == this || ALLOWED.contains(module.getName())) {
                continue;
            }
            hookedIndices.add(i);
            hooked.add(module);
            boolean enabled = module.isEnable();
            wereEnabled.add(enabled);
            all.remove(i);
            if (enabled) {
                module.onDisable();
            }
        }
        Collections.reverse(hooked);
        Collections.reverse(hookedIndices);
        Collections.reverse(wereEnabled);
    }

    @Override
    public void onDisable() {
        super.onDisable();
        List<Module> all = ModuleClass.INSTANCE.getObject();
        for (int k = 0; k < hooked.size(); k++) {
            Module module = hooked.get(k);
            int index = Math.min(hookedIndices.get(k), all.size());
            all.add(index, module);
            if (wereEnabled.get(k)) {
                module.onEnable();
            }
        }
        hooked.clear();
        hookedIndices.clear();
        wereEnabled.clear();
    }
}
