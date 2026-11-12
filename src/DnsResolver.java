import java.util.Set;

public class DnsResolver {
    private final Config config;
    private final Set<String> adList;
    private final Set<String> sanctionList;
    private final DnsCache cache;
    private final UpstreamClient upstreamClient;

    public DnsResolver(Config config, Set<String> adList, Set<String> sanctionList, DnsCache cache) {
        this.config = config;
        this.adList = adList;
        this.sanctionList = sanctionList;
        this.cache = cache;
        this.upstreamClient = new UpstreamClient(config);
    }

    public DnsResponse resolve(String domain) {
        for (String entry : adList) {
            if (DomainMatcher.matches(domain, entry)) {
                return new DnsResponse(domain, "AD_BLOCK", "OK", config.sinkholeIp, String.valueOf(config.localTtl));
            }
        }

        for (String entry : sanctionList) {
            if (DomainMatcher.matches(domain, entry)) {
                return new DnsResponse(domain, "SANCTION", "OK", config.proxyIp, String.valueOf(config.localTtl));
            }
        }

        if (config.cacheEnabled) {
            CacheEntry cachedEntry = cache.get(domain);
            if (cachedEntry != null) {
                return new DnsResponse(domain, "CACHE", "OK", cachedEntry.ip, String.valueOf(cachedEntry.getRemainingTtl()));
            }
        }

        DnsResponse upstreamResponse = upstreamClient.query(domain);

        if (upstreamResponse.status.equals("OK") && !upstreamResponse.ttl.equals("-")) {
            try {
                int ttlValue = Integer.parseInt(upstreamResponse.ttl);
                if (config.cacheEnabled && ttlValue > 0) {
                    cache.put(domain, upstreamResponse.ip, ttlValue);
                }
            } catch (NumberFormatException ignored) {}
        }

        return upstreamResponse;
    }
}