package com.kulisaiji.chameleon;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

public class ConfigLoader {
    private static final Path CONFIG_PATH = Paths.get("config", "chameleon_config.json");
    private JsonObject config;

    public void loadOrCreate() {
        if (!Files.exists(CONFIG_PATH)) {
            createDefault();
        }
        try (Reader reader = Files.newBufferedReader(CONFIG_PATH)) {
            config = JsonParser.parseReader(reader).getAsJsonObject();
        } catch (IOException e) {
            config = new JsonObject();
        }
    }

    private void createDefault() {
        try {
            Files.createDirectories(CONFIG_PATH.getParent());
            JsonObject def = new JsonObject();
            JsonObject equipment = new JsonObject();
            equipment.add("android", new JsonArray());
            equipment.add("windows", new JsonArray());
            equipment.add("linux", new JsonArray());
            equipment.add("mac", new JsonArray());
            JsonObject environment = new JsonObject();
            environment.add("client", new JsonArray());
            environment.add("server", new JsonArray());
            def.add("equipment", equipment);
            def.add("environment", environment);
            Files.write(CONFIG_PATH, new Gson().toJson(def).getBytes());
            config = def;
        } catch (IOException e) {
            config = new JsonObject();
        }
    }

    public List<String> getDisablePatterns(String device, String runtime) {
        List<String> patterns = new ArrayList<>();
        if (config == null) return patterns;

        JsonObject equipment = config.getAsJsonObject("equipment");
        if (equipment != null && equipment.has(device)) {
            for (JsonElement e : equipment.getAsJsonArray(device)) {
                patterns.add(e.getAsString());
            }
        }

        JsonObject environment = config.getAsJsonObject("environment");
        if (environment != null && environment.has(runtime)) {
            for (JsonElement e : environment.getAsJsonArray(runtime)) {
                patterns.add(e.getAsString());
            }
        }
        return patterns;
    }
}
