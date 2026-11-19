import java.util.LinkedHashMap;
import java.util.Map;

public class DnsCache {
    private final Map<String, CacheEntry> cache = new LinkedHashMap<>();
    private final int maxEntries;

    public DnsCache(int maxEntries) {
        this.maxEntries = maxEntries;
    }

    public void put(String domain, String ip, int ttl) {
        if (ttl <= 0 || maxEntries <= 0) {
            return;
        }

        cleanExpiredEntries();

        if (cache.containsKey(domain)) {
            cache.remove(domain);
            cache.put(domain, new CacheEntry(ip, ttl));
            return;
        }

        if (cache.size() >= maxEntries) {
            String oldestKey = cache.keySet().iterator().next();
            cache.remove(oldestKey);
        }

        cache.put(domain, new CacheEntry(ip, ttl));
    }

    public CacheEntry get(String domain) {
        if (!cache.containsKey(domain)) {
            return null;
        }
        CacheEntry entry = cache.get(domain);
        if (entry.getRemainingTtl() >= 1) {
            return entry;
        } else {
            cache.remove(domain);
            return null;
        }
    }

    private void cleanExpiredEntries() {
        cache.entrySet().removeIf(entry -> entry.getValue().getRemainingTtl() < 1);
    }

    public void printCache() {
        cleanExpiredEntries();

        if (cache.isEmpty()) {
            System.out.println("Cache is empty.");
            System.out.println();
            return;
        }

        for (Map.Entry<String, CacheEntry> entry : cache.entrySet()) {
            String domain = entry.getKey();
            CacheEntry data = entry.getValue();
            System.out.println(domain + " | " + data.ip + " | " + data.getRemainingTtl());
        }
        System.out.println();
    }
}