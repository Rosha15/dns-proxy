import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

public class ConfigLoader {
    public static Config loadConfig(String filename) {
        try {
            Path path = Path.of(filename);
            if (!Files.isRegularFile(path)) {
                throw new IllegalArgumentException("Configuration file not found: " + filename);
            }

            Map<String, String> values = new HashMap<>();
            for (String raw : Files.readAllLines(path, StandardCharsets.UTF_8)) {
                String line = raw.trim();
                if (line.isEmpty() || line.startsWith("#")) {
                    continue;
                }
                int equal = line.indexOf('=');
                if (equal <= 0 || equal == line.length() - 1) {
                    throw new IllegalArgumentException("Expected key=value: " + line);
                }
                String key = line.substring(0, equal).trim();
                String value = line.substring(equal + 1).trim();
                if (key.isEmpty() || value.isEmpty()) {
                    throw new IllegalArgumentException("Empty key or value: " + line);
                }
                values.put(key, value);
            }

            Config config = new Config();
            config.upstreamIp = requiredIpv4(values, "upstream_ip");
            config.upstreamPort = requiredInt(values, "upstream_port", 1, 65535);
            config.sinkholeIp = requiredIpv4(values, "sinkhole_ip");
            config.proxyIp = requiredIpv4(values, "proxy_ip");
            config.adListFile = required(values, "ad_list_file");
            config.sanctionListFile = required(values, "sanction_list_file");

            config.upstreamTimeoutSec = optionalInt(values, "upstream_timeout_sec", 2, 1,
                    Integer.MAX_VALUE / 1000);
            config.localTtl = optionalInt(values, "local_ttl", 60, 0, Integer.MAX_VALUE);
            config.cacheMaxEntries = optionalInt(values, "cache_max_entries", 100, 1, Integer.MAX_VALUE);
            String cache = values.getOrDefault("cache_enabled", "true");
            if (!cache.equals("true") && !cache.equals("false")) {
                throw new IllegalArgumentException("cache_enabled must be true or false");
            }
            config.cacheEnabled = Boolean.parseBoolean(cache);
            return config;

        } catch (IOException | RuntimeException e) {
            System.err.println("Config error: " + e.getMessage());
            System.exit(1);
            return null;
        }
    }

    private static String required(Map<String, String> values, String key) {
        String value = values.get(key);
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Missing required key: " + key);
        }
        return value;
    }

    private static String requiredIpv4(Map<String, String> values, String key) {
        String value = required(values, key);
        if (!UpstreamClient.isValidIpv4(value)) {
            throw new IllegalArgumentException("Invalid IPv4 for " + key + ": " + value);
        }
        return value;
    }

    private static int requiredInt(Map<String, String> values, String key, int min, int max) {
        String value = required(values, key);
        return parseInt(value, key, min, max);
    }

    private static int optionalInt(Map<String, String> values, String key, int fallback,
                                   int min, int max) {
        String value = values.get(key);
        return value == null ? fallback : parseInt(value, key, min, max);
    }

    private static int parseInt(String value, String key, int min, int max) {
        final int result;
        try {
            result = Integer.parseInt(value);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Invalid integer for " + key + ": " + value);
        }
        if (result < min || result > max) {
            throw new IllegalArgumentException(key + " must be between " + min + " and " + max);
        }
        return result;
    }
}
