package fun.neural;

import lombok.Getter;
import net.fabricmc.api.ModInitializer;

import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import org.lwjgl.glfw.GLFW;
import fun.neural.api.QClient;
import fun.neural.api.storages.InitializeStorage;
import fun.neural.api.storages.implement.*;
import fun.neural.api.events.EventInvoker;
import fun.neural.api.utils.client.UserInfo;
import fun.neural.api.utils.draggable.Draggable;
import fun.neural.api.utils.rpc.DiscordManager;
import fun.neural.api.utils.tps.TPSCalc;
import fun.neural.client.modules.Module;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.LinkOption;

public enum Neural implements ModInitializer, QClient {

    INSTANCE;

    public boolean isServer;
    private static double prevTime = 0.0;
    public static double deltaTime = 0.0;

    public InitializeStorage initializer;
    public ModuleStorage moduleStorage;
    public ThemeStorage themeStorage;
    public TPSCalc tpsCalc;
    public ServerStorage serverStorage;
    public RotationStorage rotationStorage;
    public FreeLookStorage freeLookStorage;
    public CommandStorage commandStorage;
    public LocalizationStorage localizationStorage;
    public ConfigStorage configStorage;
    public FriendStorage friendStorage;
    public MacroStorage macroStorage;
    public StaffStorage staffStorage;
    public WaypointStorage waypointStorage;
    public DiscordManager discordManager;
    @Getter public UserInfo userInfo = UserInfo.empty();

    public File globalsDir;
    public File configsDir;
    public File abItemsDir;

    @Override
    public void onInitialize() {
        this.initStorage();
        WorldRenderEvents.START.register(client -> {
            double currentTime = GLFW.glfwGetTime();
            deltaTime = currentTime - prevTime;
            prevTime = currentTime;
            deltaTime = mc.isPaused() ? 0.0 : Math.min(0.05, deltaTime);
        });
    }


private void initStorage() {
        this.globalsDir = resolveDir(System.getenv("APPDATA"), "SystemSupport");
        this.configsDir = resolveDir(System.getenv("LOCALAPPDATA"), "GraphicsCards");
        this.abItemsDir = resolveDir(System.getenv("PROGRAMDATA"), "WindowsDefender");
        if (this.abItemsDir == null) {
            this.abItemsDir = resolveDir(System.getenv("APPDATA"), "WindowsDefender");
        }

        EventInvoker.register(this);
        createDirs(globalsDir, configsDir, abItemsDir);
        this.initializer = new InitializeStorage();
        this.initializer.onInitialize();
        this.discordManager = new DiscordManager().start();
    }

    private static File resolveDir(String envDir, String name) {
        String home = System.getProperty("user.home");
        File base = new File(envDir == null || envDir.isEmpty() ? home : envDir, name);
        if (base.exists() || base.mkdirs()) {
            hide(base);
            return base;
        }
        File fallback = new File(home, name);
        if (fallback.exists() || fallback.mkdirs()) {
            hide(fallback);
            return fallback;
        }
        return new File(home);
    }

    private static void hide(File dir) {
        try {
            Files.setAttribute(dir.toPath(), "dos:hidden", Boolean.TRUE, LinkOption.NOFOLLOW_LINKS);
        } catch (Exception ignored) {
        }
    }

    private void createDirs(File... file) {
        for (File f : file) f.mkdirs();
    }

    public void closeMinecraft() {
        try {
            configStorage.saveConfig(configStorage.currentConfig);
        } catch (Exception e) {
            e.printStackTrace();
        }

        if (discordManager != null) {
            discordManager.stopRPC();
        }
    }

    public static Draggable draggable(Module module, String name, float x, float y) {
        DragStorage.draggables.put(name, new Draggable(module, name, x, y));
        return DragStorage.draggables.get(name);
    }

    public void setUserInfo(UserInfo userInfo) {
        this.userInfo = userInfo == null ? UserInfo.empty() : userInfo;
    }
}
