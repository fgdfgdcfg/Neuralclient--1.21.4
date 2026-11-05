package fun.neural.client.ui;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.util.Identifier;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.metadata.IIOMetadata;
import org.w3c.dom.NamedNodeMap;
import javax.imageio.stream.ImageInputStream;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public final class MenuBackground {
    private static final int MAX_FRAMES = 240;
    private static final int MAX_WIDTH = 640;
    private static final long MAX_BYTES = 24L * 1024L * 1024L;
    private static final String STATE_FILE = "selected.txt";

    private static boolean scanned;
    private static final List<Entry> entries = new ArrayList<>();
    private static int selectedIndex = -1;

    private static final List<Identifier> frames = new ArrayList<>();
    private static final List<Integer> delays = new ArrayList<>();
    private static int frameWidth;
    private static int frameHeight;
    private static int totalDuration;
    private static int counter;

    private static final class Entry {
        final File file;
        final String resource;
        final String name;
        final boolean animated;

        Entry(File file, String name, boolean animated) {
            this.file = file;
            this.resource = null;
            this.name = name;
            this.animated = animated;
        }

        Entry(String resource, String name) {
            this.file = null;
            this.resource = resource;
            this.name = name;
            this.animated = false;
        }
    }

    private MenuBackground() {
    }

    public static void loadIfNeeded() {
        if (!scanned) {
            scanned = true;
            refresh();
            restoreSelection();
        }
    }

    public static void refresh() {
        entries.clear();
        try {
            Path runDir = MinecraftClient.getInstance().runDirectory.toPath();
            File folder = runDir.resolve("menu-background").toFile();
            if (folder.isDirectory()) {
                File[] files = folder.listFiles((dir, name) -> {
                    String n = name.toLowerCase();
                    return n.endsWith(".png") || n.endsWith(".jpg") || n.endsWith(".jpeg") || n.endsWith(".gif");
                });
                if (files != null) {
                    Arrays.sort(files, Comparator.comparing(File::getName));
                    for (File file : files) {
                        if (file.isFile() && file.length() <= MAX_BYTES) {
                            entries.add(new Entry(file, stripExt(file.getName()), file.getName().toLowerCase().endsWith(".gif")));
                        }
                    }
                }
            }
            File gif = new File(runDir.toFile(), "menu-background.gif");
            if (gif.isFile()) {
                entries.add(new Entry(gif, "menu-background.gif", true));
            }
            File image = findFirst(runDir.toFile(), "menu-background.png", "menu-background.jpg", "menu-background.jpeg");
            if (image != null) {
                entries.add(new Entry(image, stripExt(image.getName()), false));
            }
            if (entries.isEmpty()) {
                // встроенный дефолт, чтобы фон был и на чужих лаунчерах без папки menu-background
                entries.add(new Entry("/assets/neural/menu-background/default.png", "default"));
            }
        } catch (Throwable t) {
            System.err.println("[Neural] Failed to scan menu backgrounds:");
            t.printStackTrace();
        }
    }

    public static int getCount() {
        return entries.size();
    }

    public static String getName(int index) {
        return index >= 0 && index < entries.size() ? entries.get(index).name : "";
    }

    public static int getSelectedIndex() {
        return selectedIndex;
    }

    public static void select(int index) {
        if (entries.isEmpty()) {
            releaseFrames();
            selectedIndex = -1;
            saveSelection();
            return;
        }
        index = Math.floorMod(index, entries.size());
        if (index == selectedIndex && !frames.isEmpty()) {
            return;
        }
        releaseFrames();
        selectedIndex = index;
        try {
            Entry entry = entries.get(index);
            if (entry.animated) {
                loadGif(entry.file);
            } else if (entry.file != null) {
                BufferedImage img = ImageIO.read(entry.file);
                if (img != null) {
                    addFrame(scale(img), 0);
                }
            } else if (entry.resource != null) {
                try (java.io.InputStream in = MenuBackground.class.getResourceAsStream(entry.resource)) {
                    if (in != null) {
                        BufferedImage img = ImageIO.read(in);
                        if (img != null) {
                            addFrame(scale(img), 0);
                        }
                    }
                }
            }
        } catch (Throwable t) {
            System.err.println("[Neural] Failed to load background '" + entries.get(index).name + "':");
            t.printStackTrace();
        }
        if (frames.isEmpty()) {
            selectedIndex = -1;
        }
        saveSelection();
    }

    public static void cycle(int direction) {
        loadIfNeeded();
        refresh();
        if (entries.isEmpty()) {
            return;
        }
        int base = selectedIndex >= 0 ? selectedIndex : -direction;
        select(Math.floorMod(base + direction, entries.size()));
    }

    public static boolean isAvailable() {
        return !frames.isEmpty();
    }

    public static String getStatusText() {
        if (entries.isEmpty()) {
            return "";
        }
        String name = selectedIndex >= 0 ? entries.get(selectedIndex).name : "нет";
        return (selectedIndex + 1) + "/" + entries.size() + " \u00b7 " + name;
    }

    public static void draw(DrawContext context) {
        if (frames.isEmpty()) {
            return;
        }
        Identifier id = frames.get(currentIndex());
        context.drawTexture(RenderLayer::getGuiTextured, id,
                0, 0,
                0.0F, 0.0F,
                context.getScaledWindowWidth(), context.getScaledWindowHeight(),
                frameWidth, frameHeight,
                frameWidth, frameHeight);
    }

    private static void releaseFrames() {
        if (frames.isEmpty()) {
            return;
        }
        for (Identifier id : frames) {
            try {
                MinecraftClient.getInstance().getTextureManager().destroyTexture(id);
            } catch (Throwable ignored) {
            }
        }
        frames.clear();
        delays.clear();
        totalDuration = 0;
    }

    private static void restoreSelection() {
        if (entries.isEmpty()) {
            return;
        }
        File state = stateFile();
        if (state.isFile()) {
            try {
                String saved = new String(Files.readAllBytes(state.toPath()), StandardCharsets.UTF_8).trim();
                for (int i = 0; i < entries.size(); i++) {
                    Entry entry = entries.get(i);
                    String entryName = entry.file != null ? entry.file.getName() : entry.name;
                    if (entryName.equalsIgnoreCase(saved)) {
                        select(i);
                        return;
                    }
                }
            } catch (IOException ignored) {
            }
        }
        select(0);
    }

    private static void saveSelection() {
        File state = stateFile();
        try {
            File parent = state.getParentFile();
            if (parent != null && !parent.exists()) {
                parent.mkdirs();
            }
            String value = selectedIndex >= 0 ? entries.get(selectedIndex).name : "";
            Files.write(state.toPath(), value.getBytes(StandardCharsets.UTF_8));
        } catch (IOException ignored) {
        }
    }

    private static File stateFile() {
        Path runDir = MinecraftClient.getInstance().runDirectory.toPath();
        return runDir.resolve("menu-background").resolve(STATE_FILE).toFile();
    }

    private static int currentIndex() {
        if (frames.size() == 1 || totalDuration <= 0) {
            return 0;
        }
        long t = System.currentTimeMillis() % totalDuration;
        for (int i = 0; i < frames.size(); i++) {
            t -= delays.get(i);
            if (t < 0) {
                return i;
            }
        }
        return frames.size() - 1;
    }

    private static void loadGif(File file) throws Exception {
        ImageReader reader = ImageIO.getImageReadersByFormatName("gif").next();
        try (ImageInputStream in = ImageIO.createImageInputStream(file)) {
            reader.setInput(in);
            int count = Math.min(reader.getNumImages(true), MAX_FRAMES);
            for (int i = 0; i < count; i++) {
                BufferedImage img = reader.read(i);
                if (img != null) {
                    addFrame(scale(img), readDelay(reader, i));
                }
            }
        } finally {
            reader.dispose();
        }
    }

    private static int readDelay(ImageReader reader, int index) {
        try {
            IIOMetadata meta = reader.getImageMetadata(index);
            Node root = meta.getAsTree(meta.getNativeMetadataFormatName());
            NodeList children = root.getChildNodes();
            for (int i = 0; i < children.getLength(); i++) {
                Node node = children.item(i);
                if ("GraphicControlExtension".equals(node.getNodeName())) {
                    NamedNodeMap attrs = node.getAttributes();
                    Node delay = attrs.getNamedItem("delayTime");
                    if (delay != null) {
                        return Math.max(20, Integer.parseInt(delay.getNodeValue()) * 10);
                    }
                }
            }
        } catch (Exception ignored) {
        }
        return 50;
    }

    private static void addFrame(BufferedImage img, int delayMs) {
        NativeImage image = new NativeImage(NativeImage.Format.RGBA, img.getWidth(), img.getHeight(), false);
        for (int y = 0; y < img.getHeight(); y++) {
            for (int x = 0; x < img.getWidth(); x++) {
                image.setColorArgb(x, y, img.getRGB(x, y));
            }
        }
        Identifier id = Identifier.of("neural", "menu_bg_" + counter++);
        MinecraftClient.getInstance().getTextureManager().registerTexture(id, new NativeImageBackedTexture(image));
        if (frames.isEmpty()) {
            frameWidth = img.getWidth();
            frameHeight = img.getHeight();
        }
        frames.add(id);
        delays.add(delayMs > 0 ? delayMs : 1000);
        totalDuration += delayMs > 0 ? delayMs : 1000;
    }

    private static BufferedImage scale(BufferedImage src) {
        if (src.getWidth() <= MAX_WIDTH) {
            return argb(src);
        }
        int nh = Math.max(1, Math.round(src.getHeight() * MAX_WIDTH / (float) src.getWidth()));
        BufferedImage dst = new BufferedImage(MAX_WIDTH, nh, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = dst.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g.drawImage(src, 0, 0, MAX_WIDTH, nh, null);
        g.dispose();
        return dst;
    }

    private static BufferedImage argb(BufferedImage src) {
        if (src.getType() == BufferedImage.TYPE_INT_ARGB) {
            return src;
        }
        BufferedImage dst = new BufferedImage(src.getWidth(), src.getHeight(), BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = dst.createGraphics();
        g.drawImage(src, 0, 0, null);
        g.dispose();
        return dst;
    }

    private static File findFirst(File dir, String... names) {
        for (String name : names) {
            File file = new File(dir, name);
            if (file.isFile()) {
                return file;
            }
        }
        return null;
    }

    private static String stripExt(String name) {
        int dot = name.lastIndexOf('.');
        return dot > 0 ? name.substring(0, dot) : name;
    }
}
