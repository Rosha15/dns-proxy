import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Scanner;
import java.util.Set;

public class Main {
    public static void main(String[] args) {
        Config config = ConfigLoader.loadConfig("config.txt");
        Set<String> ads = DomainListLoader.loadList(config.adListFile);
        Set<String> sanctions = DomainListLoader.loadList(config.sanctionListFile);
        DnsCache cache = new DnsCache(config.cacheMaxEntries);
        DnsResolver resolver = new DnsResolver(config, ads, sanctions, cache);
        RequestStats stats = new RequestStats();
        RequestLogger logger = new RequestLogger(Path.of("log.txt"));

        System.out.println("DNS Proxy ready.");
        try (Scanner scanner = new Scanner(System.in)) {
            while (true) {
                System.out.print("Enter domain (or exit): ");
                if (!scanner.hasNextLine()) break;
                String input = scanner.nextLine().trim();

                if (input.equalsIgnoreCase("exit")) {
                    System.out.println("Bye.");
                    break;
                }
                if (input.equalsIgnoreCase("stats")) {
                    System.out.println(stats.summary());
                    continue;
                }
                if (input.equalsIgnoreCase("reload")) {
                    try {
                        resolver.reloadLists();
                        System.out.println("Lists reloaded.");
                    } catch (IOException | RuntimeException error) {
                        System.err.println("Reload error: " + error.getMessage());
                    }
                    continue;
                }
                if (input.equalsIgnoreCase("cache")) {
                    List<DnsCache.CacheSnapshot> entries = config.cacheEnabled
                            ? cache.snapshot() : List.of();
                    if (entries.isEmpty()) {
                        System.out.println("Cache is empty.");
                    } else {
                        for (DnsCache.CacheSnapshot entry : entries) {
                            System.out.println(entry.domain + " | " + entry.ip
                                    + " | " + entry.remainingTtl);
                        }
                    }
                    continue;
                }

                DnsResponse response = resolver.resolve(input);
                response.printResult();
                stats.record(response);
                try {
                    logger.log(response);
                } catch (IOException | RuntimeException error) {
                    System.err.println("Log error: " + error.getMessage());
                }
            }
        }
    }
}
