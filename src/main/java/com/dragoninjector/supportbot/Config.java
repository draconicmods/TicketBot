package com.dragoninjector.supportbot;

import com.google.gson.*;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

public class Config {

    private final SupportBot main;
    private final Path configDirectory;
    private JsonElement config;
    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();

    public Config(SupportBot main, Path configDirectory) {
        this.main = main;
        this.configDirectory = configDirectory;
    }

    JsonObject newConfig(String configName, JsonObject configOptions) {
        JsonObject conf = null;
        Path configFile = configDirectory.resolve(configName + ".json");
        try {
            if (!Files.exists(configFile)) {
                Files.createFile(configFile);
                write(configFile, configOptions);
            }
            if (hasAllEntries(read(configFile).getAsJsonObject(), configOptions)) {
                conf = read(configFile).getAsJsonObject();
            } else {
                write(configFile, writeNotFoundDefaults(read(configFile).getAsJsonObject(), configOptions));
                conf = read(configFile).getAsJsonObject();
            }
        } catch (IOException ex) {
            ex.printStackTrace();
        }
        this.config = conf;
        return conf;
    }

    private JsonElement read(Path config) {
        JsonElement element = null;
        try (BufferedReader reader = Files.newBufferedReader(config)) {
            JsonParser parser = new JsonParser();
            element = parser.parse(reader);
        } catch (IOException ex) {
            ex.printStackTrace();
        }
        return element;
    }

    private void write(Path config, JsonObject object) {
        try (BufferedWriter writer = Files.newBufferedWriter(config)) {
            writer.write(gson.toJson(object));
            writer.flush();
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    public void saveConfig(String configName, JsonElement conf) {
        try (BufferedWriter writer = Files.newBufferedWriter(configDirectory.resolve(configName + ".json"))) {
            writer.write(gson.toJson(conf));
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public static JsonObject writeDefaults() {
        JsonObject jo = new JsonObject();
        jo.addProperty("token", "add_me");
        jo.addProperty("commandPrefix", "/");
        jo.addProperty("supportCategoryId", "add_me");
        jo.addProperty("guildID", "add_me");
        jo.addProperty("lockedLogChannelId", "add_me");
        jo.addProperty("logChannelId", "add_me");
        jo.addProperty("TicketCreationChannelID", "add_me");
        jo.addProperty("debug", false);
        JsonObject colour = new JsonObject();
        colour.addProperty("Red", 248);
        colour.addProperty("Green", 195);
        colour.addProperty("Blue", 0);
        jo.add("colour", colour);
        return jo;
    }

    public JsonObject writeNotFoundDefaults(JsonObject config, JsonObject configOptions) {
        JsonObject finished = new JsonObject();
        for (Map.Entry<String, JsonElement> entrySet : configOptions.entrySet()) {
            if (!config.has(entrySet.getKey())) {
                finished.add(entrySet.getKey(), entrySet.getValue());
            } else {
                finished.add(entrySet.getKey(), config.get(entrySet.getKey()));
            }
        }
        return finished;
    }

    public boolean hasAllEntries(JsonObject config, JsonObject configOptions) {
        int count = 0;
        for (Map.Entry<String, JsonElement> entrySet : configOptions.entrySet()) {
            if (config.has(entrySet.getKey())) {
                count++;
            }
        }
        return (count == configOptions.size());
    }

    public <T extends JsonElement> T getConfigValue(String... keys) {
        JsonObject parent = (JsonObject) config;
        JsonElement temp = parent.get(keys[0]);
        if (temp.isJsonArray()) {
            return (T) temp.getAsJsonArray();
        }
        JsonElement object = temp;
        try {
            for (int i = 1; i < keys.length; i++) {
                temp = ((JsonObject) object).get(keys[i]);
                if (temp.isJsonArray()) {
                    return (T) temp.getAsJsonArray();
                }
                if (temp.isJsonPrimitive()) {
                    return (T) temp.getAsJsonPrimitive();
                }
                object = temp.getAsJsonObject();
            }
        } catch (NullPointerException ex) {
            return (T) object;
        }
        return (T) object;
    }
}
