import java.util.Objects;
import java.util.Set;

public class DnsResolver {
    private final Config config;
    private final Set<String> adList;
    private final Set<String> sanctionList;
    private final DnsCache cache;
    private final UpstreamClient upstreamClient;

    // Compatible with the constructor used by the current Main.java.
    public DnsResolver(Config config, Set<String> adList,
                       Set<String> sanctionList, DnsCache cache) {
        this.config = Objects.requireNonNull(config, "config");
        this.adList = Objects.requireNonNull(adList, "adList");
        this.sanctionList = Objects.requireNonNull(sanctionList, "sanctionList");
        this.cache = Objects.requireNonNull(cache, "cache");
        this.upstreamClient = new UpstreamClient(config);
    }

    public DnsResponse resolve(String input) {
        String rawDomain = input == null ? "" : input.trim();

        // Validate and normalize here as well; direct callers should be safe.
        if (!DomainValidator.isValidDomain(rawDomain)) {
            String displayDomain = rawDomain.isEmpty() ? "(empty)" : rawDomain;
            return new DnsResponse(displayDomain, "NONE", "INVALID_INPUT", "-", "-");
        }
        String domain = DomainNormalizer.normalize(rawDomain);

        // Lists must always take precedence over cache and upstream.
        if (isInList(domain, adList)) {
            return new DnsResponse(domain, "AD_BLOCK", "OK",
                    config.sinkholeIp, String.valueOf(config.localTtl));
        }
        if (isInList(domain, sanctionList)) {
            return new DnsResponse(domain, "SANCTION", "OK",
                    config.proxyIp, String.valueOf(config.localTtl));
        }

        if (config.cacheEnabled) {
            CacheEntry cachedEntry = cache.get(domain);
            if (cachedEntry != null) {
                int remainingTtl = cachedEntry.getRemainingTtl();
                if (remainingTtl >= 1) {
                    return new DnsResponse(domain, "CACHE", "OK",
                            cachedEntry.ip, String.valueOf(remainingTtl));
                }
            }
        }

        DnsResponse upstreamResponse = upstreamClient.query(domain);

        // A NOT_FOUND or SERVER_ERROR response is never cached.
        if (!"OK".equals(upstreamResponse.status)) {
            return upstreamResponse;
        }

        // Reject malformed responses instead of returning OK by mistake.
        try {
            int ttl = Integer.parseInt(upstreamResponse.ttl);
            if (ttl < 0 || !UpstreamClient.isValidIpv4(upstreamResponse.ip)) {
                return serverError(domain);
            }
            if (config.cacheEnabled && ttl > 0) {
                cache.put(domain, upstreamResponse.ip, ttl);
            }
        } catch (NumberFormatException | NullPointerException ex) {
            return serverError(domain);
        }

        return upstreamResponse;
    }

    private static boolean isInList(String domain, Set<String> entries) {
        for (String entry : entries) {
            if (DomainMatcher.matches(domain, entry)) {
                return true;
            }
        }
        return false;
    }

    private static DnsResponse serverError(String domain) {
        return new DnsResponse(domain, "UPSTREAM", "SERVER_ERROR", "-", "-");
    }
}
