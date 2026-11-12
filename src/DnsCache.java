import java.util.HashMap;
import java.util.Map;

public class DnsCache {
    private final Map<String, CacheEntry> cache = new HashMap<>();

    public void put(String domain, String ip, int ttl) {
        if (ttl > 0) {
            cache.put(domain, new CacheEntry(ip, ttl));
        }
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
}