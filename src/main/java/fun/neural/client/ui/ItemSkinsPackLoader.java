package fun.neural.client.ui;

import java.io.*;
import java.nio.file.*;

public class ItemSkinsPackLoader {
    private static boolean applied = false;
    private static final String PACK_ID = "file/ItemSkins";

    public static void tryEnable() {
        if (applied) return;
        applied = true;

        try {
            File runDir = new File(".");
            File optionsFile = new File(runDir, "options.txt");
            if (!optionsFile.exists()) return;

            String content = new String(Files.readAllBytes(optionsFile.toPath()));

            if (content.contains(PACK_ID)) return;

            String marker = "resourcePacks:";
            int idx = content.indexOf(marker);
            if (idx == -1) return;

            int end = content.indexOf('\n', idx);
            if (end == -1) end = content.length();

            String currentLine = content.substring(idx, end).trim();
            String packsValue = currentLine.substring(marker.length());

            if (packsValue.contains(PACK_ID)) return;

            if (packsValue.startsWith("[")) {
                if (packsValue.equals("[]")) {
                    packsValue = "[" + PACK_ID + "]";
                } else {
                    packsValue = "[" + PACK_ID + "," + packsValue.substring(1);
                }
            } else {
                packsValue = "[" + PACK_ID + "]";
            }

            String newLine = marker + packsValue;
            content = content.substring(0, idx) + newLine + content.substring(end);

            Files.write(optionsFile.toPath(), content.getBytes());
        } catch (Exception ignored) {
        }
    }
}
