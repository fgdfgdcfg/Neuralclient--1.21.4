package fun.neural.api.utils.rpc;

import lombok.Getter;
import net.minecraft.client.gui.screen.TitleScreen;
import net.minecraft.client.gui.screen.multiplayer.MultiplayerScreen;
import net.minecraft.client.gui.screen.world.SelectWorldScreen;
import net.minecraft.client.network.ServerInfo;
import fun.neural.api.QClient;
import fun.neural.api.utils.rpc.utils.DiscordEventHandlers;
import fun.neural.api.utils.rpc.utils.DiscordRPC;
import fun.neural.api.utils.rpc.utils.DiscordRichPresence;
import fun.neural.client.modules.impl.render.base.implement.WaterMark;

@Getter
public class DiscordManager implements QClient {

    private DiscordDaemonThread discordDaemonThread;
    private long APPLICATION_ID;

    private boolean running;

    private String image;
    private String site;
    private String telegram;

    private void cppInit() {
        discordDaemonThread = new DiscordDaemonThread();
        APPLICATION_ID = 1543078497084514334L;
        running = true;
        image = "https://raw.githubusercontent.com/fgdfgdcfg/gfdg/main/6db3fe0a34711f1ab1026c1e796819c%20(online-video-cutter.com)%20(1).gif";
        site = "https://neuralclient.ru/";
        telegram = "https://t.me/neuralclient";
    }

    String state = "";

    public static DiscordRichPresence discordRichPresence = new DiscordRichPresence();
    public static DiscordRPC discordRPC = DiscordRPC.INSTANCE;

    public void init() {
        cppInit();
        DiscordEventHandlers handlers = new DiscordEventHandlers.Builder().build();

        DiscordRPC.INSTANCE.Discord_Initialize(String.valueOf(APPLICATION_ID), handlers, true, "");
        discordRichPresence.startTimestamp = System.currentTimeMillis() / 1000L;
        discordRPC.Discord_UpdatePresence(discordRichPresence);

        new Thread(() -> {
            while (running) {
                try {
                    discordRichPresence.details = "Разработчик » 𝙼𝚘𝚗𝟶𝚕𝚘𝚐";
                    discordRichPresence.state = "Name » " + WaterMark.getUsername() + " | UID » " + WaterMark.getUID();
                    discordRichPresence.largeImageKey = image;
                    discordRichPresence.largeImageText = "NeuralClient";
                    discordRichPresence.button_label_1 = "Купить";
                    discordRichPresence.button_url_1 = site;
                    discordRichPresence.button_label_2 = "Телеграмм";
                    discordRichPresence.button_url_2 = telegram;
                    DiscordRPC.INSTANCE.Discord_UpdatePresence(discordRichPresence);
                    Thread.sleep(2000);
                } catch (InterruptedException ignored) {
                    Thread.currentThread().interrupt();
                    return;
                }
            }
        }, "Discord-RPC-Updater").start();

        discordDaemonThread.start();
    }

    public DiscordManager start() {
        init();
        return this;
    }

    public void stopRPC() {
        running = false;
        DiscordRPC.INSTANCE.Discord_Shutdown();
        if (discordDaemonThread != null) {
            discordDaemonThread.interrupt();
        }
    }

    private class DiscordDaemonThread extends Thread {
        @Override
        public void run() {
            this.setName("Discord-RPC");

            try {
                while (running) {
                    DiscordRPC.INSTANCE.Discord_RunCallbacks();
                    Thread.sleep(15 * 1000);
                }
            } catch (Exception exception) {
                stopRPC();
            }

            super.run();
        }
    }
}
