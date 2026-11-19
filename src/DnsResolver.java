import java.util.Objects;
import java.util.Set;

public class DnsResolver {
    private final Config config;
    private Set<String> adList;
    private Set<String> sanctionList;
    private final DnsCache cache;
    private final UpstreamClient upstreamClient;

    public DnsResolver(Config config, Set<String> adList,
                       Set<String> sanctionList, DnsCache cache) {
        this.config = Objects.requireNonNull(config, "config");
        this.adList = Set.copyOf(Objects.requireNonNull(adList, "adList"));
        this.sanctionList = Set.copyOf(Objects.requireNonNull(sanctionList, "sanctionList"));
        this.cache = Objects.requireNonNull(cache, "cache");
        this.upstreamClient = new UpstreamClient(config);
    }

    public void reloadLists(Set<String> newAds, Set<String> newSanctions) {
        Set<String> checkedAds = Set.copyOf(Objects.requireNonNull(newAds, "newAds"));
        Set<String> checkedSanctions = Set.copyOf(Objects.requireNonNull(newSanctions, "newSanctions"));
        this.adList = checkedAds;
        this.sanctionList = checkedSanctions;
    }

    public DnsResponse resolve(String input) {
        String rawDomain = input == null ? "" : input.trim();

        if (!DomainValidator.isValidDomain(rawDomain)) {
            String displayDomain = rawDomain.isEmpty() ? "(empty)" : rawDomain;
            return new DnsResponse(displayDomain, "NONE", "INVALID_INPUT", "-", "-");
        }
        String domain = DomainNormalizer.normalize(rawDomain);

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

        if (!"OK".equals(upstreamResponse.getStatus())) {
            return upstreamResponse;
        }

        try {
            int ttl = Integer.parseInt(upstreamResponse.getTtl());
            if (ttl < 0 || !UpstreamClient.isValidIpv4(upstreamResponse.getIp())) {
                return serverError(domain);
            }
            if (config.cacheEnabled && ttl > 0) {
                cache.put(domain, upstreamResponse.getIp(), ttl);
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
