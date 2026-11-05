package fun.neural.api.storages;


import fun.neural.Neural;
import fun.neural.api.QClient;
import fun.neural.api.events.EventInvoker;
import fun.neural.api.storages.implement.*;
import fun.neural.api.storages.implement.helpertstorages.enumvar.ModuleClass;
import fun.neural.api.utils.tps.TPSCalc;
import fun.neural.client.modules.impl.render.TotemAngel;

public class InitializeStorage implements QClient {

    public void onInitialize() {
        EventInvoker.register(this);
        this.initStorages();
    }

    
    public void initStorages() {
        Neural.INSTANCE.moduleStorage = new ModuleStorage();
        Neural.INSTANCE.themeStorage = new ThemeStorage();
        Neural.INSTANCE.tpsCalc = new TPSCalc();
        EventInvoker.register(Neural.INSTANCE.tpsCalc);
        Neural.INSTANCE.localizationStorage = new LocalizationStorage();
        Neural.INSTANCE.freeLookStorage = new FreeLookStorage();
        Neural.INSTANCE.rotationStorage = new RotationStorage();
        // Neural.INSTANCE.serverStorage = new ServerStorage();
        Neural.INSTANCE.friendStorage = new FriendStorage();
        Neural.INSTANCE.macroStorage = new MacroStorage();
        Neural.INSTANCE.staffStorage = new StaffStorage();
        Neural.INSTANCE.waypointStorage = new WaypointStorage();
        Neural.INSTANCE.commandStorage = new CommandStorage();
        Neural.INSTANCE.configStorage = new ConfigStorage();
    }
}
