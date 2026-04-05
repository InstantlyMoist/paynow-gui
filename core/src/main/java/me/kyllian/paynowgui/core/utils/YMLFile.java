package me.kyllian.paynowgui.core.utils;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import lombok.Getter;
import org.yaml.snakeyaml.Yaml;

/**
 * Platform-agnostic YAML file handler using SnakeYAML directly.
 */
public class YMLFile {

    @Getter
    private final File file;
    private Map<String, Object> data;
    private final Yaml yaml = new Yaml();

    public YMLFile(File dataFolder, String fileName) {
        if (!fileName.endsWith(".yml")) {
            throw new IllegalArgumentException("File name must end with .yml");
        }

        file = new File(dataFolder, fileName);
        if (!file.exists()) {
            file.getParentFile().mkdirs();
            try {
                // Try to copy resource
                InputStream resource = getClass().getClassLoader().getResourceAsStream(fileName);
                if (resource != null) {
                    Files.copy(resource, file.toPath());
                    resource.close();
                } else {
                    file.createNewFile();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        reload();
    }

    @SuppressWarnings("unchecked")
    public void reload() {
        try (InputStream is = Files.newInputStream(file.toPath())) {
            data = yaml.load(is);
            if (data == null) data = new java.util.LinkedHashMap<>();
        } catch (IOException e) {
            e.printStackTrace();
            data = new java.util.LinkedHashMap<>();
        }
    }

    public void save() {
        try (var writer = Files.newBufferedWriter(file.toPath())) {
            yaml.dump(data, writer);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public String getString(String path) {
        return getString(path, null);
    }

    public String getString(String path, String defaultValue) {
        Object value = get(path);
        return value != null ? value.toString() : defaultValue;
    }

    public List<String> getStringList(String path) {
        Object value = get(path);
        if (value instanceof List<?>) {
            List<String> result = new ArrayList<>();
            for (Object item : (List<?>) value) {
                result.add(item.toString());
            }
            return result;
        }
        return List.of();
    }

    public int getInt(String path) {
        return getInt(path, 0);
    }

    public int getInt(String path, int defaultValue) {
        Object value = get(path);
        if (value instanceof Number) return ((Number) value).intValue();
        return defaultValue;
    }

    public boolean getBoolean(String path) {
        return getBoolean(path, false);
    }

    public boolean getBoolean(String path, boolean defaultValue) {
        Object value = get(path);
        if (value instanceof Boolean) return (Boolean) value;
        return defaultValue;
    }

    public Object get(String path) {
        String[] parts = path.split("\\.");
        Object current = data;
        for (String part : parts) {
            if (current instanceof Map) {
                @SuppressWarnings("unchecked")
                Map<String, Object> map = (Map<String, Object>) current;
                current = map.get(part);
            } else {
                return null;
            }
        }
        return current;
    }

    @SuppressWarnings("unchecked")
    public void set(String path, Object value) {
        String[] parts = path.split("\\.");
        Map<String, Object> current = data;
        for (int i = 0; i < parts.length - 1; i++) {
            Object next = current.get(parts[i]);
            if (!(next instanceof Map)) {
                next = new java.util.LinkedHashMap<String, Object>();
                current.put(parts[i], next);
            }
            current = (Map<String, Object>) next;
        }
        current.put(parts[parts.length - 1], value);
    }

    public boolean has(String path) {
        return get(path) != null;
    }

    @SuppressWarnings("unchecked")
    public Set<String> getKeys(String path) {
        Object value = get(path);
        if (value instanceof Map) {
            return ((Map<String, Object>) value).keySet();
        }
        return Set.of();
    }
}
