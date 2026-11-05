package fun.neural.client.modules.impl.render;

import fun.neural.client.modules.Module;
import fun.neural.client.modules.settings.implement.ModeSetting;
import fun.neural.mixin.ItemRenderStateAccessor;
import fun.neural.mixin.LayerRenderStateAccessor;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.item.ItemRenderState;
import net.minecraft.client.render.item.model.ItemModel;
import net.minecraft.client.render.model.BakedModel;
import net.minecraft.client.render.model.BakedModelManager;
import net.minecraft.client.util.ModelIdentifier;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.CustomModelDataComponent;
import net.minecraft.item.ItemStack;
import net.minecraft.item.SwordItem;
import net.minecraft.util.Identifier;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ItemSkins extends Module {

    public static final String[] MODELS = {
            "Pheonix Grace", "Abominable Great Saber", "Abominable Blade", "Abominable Scythe",
            "Acidic Cleaver", "Amethyst Shuriken", "Ancient Royal Great Sword", "Aquatic Sacred Blade",
            "Ocean's Rage", "Ashura's Blade", "Blood Edge", "Bloody Death",
            "Carian Knight's Sword", "Chrono Blade", "Corrupted Mythic Blade", "Creation Splitter",
            "Crescent Rose", "Cyber Katana", "Cyber Mantis Blade", "Cybernetic Katana",
            "Cybernetic Knife", "Cybernetic Chainsaw Blade", "Cyber Sword", "Dainsleif",
            "Dark Blade", "Dark Cleaver", "Death Knight's Dagger", "Death Knight's Sword",
            "Demigod's Unholy Halberd", "Demigod's Unholy Blade", "Demonic Blade", "Demonic Cleaver",
            "Demon Lord's Great Axe", "Demon Lord's Sword", "Divine Justice", "Divine Reaper",
            "Divine Punisher", "Divine Axe Rhitta", "Dragon Slaying blade", "Edge Of The Astral Plane",
            "Epic Sword", "Estoc", "Fallen God's Spear", "Fallen God's Sword",
            "Floral Longsword", "Floral Sabre", "Forest Guardian's Glaive", "Frost Axe",
            "Frost Blade", "Frost Scythe", "Wraith Scythe", "Hero Sword",
            "Holy Moonlight Sword", "Hornet's Needle", "Jade Halberd", "Katana",
            "Legendary Sword", "Longsword", "Masamune", "Mjolnir",
            "Molten Blade", "Molten Sword", "Muramasa", "Mystical Spellblade",
            "Mythic Blade", "Partisan", "Pharaoh's Treasure", "Power Fuse Hammer",
            "Power Fuse Sword", "Rivers Of Blood", "Royal Chakram", "Royal Rapier",
            "Sabre", "Sculk Cleaver", "Scissor Blade", "Sculk Scythe",
            "Sculk Sword", "Silverine Blade", "Sentinel's Will", "Soul Devourer",
            "Soul Collector", "Soul Edge", "Soul Claws", "Steel Sword",
            "Soul Harvester", "Tengen's Blade", "Stop Sign", "Sunbreak",
            "Thunder Bringer", "Terra Blade", "Thousand Demon Daggers", "Yoru",
            "Plague Longsword", "Vampiric Needle", "Wakizashi", "Whisperwind",
            "Wickpiercer", "Thunderbrand", "Watcher Claymore", "Watching Warglaive",
            "Waxweaver", "Star's Edge", "Storm Bringer", "Storm's Edge",
            "Ribbon Cleaver", "Righteous Relic", "Soulrender", "Soul Stealer",
            "Enigma", "Hearthflame", "Icewhisper", "Magi Scythe",
            "Arcanethyst", "Awakened Lichblade", "Bramblethorn", "Brimstone Claymore",
            "Emberblade", "True Excalibur", "Requiem of the Ninth Abyss"
    };

    public static final String[] MODEL_FILES = {
            "pheonixgrace",
            "abominablegreatsaber",
            "abominableblade",
            "abominablescythe",
            "aciddemon",
            "amethyst_shuriken",
            "ancient_royal_great_sword",
            "aquantic_sacred_blade",
            "aquantictrident",
            "ashura_blade",
            "bloodedge",
            "bloodydeath",
            "cariansword",
            "chrono_blade",
            "corruptedmythicblade",
            "creationsplitter",
            "crescentrose",
            "cyberkatana",
            "cybermantisblade",
            "cybernetickatana",
            "cyberneticknife",
            "cyberneticsawblade",
            "cybersword",
            "dainsleif",
            "dark_blade",
            "dark_cleaver",
            "death_knight_dagger",
            "death_knight_sword",
            "demigodsunholyhalberd",
            "demigodsunholyblade",
            "demonicblade",
            "demoniccleaver",
            "demonlordsgreataxe",
            "demonlordsword",
            "divine_justice",
            "divine_reaper",
            "divinepunisher",
            "divineaxerhitta",
            "dragonslayingblade",
            "edgeoftheastralplane",
            "epicsword",
            "estoc",
            "fallengodspear",
            "fallengodsword",
            "floral_longsword",
            "floral_sabre",
            "forest_guardian_glaive",
            "frostaxe",
            "frostblade",
            "frostscythe",
            "greenscythe",
            "herosword",
            "holymoonlightsword",
            "hornetsneedle",
            "jadehalberd",
            "katana",
            "legendarysword",
            "longsword",
            "masamune",
            "mjolnir",
            "moltenblade",
            "moltensword",
            "muramasa",
            "mysticalspellblade",
            "mythicblade",
            "partisan",
            "pharaohs_treasure",
            "powerfusehammer",
            "powerfusesword",
            "riversofblood",
            "royalchakram",
            "royalrapier",
            "sabre",
            "sculkcleaver",
            "scissorblade",
            "sculkscythe",
            "sculksword",
            "silverine_blade",
            "sentinels_will",
            "soul_devourer",
            "soul_collector",
            "souledge",
            "soulclaws",
            "steelsword",
            "soulharvester",
            "tengensblade",
            "stop_sign",
            "sunbreak",
            "thunderbringer",
            "terrablade",
            "thousanddemondaggers",
            "yoru",
            "toxic_longsword",
            "vampiricneedle",
            "wakizashi",
            "whisperwind",
            "wickpiercer",
            "thunderbrand",
            "watcher_claymore",
            "watching_warglaive",
            "waxweaver",
            "stars_edge",
            "stormbringer",
            "storms_edge",
            "ribboncleaver",
            "righteous_relic",
            "soulrender",
            "soulstealer",
            "enigma",
            "hearthflame",
            "icewhisper",
            "magiscythe",
            "arcanethyst",
            "awakened_lichblade",
            "bramblethorn",
            "brimstone_claymore",
            "emberblade",
            "excalibur",
            "requiem_of_hell"
    };

    public static final ItemSkins INSTANCE = new ItemSkins();

    private final ModeSetting model = new ModeSetting("Модель", "Katana", MODELS);

    public ItemSkins() {
        super("ItemSkins", "Заменяет визуальную модель мечей", ModuleCategory.RENDER);
        addSettings(model);
    }

    public static boolean isSword(ItemStack stack) {
        return stack != null && !stack.isEmpty() && stack.getItem() instanceof SwordItem;
    }

    public static ItemStack getRenderStack(ItemStack stack) {
        if (INSTANCE.isEnable() && isSword(stack)) {
            String name = MODELS[currentIndex()];
            if (name != null && !name.equals("minecraft:air")) {
                ItemStack copy = stack.copy();
                copy.set(DataComponentTypes.ITEM_MODEL, Identifier.of("minecraft", "neural_sword"));
                copy.set(DataComponentTypes.CUSTOM_MODEL_DATA,
                        new CustomModelDataComponent(List.of(), List.of(), List.of(name), List.of()));
                return copy;
            }
        }
        return stack;
    }

    private static final Map<String, BakedModel> MODEL_CACHE = new HashMap<>();

    public static void applySkin(ItemRenderState state, ItemStack stack) {
        if (!INSTANCE.isEnable() || !isSword(stack)) {
            return;
        }
        BakedModel model = getBakedModel();
        if (model == null) {
            return;
        }
        ItemRenderState.LayerRenderState[] layers = ((ItemRenderStateAccessor) (Object) state).neural$getLayers();
        if (layers == null || layers.length == 0 || layers[0] == null) {
            return;
        }
        LayerRenderStateAccessor layer = (LayerRenderStateAccessor) (Object) layers[0];
        if (layer.neural$getModel() == model) {
            return;
        }
        layer.neural$setModel(model);
    }

    public static BakedModel getBakedModel() {
        int index = currentIndex();
        if (index < 0 || index >= MODEL_FILES.length) {
            return null;
        }
        String file = MODEL_FILES[index];
        if (file == null || file.isEmpty()) {
            return null;
        }
        BakedModel cached = MODEL_CACHE.get(file);
        if (cached != null) {
            return cached;
        }
        BakedModel model = resolveBakedModel(file);
        if (model != null) {
            MODEL_CACHE.put(file, model);
        }
        return model;
    }

    private static BakedModel resolveBakedModel(String file) {
        MinecraftClient client = MinecraftClient.getInstance();
        BakedModelManager bmm = client.getBakedModelManager();
        if (bmm == null) {
            return null;
        }
        try {
            BakedModel missing = bmm.getModel(new ModelIdentifier(Identifier.ofVanilla("builtin/missing"), "inventory"));
            BakedModel model = bmm.getModel(new ModelIdentifier(Identifier.of("neural", "item/" + file), "inventory"));
            if (model != null && model != missing) {
                return model;
            }
        } catch (Throwable t) {
            // fall through
        }
        try {
            Identifier id = Identifier.of("minecraft", "neural_sword");
            ItemModel itemModel = bmm.getItemModel(id);
            if (itemModel == null) {
                return null;
            }
            ItemRenderState temp = new ItemRenderState();
            ItemStack tempStack = new ItemStack(net.minecraft.item.Items.IRON_SWORD);
            tempStack.set(DataComponentTypes.CUSTOM_MODEL_DATA,
                    new CustomModelDataComponent(List.of(), List.of(), List.of(MODELS[currentIndex()]), List.of()));
            itemModel.update(temp, tempStack, client.getItemModelManager(),
                    net.minecraft.item.ModelTransformationMode.NONE, client.world, client.player, 0);
            ItemRenderState.LayerRenderState[] layers = ((ItemRenderStateAccessor) (Object) temp).neural$getLayers();
            if (layers == null || layers.length == 0) {
                return null;
            }
            BakedModel model = ((LayerRenderStateAccessor) (Object) layers[0]).neural$getModel();
            if (model == null || model == bmm.getModel(new ModelIdentifier(Identifier.ofVanilla("builtin/missing"), "inventory"))) {
                return null;
            }
            return model;
        } catch (Throwable t) {
            return null;
        }
    }

    private static int currentIndex() {
        String name = INSTANCE.model.getCurrent();
        for (int i = 0; i < MODELS.length; i++) {
            if (MODELS[i].equals(name)) {
                return i;
            }
        }
        return 0;
    }
}
