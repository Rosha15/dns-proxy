
import java.util.Scanner;

public class Main {

    public static void main(String[] args) {
        Config config = ConfigLoader.loadConfig("config.txt");
        System.out.println("DNS Proxy ready.");
        Scanner scanner = new Scanner(System.in);
        UpstreamClient upstreamClient = new UpstreamClient(config);
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

            String response = upstreamClient.queryUpstream(domain);
            System.out.println("Domain: " + domain);
            System.out.println("Source: UPSTREAM");

            if (response.startsWith("OK")) {
                String[] parts = response.split("\\s+");
                System.out.println("Status: OK");
                System.out.println("IP: " + (parts.length > 1 ? parts[1] : "-"));
                System.out.println("TTL: " + (parts.length > 2 ? parts[2] : "-"));
            } else if (response.equals("NOT_FOUND")) {
                System.out.println("Status: NOT_FOUND");
                System.out.println("IP: -");
                System.out.println("TTL: -");
            } else {
                System.out.println("Status: SERVER_ERROR");
                System.out.println("IP: -");
                System.out.println("TTL: -");
            }
            System.out.println();

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
