import java.io.IOException;
import java.util.Objects;
import java.util.Set;


public class DnsResolver {
    private final Config config;
    private volatile DomainLists lists;
    private final DnsCache cache;
    private final UpstreamClient upstreamClient;
    private final RealDnsClient realDnsClient;

    public DnsResolver(Config config, Set<String> adList,
                       Set<String> sanctionList, DnsCache cache) {
        this.config = Objects.requireNonNull(config, "config");
        this.lists = new DomainLists(adList, sanctionList);
        this.cache = Objects.requireNonNull(cache, "cache");
        this.upstreamClient = new UpstreamClient(config);
        this.realDnsClient = new RealDnsClient(config);
    }

    /** B2: load both new lists first; atomically replace them only on success. */
    public void reloadLists() throws IOException {
        Set<String> ad = DomainListLoader.readList(config.adListFile);
        Set<String> sanctions = DomainListLoader.readList(config.sanctionListFile);
        this.lists = new DomainLists(ad, sanctions);
    }

    public DnsResponse resolve(String input) {
        String raw = input == null ? "" : input.trim();
        if (!DomainValidator.isValidDomain(raw)) {
            return DnsResponse.invalidInput(raw.isEmpty() ? "(empty)" : raw);
        }
        String domain = DomainNormalizer.normalize(raw);
        DomainLists currentLists = lists;

        if (isInList(domain, currentLists.ads)) {
            return DnsResponse.ok(domain, "AD_BLOCK", config.sinkholeIp, config.localTtl);
        }
        if (isInList(domain, currentLists.sanctions)) {
            return DnsResponse.ok(domain, "SANCTION", config.proxyIp, config.localTtl);
        }
        if (config.cacheEnabled) {
            CacheEntry cached = cache.get(domain);
            if (cached != null) {
                int remaining = cached.getRemainingTtl();
                if (remaining >= 1) return DnsResponse.ok(domain, "CACHE", cached.ip, remaining);
            }
        }

        DnsResponse upstream = config.realDnsEnabled
                ? realDnsClient.query(domain) : upstreamClient.query(domain);
        if (!"OK".equals(upstream.getStatus())) return upstream;

        try {
            long ttl = Long.parseLong(upstream.getTtl());
            if (ttl < 0 || !UpstreamClient.isValidIpv4(upstream.getIp())) {
                return DnsResponse.serverError(domain);
            }
            if (config.cacheEnabled && ttl > 0) {
                cache.put(domain, upstream.getIp(), (int) Math.min(ttl, Integer.MAX_VALUE));
            }
        } catch (NumberFormatException e) {
            return DnsResponse.serverError(domain);
        }
        return upstream;
    }

    public DnsCache getCache() { return cache; }

    private static boolean isInList(String domain, Set<String> entries) {
        for (String entry : entries) {
            if (DomainMatcher.matches(domain, entry)) return true;
        }
        return false;
    }

    private static final class DomainLists {
        final Set<String> ads;
        final Set<String> sanctions;
        DomainLists(Set<String> ads, Set<String> sanctions) {
            this.ads = Set.copyOf(Objects.requireNonNull(ads));
            this.sanctions = Set.copyOf(Objects.requireNonNull(sanctions));
        }
    }
}
