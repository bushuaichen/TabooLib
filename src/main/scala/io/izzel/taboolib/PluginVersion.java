package io.izzel.taboolib;

import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.Objects;

/**
 * TabooLibLoader
 * io.izzel.taboolib.loader.Version
 *
 * @author sky
 * @since 2021/5/20 1:49 下午
 */
public class PluginVersion implements Comparable<PluginVersion> {

    private final String source;
    private final int[] version;

    public PluginVersion(String source) {
        this.source = source;
        String[] type = source.split("[- ]");
        String[] args = type[0].split("\\.");
        if (args.length == 2) {
            version = new int[]{-1, Integer.parseInt(args[0]), Integer.parseInt(args[1])};
        } else if (args.length == 3) {
            version = new int[]{Integer.parseInt(args[0]), Integer.parseInt(args[1]), Integer.parseInt(args[2])};
        } else {
            throw new VersionFormatException(source);
        }
    }

    public boolean isBefore(PluginVersion version) {
        return compareTo(version) < 0;
    }

    public boolean isAfter(PluginVersion version) {
        return compareTo(version) > 0;
    }

    public String getSource() {
        return source;
    }

    public boolean isLegacy() {
        return version[0] == -1;
    }

    public int[] getVersion() {
        return version;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof PluginVersion)) return false;
        PluginVersion version1 = (PluginVersion) o;
        return getSource().equals(version1.getSource()) && Arrays.equals(getVersion(), version1.getVersion());
    }

    @Override
    public int hashCode() {
        int result = Objects.hash(getSource());
        result = 31 * result + Arrays.hashCode(getVersion());
        return result;
    }

    @Override
    public String toString() {
        return "PluginVersion{" +
                "source='" + source + '\'' +
                ", version=" + Arrays.toString(version) +
                '}';
    }

    @Override
    public int compareTo(@NotNull PluginVersion o) {
        if (version[0] > o.version[0]) {
            return 1;
        } else if (version[0] == o.version[0]) {
            if (version[1] > o.version[1]) {
                return 1;
            } else if (version[1] == o.version[1]) {
                return Integer.compare(version[2], o.version[2]);
            } else {
                return -1;
            }
        } else {
            return -1;
        }
    }

    public static class VersionFormatException extends IllegalArgumentException {

        public VersionFormatException(String source) {
            super("For input version: \"" + source + "\"");
        }
    }
}
