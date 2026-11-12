
import java.util.Scanner;

public class Main {

    public static void main(String[] args) {
        Config config = ConfigLoader.loadConfig("config.txt");
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

            String domain = DomainNormalizer.normalize(input);

            if (!DomainValidator.isValidDomain(domain)) {

                String displayDomain = input.trim().isEmpty()
                        ? "(empty)"
                        : input.trim();

                printInvalidInput(displayDomain);

                continue;
            }
            // DNS resolution will be implemented in later steps.
            System.out.println("Domain: " + domain);
            System.out.println("Input accepted.");
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
