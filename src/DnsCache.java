import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

//B3: cache inspection; B5: FIFO capacity limit
public class DnsCache {
    private final Map<String, CacheEntry> cache = new LinkedHashMap<>();
    private final int maxEntries;

    public DnsCache() {
        this(Integer.MAX_VALUE);
    }

    public DnsCache(int maxEntries) {
        if (maxEntries < 1) throw new IllegalArgumentException("maxEntries must be positive");
        this.maxEntries = maxEntries;
    }

    public void put(String domain, String ip, int ttl) {
        if (ttl <= 0) return;
        purgeExpired();
        // A refreshed record is treated as a new insertion.
        cache.remove(domain);
        while (cache.size() >= maxEntries) {
            Iterator<String> iterator = cache.keySet().iterator();
            if (!iterator.hasNext()) break;
            iterator.next();
            iterator.remove();
        }
        cache.put(domain, new CacheEntry(ip, ttl));
    }

    public CacheEntry get(String domain) {
        CacheEntry entry = cache.get(domain);
        if (entry == null) return null;
        if (entry.getRemainingTtl() >= 1) return entry;
        cache.remove(domain);
        return null;
    }

    public List<CacheSnapshot> snapshot() {
        purgeExpired();
        List<CacheSnapshot> result = new ArrayList<>();
        for (Map.Entry<String, CacheEntry> item : cache.entrySet()) {
            int ttl = item.getValue().getRemainingTtl();
            if (ttl >= 1) {
                result.add(new CacheSnapshot(item.getKey(), item.getValue().ip, ttl));
            }
        }
        return result;
    }

    private void purgeExpired() {
        cache.entrySet().removeIf(entry -> entry.getValue().getRemainingTtl() < 1);
    }

    public static final class CacheSnapshot {
        public final String domain;
        public final String ip;
        public final int remainingTtl;
        CacheSnapshot(String domain, String ip, int remainingTtl) {
            this.domain = domain;
            this.ip = ip;
            this.remainingTtl = remainingTtl;
        }
    }
}
