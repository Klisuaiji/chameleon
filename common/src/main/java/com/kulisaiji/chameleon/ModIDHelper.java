package com.kulisaiji.chameleon;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.moandjiezana.toml.Toml;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.Path;
import java.util.jar.JarFile;
import java.util.zip.ZipEntry;

public class ModIDHelper {
    public static String getModId(Path jarPath) {
        String fileName = jarPath.getFileName().toString().toLowerCase();
        if (fileName.endsWith(".jar") || fileName.endsWith(".zip")) {
            return getModIdFromArchive(jarPath);
        }
        return null;
    }

    private static String getModIdFromArchive(Path archivePath) {
        try (JarFile jar = new JarFile(archivePath.toFile())) {
            // NeoForge 1.21.1 新标准优先，旧版兼容回退
            ZipEntry neoforgeModsToml = jar.getEntry("META-INF/neoforge.mods.toml");
            if (neoforgeModsToml != null) {
                String id = extractModIdFromToml(jar.getInputStream(neoforgeModsToml));
                if (id != null) return id;
            }

            // 旧版 Forge/NeoForge 兼容
            ZipEntry modsToml = jar.getEntry("META-INF/mods.toml");
            if (modsToml != null) {
                String id = extractModIdFromToml(jar.getInputStream(modsToml));
                if (id != null) return id;
            }

            // Fabric 格式
            ZipEntry fabricJson = jar.getEntry("fabric.mod.json");
            if (fabricJson != null) {
                JsonObject json = JsonParser.parseReader(
                    new InputStreamReader(jar.getInputStream(fabricJson))
                ).getAsJsonObject();
                if (json.has("id")) {
                    return json.get("id").getAsString();
                }
            }

            // Quilt 格式：id 位于 quilt_loader.id，而非顶层 id
            ZipEntry quiltJson = jar.getEntry("quilt.mod.json");
            if (quiltJson != null) {
                JsonObject json = JsonParser.parseReader(
                    new InputStreamReader(jar.getInputStream(quiltJson))
                ).getAsJsonObject();
                if (json.has("quilt_loader")) {
                    JsonObject loader = json.getAsJsonObject("quilt_loader");
                    if (loader.has("id")) {
                        return loader.get("id").getAsString();
                    }
                }
            }
        } catch (IOException e) {
            // 解析失败，回退到文件名匹配
        }
        return null;
    }

    private static String extractModIdFromToml(InputStream inputStream) {
        try (InputStreamReader reader = new InputStreamReader(inputStream)) {
            Toml toml = new Toml().read(reader);
            var mods = toml.getList("mods");
            if (mods != null && !mods.isEmpty()) {
                return ((Toml) mods.get(0)).getString("modId");
            }
        } catch (Exception e) {
            // toml 解析失败
        }
        return null;
    }
}
