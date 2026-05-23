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
    
    /**
     * 获取模组完整信息
     */
    public static ModInfo getModInfo(Path jarPath) {
        String fileName = jarPath.getFileName().toString();
        
        try (JarFile jar = new JarFile(jarPath.toFile())) {
            // NeoForge 新标准优先
            ZipEntry neoforgeModsToml = jar.getEntry("META-INF/neoforge.mods.toml");
            if (neoforgeModsToml != null) {
                return extractNeoForgeInfo(jar, neoforgeModsToml, fileName);
            }

            // 旧版 Forge/NeoForge 兼容
            ZipEntry modsToml = jar.getEntry("META-INF/mods.toml");
            if (modsToml != null) {
                return extractNeoForgeInfo(jar, modsToml, fileName);
            }

            // Fabric 格式
            ZipEntry fabricJson = jar.getEntry("fabric.mod.json");
            if (fabricJson != null) {
                return extractFabricInfo(jar, fabricJson, fileName);
            }

            // Quilt 格式
            ZipEntry quiltJson = jar.getEntry("quilt.mod.json");
            if (quiltJson != null) {
                return extractQuiltInfo(jar, quiltJson, fileName);
            }
        } catch (IOException e) {
            System.err.println("[Chameleon] 无法读取模组文件: " + fileName + " - " + e.getMessage());
        }
        
        // 无法解析时返回基本信息
        return new ModInfo(fileName, null);
    }
    
    /**
     * 获取模组ID（兼容旧代码）
     */
    public static String getModId(Path jarPath) {
        ModInfo info = getModInfo(jarPath);
        return info.getModId();
    }
    
    private static ModInfo extractNeoForgeInfo(JarFile jar, ZipEntry entry, String fileName) throws IOException {
        try (InputStreamReader reader = new InputStreamReader(jar.getInputStream(entry))) {
            Toml toml = new Toml().read(reader);
            var mods = toml.getList("mods");
            if (mods != null && !mods.isEmpty()) {
                Toml mod = (Toml) mods.get(0);
                String modId = mod.getString("modId");
                String modName = mod.getString("displayName");
                String version = mod.getString("version");
                String description = mod.getString("description");
                String logoFile = toml.getString("modloader.logoFile");
                
                return new ModInfo(fileName, modId, modName, version, 
                    logoFile, logoFile, description);
            }
        }
        return new ModInfo(fileName, null);
    }
    
    private static ModInfo extractFabricInfo(JarFile jar, ZipEntry entry, String fileName) throws IOException {
        JsonObject json = JsonParser.parseReader(
            new InputStreamReader(jar.getInputStream(entry))
        ).getAsJsonObject();
        
        String modId = json.has("id") ? json.get("id").getAsString() : null;
        String modName = json.has("name") ? json.get("name").getAsString() : null;
        String version = json.has("version") ? json.get("version").getAsString() : null;
        String description = json.has("description") ? json.get("description").getAsString() : null;
        
        // Fabric 图标通常在 resources 目录
        String iconPath = null;
        if (json.has("icon")) {
            iconPath = json.get("icon").getAsString();
        }
        
        return new ModInfo(fileName, modId, modName, version, iconPath, iconPath, description);
    }
    
    private static ModInfo extractQuiltInfo(JarFile jar, ZipEntry entry, String fileName) throws IOException {
        JsonObject json = JsonParser.parseReader(
            new InputStreamReader(jar.getInputStream(entry))
        ).getAsJsonObject();
        
        String modId = null;
        String modName = null;
        String version = null;
        String description = null;
        String iconPath = null;
        
        if (json.has("quilt_loader")) {
            JsonObject loader = json.getAsJsonObject("quilt_loader");
            if (loader.has("id")) {
                modId = loader.get("id").getAsString();
            }
            if (loader.has("version")) {
                version = loader.get("version").getAsString();
            }
            
            // Quilt 的元数据在 metadata 中
            if (loader.has("metadata")) {
                JsonObject metadata = loader.getAsJsonObject("metadata");
                if (metadata.has("name")) {
                    modName = metadata.get("name").getAsString();
                }
                if (metadata.has("description")) {
                    description = metadata.get("description").getAsString();
                }
                if (metadata.has("icon")) {
                    iconPath = metadata.get("icon").getAsString();
                }
            }
        }
        
        return new ModInfo(fileName, modId, modName, version, iconPath, iconPath, description);
    }
}
