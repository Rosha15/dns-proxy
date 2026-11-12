
import java.util.Scanner;
import java.util.Set;

public class Main {

    public static void main(String[] args) {
        Config config = ConfigLoader.loadConfig("config.txt");
        Set<String> adList = DomainListLoader.loadList(config.adListFile);
        Set<String> sanctionList = DomainListLoader.loadList(config.sanctionListFile);
        DnsCache cache = new DnsCache();
        DnsResolver resolver = new DnsResolver(config, adList, sanctionList, cache);
        System.out.println("DNS Proxy ready.");
        Scanner scanner = new Scanner(System.in);

        while (true) {
            System.out.print("Enter domain (or exit): ");

            if (!scanner.hasNextLine()) {
                break;
            }

            String input = scanner.nextLine();

            if (input.trim().equalsIgnoreCase("exit")) {
                System.out.println("Bye.");
                break;
            }
            String trimmedInput = input.trim();

            if (!DomainValidator.isValidDomain(trimmedInput)) {
                String displayDomain = trimmedInput.isEmpty()
                        ? "(empty)"
                        : trimmedInput;

                printInvalidInput(displayDomain);

                continue;
            }
            String domain = DomainNormalizer.normalize(trimmedInput);
            DnsResponse response = resolver.resolve(domain);
            response.printResult();
        }

        scanner.close();
    }

    private static void printInvalidInput(String domain) {

        System.out.println("Domain: " + domain);
        System.out.println("Source: NONE");
        System.out.println("Status: INVALID_INPUT");
        System.out.println("IP: -");
        System.out.println("TTL: -");
        System.out.println();
    }
}
