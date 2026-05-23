package com.kulisaiji.chameleon;

/**
 * 模组信息类，包含模组元数据和图片信息
 */
public class ModInfo {
    private final String fileName;
    private final String modId;
    private final String modName;
    private final String version;
    private final String iconPath;      // 模组图标路径（在JAR内）
    private final String logoPath;      // 模组Logo路径
    private final String description;
    
    public ModInfo(String fileName, String modId, String modName, String version, 
                   String iconPath, String logoPath, String description) {
        this.fileName = fileName;
        this.modId = modId;
        this.modName = modName != null ? modName : modId;
        this.version = version;
        this.iconPath = iconPath;
        this.logoPath = logoPath;
        this.description = description;
    }
    
    // 简化的构造函数
    public ModInfo(String fileName, String modId) {
        this(fileName, modId, modId, null, null, null, null);
    }
    
    public String getFileName() {
        return fileName;
    }
    
    public String getModId() {
        return modId;
    }
    
    public String getModName() {
        return modName;
    }
    
    public String getVersion() {
        return version;
    }
    
    public String getIconPath() {
        return iconPath;
    }
    
    public String getLogoPath() {
        return logoPath;
    }
    
    public String getDescription() {
        return description;
    }
    
    @Override
    public String toString() {
        return "ModInfo{" +
                "fileName='" + fileName + '\'' +
                ", modId='" + modId + '\'' +
                ", modName='" + modName + '\'' +
                ", iconPath='" + iconPath + '\'' +
                '}';
    }
}
