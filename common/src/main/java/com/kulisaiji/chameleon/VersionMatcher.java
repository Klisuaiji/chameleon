package com.kulisaiji.chameleon;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 语义化版本比较器
 * 支持 SemVer 2.0.0 规范
 */
public class VersionMatcher {
    
    private static final Pattern VERSION_PATTERN = Pattern.compile(
        "^v?(\\d+)(?:\\.(\\d+))?(?:\\.(\\d+))?(?:[-+](.*))?$"
    );
    
    /**
     * 版本约束类型
     */
    public enum ConstraintType {
        NONE,       // 无约束
        LESS_THAN,  // <
        LESS_EQUAL, // <=
        GREATER_THAN, // >
        GREATER_EQUAL, // >=
        EQUAL       // =
    }
    
    /**
     * 版本约束
     */
    public static class VersionConstraint {
        private final String modId;
        private final ConstraintType type;
        private final SemanticVersion version;
        private final String rawString;
        
        public VersionConstraint(String modId, ConstraintType type, SemanticVersion version, String rawString) {
            this.modId = modId;
            this.type = type;
            this.version = version;
            this.rawString = rawString;
        }
        
        public String getModId() {
            return modId;
        }
        
        public ConstraintType getType() {
            return type;
        }
        
        public SemanticVersion getVersion() {
            return version;
        }
        
        public String getRawString() {
            return rawString;
        }
        
        /**
         * 检查版本是否匹配约束
         */
        public boolean matches(SemanticVersion targetVersion) {
            if (type == ConstraintType.NONE || version == null) {
                return true;
            }
            if (targetVersion == null) {
                return false;
            }
            
            int cmp = targetVersion.compareTo(version);
            switch (type) {
                case LESS_THAN:
                    return cmp < 0;
                case LESS_EQUAL:
                    return cmp <= 0;
                case GREATER_THAN:
                    return cmp > 0;
                case GREATER_EQUAL:
                    return cmp >= 0;
                case EQUAL:
                    return cmp == 0;
                default:
                    return false;
            }
        }
        
        @Override
        public String toString() {
            return rawString;
        }
    }
    
    /**
     * 语义化版本
     */
    public static class SemanticVersion implements Comparable<SemanticVersion> {
        private final int major;
        private final int minor;
        private final int patch;
        private final String prerelease;
        private final String build;
        
        public SemanticVersion(int major, int minor, int patch, String prerelease, String build) {
            this.major = major;
            this.minor = minor;
            this.patch = patch;
            this.prerelease = prerelease;
            this.build = build;
        }
        
        @Override
        public int compareTo(SemanticVersion other) {
            // 比较主版本
            if (this.major != other.major) {
                return Integer.compare(this.major, other.major);
            }
            
            // 比较次版本
            if (this.minor != other.minor) {
                return Integer.compare(this.minor, other.minor);
            }
            
            // 比较补丁版本
            if (this.patch != other.patch) {
                return Integer.compare(this.patch, other.patch);
            }
            
            // 预发布版本比较（预发布 < 正式版）
            if (this.prerelease == null && other.prerelease != null) {
                return 1; // 正式版 > 预发布版
            }
            if (this.prerelease != null && other.prerelease == null) {
                return -1; // 预发布版 < 正式版
            }
            if (this.prerelease != null && other.prerelease != null) {
                int preCmp = this.prerelease.compareTo(other.prerelease);
                if (preCmp != 0) {
                    return preCmp;
                }
            }
            
            // 构建元数据不参与版本比较（SemVer 规范）
            return 0;
        }
        
        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append(major).append(".").append(minor).append(".").append(patch);
            if (prerelease != null) {
                sb.append("-").append(prerelease);
            }
            if (build != null) {
                sb.append("+").append(build);
            }
            return sb.toString();
        }
    }
    
    /**
     * 解析版本约束字符串
     * 例如："sodium (<0.6.0)" => modId="sodium", constraint="<0.6.0"
     */
    public static VersionConstraint parseConstraint(String rule) {
        if (rule == null || rule.trim().isEmpty()) {
            return null;
        }
        
        rule = rule.trim();
        
        // 检查是否有版本约束 (括号)
        int parenStart = rule.indexOf('(');
        int parenEnd = rule.lastIndexOf(')');
        
        String modId;
        String constraintStr;
        
        if (parenStart != -1 && parenEnd != -1 && parenEnd > parenStart) {
            modId = rule.substring(0, parenStart).trim();
            constraintStr = rule.substring(parenStart + 1, parenEnd).trim();
        } else {
            modId = rule;
            constraintStr = null;
        }
        
        // 解析约束条件
        ConstraintType type = ConstraintType.NONE;
        SemanticVersion version = null;
        
        if (constraintStr != null && !constraintStr.isEmpty()) {
            // 尝试解析约束操作符
            if (constraintStr.startsWith("<=")) {
                type = ConstraintType.LESS_EQUAL;
                version = parseVersion(constraintStr.substring(2).trim());
            } else if (constraintStr.startsWith(">=")) {
                type = ConstraintType.GREATER_EQUAL;
                version = parseVersion(constraintStr.substring(2).trim());
            } else if (constraintStr.startsWith("<")) {
                type = ConstraintType.LESS_THAN;
                version = parseVersion(constraintStr.substring(1).trim());
            } else if (constraintStr.startsWith(">")) {
                type = ConstraintType.GREATER_THAN;
                version = parseVersion(constraintStr.substring(1).trim());
            } else if (constraintStr.startsWith("=")) {
                type = ConstraintType.EQUAL;
                version = parseVersion(constraintStr.substring(1).trim());
            } else {
                // 只有版本号，默认为等于
                type = ConstraintType.EQUAL;
                version = parseVersion(constraintStr);
            }
        }
        
        return new VersionConstraint(modId, type, version, rule);
    }
    
    /**
     * 解析版本号
     */
    public static SemanticVersion parseVersion(String versionStr) {
        if (versionStr == null || versionStr.trim().isEmpty()) {
            return null;
        }
        
        versionStr = versionStr.trim();
        Matcher matcher = VERSION_PATTERN.matcher(versionStr);
        
        if (!matcher.matches()) {
            // 尝试简单数字版本
            try {
                String[] parts = versionStr.split("\\.");
                int major = Integer.parseInt(parts[0]);
                int minor = parts.length > 1 ? Integer.parseInt(parts[1]) : 0;
                int patch = parts.length > 2 ? Integer.parseInt(parts[2]) : 0;
                return new SemanticVersion(major, minor, patch, null, null);
            } catch (NumberFormatException e) {
                return null;
            }
        }
        
        int major = Integer.parseInt(matcher.group(1));
        int minor = matcher.group(2) != null ? Integer.parseInt(matcher.group(2)) : 0;
        int patch = matcher.group(3) != null ? Integer.parseInt(matcher.group(3)) : 0;
        
        String suffix = matcher.group(4);
        String prerelease = null;
        String build = null;
        
        if (suffix != null) {
            if (suffix.contains("+")) {
                String[] parts = suffix.split("\\+", 2);
                prerelease = parts[0];
                build = parts[1];
            } else {
                prerelease = suffix;
            }
        }
        
        return new SemanticVersion(major, minor, patch, prerelease, build);
    }
    
    /**
     * 检查版本是否满足约束
     */
    public static boolean checkVersion(String modVersion, String constraint) {
        if (constraint == null || constraint.trim().isEmpty()) {
            return true;
        }
        
        VersionConstraint vc = parseConstraint(constraint);
        if (vc == null || vc.getType() == ConstraintType.NONE) {
            return true;
        }
        
        SemanticVersion targetVersion = parseVersion(modVersion);
        if (targetVersion == null) {
            Logger.debug("无法解析版本号：" + modVersion);
            return false;
        }
        
        return vc.matches(targetVersion);
    }
}
