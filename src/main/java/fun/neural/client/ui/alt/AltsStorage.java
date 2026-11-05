package fun.neural.client.ui.alt;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import fun.neural.Neural;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.List;

public class AltsStorage {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final List<String> alts = new ArrayList<>();
    private static File file;

    private AltsStorage() {
    }

    public static void load() {
        try {
            file = new File(Neural.INSTANCE.globalsDir, "alts.json");
            alts.clear();
            if (file.isFile()) {
                try (FileReader reader = new FileReader(file)) {
                    List<String> data = GSON.fromJson(reader, new TypeToken<List<String>>() {
                    }.getType());
                    if (data != null) {
                        alts.addAll(data);
                    }
                }
            }
        } catch (Exception e) {
            alts.clear();
        }
    }

    public static void save() {
        try {
            if (file == null) {
                file = new File(Neural.INSTANCE.globalsDir, "alts.json");
            }
            try (FileWriter writer = new FileWriter(file)) {
                GSON.toJson(alts, writer);
            }
        } catch (Exception ignored) {
        }
    }

    public static List<String> getAlts() {
        return alts;
    }

    public static void add(String name) {
        if (!alts.contains(name)) {
            alts.add(name);
            save();
        }
    }

    public static void remove(String name) {
        alts.remove(name);
        save();
    }
}
