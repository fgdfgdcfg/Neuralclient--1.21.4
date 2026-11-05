package fun.neural.client.modules.impl.render;

import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.util.SkinTextures;
import net.minecraft.util.Identifier;
import fun.neural.api.utils.chat.ChatUtils;
import fun.neural.client.modules.Module;
import fun.neural.client.modules.settings.implement.ModeSetting;

import java.util.UUID;

public class SkinModel extends Module {

    public static SkinModel INSTANCE = new SkinModel();

    private final ModeSetting skin = new ModeSetting("Скин", "Freddy", "Ink Demon", "Rabbit", "Tung Tung Tung Sahur");

    private SkinTextures cachedSkin;
    private String cachedSkinName;

    public SkinModel() {
        super("Skin Model", "Меняет твой скин только для тебя", ModuleCategory.RENDER);
        addSettings(skin);
    }

    @Override
    public void onEnable() {
        cachedSkin = null;
        cachedSkinName = null;
        super.onEnable();
        ChatUtils.sendMessage("§7[§bNeural§7] §fСкин применён: §a" + skin.displayCurrent() + " §7(только для тебя)");
    }

    @Override
    public void onDisable() {
        cachedSkin = null;
        cachedSkinName = null;
        super.onDisable();
    }

    public static SkinTextures overrideSkin(AbstractClientPlayerEntity player, SkinTextures original) {
        SkinModel instance = INSTANCE;
        if (instance == null || !instance.isEnable()) {
            return null;
        }
        if (player == null || original == null || instance.mc.player == null || player != instance.mc.player) {
            return null;
        }
        return instance.resolveSkin(original);
    }

    public static SkinTextures overrideSkin(UUID uuid, SkinTextures original) {
        SkinModel instance = INSTANCE;
        if (instance == null || !instance.isEnable()) {
            return null;
        }
        if (uuid == null || original == null || instance.mc.player == null || !uuid.equals(instance.mc.player.getUuid())) {
            return null;
        }
        return instance.resolveSkin(original);
    }

    private SkinTextures resolveSkin(SkinTextures original) {
        String name = skin.getCurrent();
        if (name.equals(cachedSkinName) && cachedSkin != null) {
            return cachedSkin;
        }
        String file = name.toLowerCase().replace(' ', '_');
        cachedSkin = new SkinTextures(
                Identifier.of("neural", "textures/skins/" + file + ".png"),
                null,
                original.capeTexture(),
                original.elytraTexture(),
                original.model(),
                true
        );
        cachedSkinName = name;
        return cachedSkin;
    }
}
