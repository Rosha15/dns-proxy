import java.io.IOException;
import java.util.Scanner;
import java.util.Set;

public class Main {
    public static void main(String[] args) {
        Config config = ConfigLoader.loadConfig("config.txt");
        Set<String> adList = DomainListLoader.loadList(config.adListFile);
        Set<String> sanctionList = DomainListLoader.loadList(config.sanctionListFile);
        DnsCache cache = new DnsCache();
        DnsResolver resolver = new DnsResolver(config, adList, sanctionList, cache);
        RequestStats stats = new RequestStats();

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
                if (input.equalsIgnoreCase("stats")) {
                    System.out.println(stats.format());
                    continue;
                }
                if (input.equalsIgnoreCase("reload")) {
                    // Load BOTH lists before changing the resolver: no half-applied update.
                    try {
                        Set<String> newAds = DomainListLoader.readList(config.adListFile);
                        Set<String> newSanctions = DomainListLoader.readList(config.sanctionListFile);
                        resolver.reloadLists(newAds, newSanctions);
                        System.out.println("Lists reloaded.");
                    } catch (IOException | RuntimeException ex) {
                        System.err.println("Reload error: " + ex.getMessage());
                    }
                    continue;
                }

                DnsResponse response = resolver.resolve(input);
                response.printResult();
                stats.record(response);
            }
        }
    }
}
