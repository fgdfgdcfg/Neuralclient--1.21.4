package fun.neural.client.modules.impl.render;

import fun.neural.client.modules.Module;

public class NoVignette extends Module {

    public static NoVignette INSTANCE = new NoVignette();
    public NoVignette() {
        super("NoVignette", "Убирает затемнения на краях экрана", ModuleCategory.RENDER);
    }
}

