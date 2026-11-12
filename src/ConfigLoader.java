import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ConfigLoader {
    public static Config loadConfig(String filename) {
        Config config = new Config();
        Path path = Path.of(filename);

        if (!Files.exists(path)) {
            System.err.println("Config error: config.txt file not found.");
            System.exit(1);
        }

        try {
            List<String> lines = Files.readAllLines(path);
            Map<String, String> rawMap = new HashMap<>();

            for (String line : lines) {
                line = line.trim();
                if (line.isEmpty() || line.startsWith("#")) {
                    continue;
                }
                int eqIdx = line.indexOf('=');
                if (eqIdx == -1) {
                    continue;
                }
                String key = line.substring(0, eqIdx).trim();
                String value = line.substring(eqIdx + 1).trim();
                rawMap.put(key, value);
            }


            if (rawMap.containsKey("upstream_ip")) config.upstreamIp = rawMap.get("upstream_ip");
            if (rawMap.containsKey("upstream_port")) config.upstreamPort = Integer.parseInt(rawMap.get("upstream_port"));
            if (rawMap.containsKey("upstream_timeout_sec")) config.upstreamTimeoutSec = Integer.parseInt(rawMap.get("upstream_timeout_sec"));
            if (rawMap.containsKey("sinkhole_ip")) config.sinkholeIp = rawMap.get("sinkhole_ip");
            if (rawMap.containsKey("proxy_ip")) config.proxyIp = rawMap.get("proxy_ip");
            if (rawMap.containsKey("local_ttl")) config.localTtl = Integer.parseInt(rawMap.get("local_ttl"));
            if (rawMap.containsKey("ad_list_file")) config.adListFile = rawMap.get("ad_list_file");
            if (rawMap.containsKey("sanction_list_file")) config.sanctionListFile = rawMap.get("sanction_list_file");
            if (rawMap.containsKey("cache_enabled")) config.cacheEnabled = Boolean.parseBoolean(rawMap.get("cache_enabled"));

        } catch (Exception e) {
            System.err.println("Config error: Invalid configuration format - " + e.getMessage());
            System.exit(1);
        }

        return config;
    }
}