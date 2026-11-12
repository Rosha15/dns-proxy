import java.util.Scanner;
import java.util.Set;

public class Main {

    public static void main(String[] args) {
        Config config = ConfigLoader.loadConfig("config.txt");
        Set<String> adList = DomainListLoader.loadList(config.adListFile);
        Set<String> sanctionList = DomainListLoader.loadList(config.sanctionListFile);

        DnsCache cache = new DnsCache();
        UpstreamClient upstream = new UpstreamClient(config);

        System.out.println("DNS Proxy ready.");

        try (Scanner scanner = new Scanner(System.in)) {
            while (true) {
                System.out.print("Enter domain (or exit): ");
                if (!scanner.hasNextLine()) {
                    break;
                }

                String input = scanner.nextLine().trim();
                if (input.equalsIgnoreCase("exit")) {
                    System.out.println("Bye.");
                    break;
                }

                if (!DomainValidator.isValidDomain(input)) {
                    printResult(input.isEmpty() ? "(empty)" : input,
                            "NONE", "INVALID_INPUT", "-", "-");
                    continue;
                }

                String domain = DomainNormalizer.normalize(input);

                // Lists take priority over both cache and upstream.
                if (matchesAny(domain, adList)) {
                    printResult(domain, "AD_BLOCK", "OK", config.sinkholeIp,
                            String.valueOf(config.localTtl));
                    continue;
                }

                if (matchesAny(domain, sanctionList)) {
                    printResult(domain, "SANCTION", "OK", config.proxyIp,
                            String.valueOf(config.localTtl));
                    continue;
                }

                if (config.cacheEnabled) {
                    CacheEntry cached = cache.get(domain);
                    if (cached != null) {
                        int remainingTtl = cached.getRemainingTtl();
                        if (remainingTtl >= 1) {
                            printResult(domain, "CACHE", "OK", cached.ip,
                                    String.valueOf(remainingTtl));
                            continue;
                        }
                    }
                }

                // Only unlisted domains missing from cache use UDP.
                String reply = upstream.queryUpstream(domain);
                handleUpstreamReply(domain, reply, cache, config.cacheEnabled);
            }
        }
    }

    private static boolean matchesAny(String domain, Set<String> list) {
        for (String entry : list) {
            if (DomainMatcher.matches(domain, entry)) {
                return true;
            }
        }
        return false;
    }

    private static void handleUpstreamReply(String domain, String reply,
                                            DnsCache cache, boolean cacheEnabled) {
        if ("NOT_FOUND".equals(reply)) {
            printResult(domain, "UPSTREAM", "NOT_FOUND", "-", "-");
            return;
        }

        if (reply != null) {
            String[] parts = reply.trim().split("\\s+");
            if (parts.length == 3 && parts[0].equals("OK") && isValidIpv4(parts[1])) {
                try {
                    int ttl = Integer.parseInt(parts[2]);
                    if (ttl >= 0) {
                        if (cacheEnabled && ttl > 0) {
                            cache.put(domain, parts[1], ttl);
                        }
                        printResult(domain, "UPSTREAM", "OK", parts[1],
                                String.valueOf(ttl));
                        return;
                    }
                } catch (NumberFormatException ignored) {
                    // Malformed TTL is treated as a server error.
                }
            }
        }

        printResult(domain, "UPSTREAM", "SERVER_ERROR", "-", "-");
    }

    private static boolean isValidIpv4(String ip) {
        String[] groups = ip.split("\\.", -1);
        if (groups.length != 4) {
            return false;
        }
        for (String group : groups) {
            if (group.isEmpty() || group.length() > 3) {
                return false;
            }
            for (int i = 0; i < group.length(); i++) {
                char c = group.charAt(i);
                if (c < '0' || c > '9') {
                    return false;
                }
            }
            if (Integer.parseInt(group) > 255) {
                return false;
            }
        }
        return true;
    }

    private static void printResult(String domain, String source,
                                    String status, String ip, String ttl) {
        System.out.println("Domain : " + domain);
        System.out.println("Source : " + source);
        System.out.println("Status : " + status);
        System.out.println("IP : " + ip);
        System.out.println("TTL : " + ttl);
        System.out.println();
    }
}
