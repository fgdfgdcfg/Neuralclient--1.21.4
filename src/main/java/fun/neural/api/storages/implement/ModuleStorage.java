package fun.neural.api.storages.implement;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import lombok.Getter;
import lombok.Setter;
import fun.neural.api.QClient;
import fun.neural.api.storages.implement.helpertstorages.enumvar.ModuleClass;
import fun.neural.client.modules.Module;
import fun.neural.client.modules.impl.combat.*;
import fun.neural.client.modules.impl.misc.*;
import fun.neural.client.modules.impl.movement.*;
import fun.neural.client.modules.impl.player.*;
import fun.neural.client.modules.impl.render.*;
import java.util.Arrays;

@Getter
@Setter
public class ModuleStorage implements QClient {

    public ModuleStorage() {
        this.initModules();
    }

    private void initModules() {
        ModuleClass.INSTANCE.initialize();
    }
}
