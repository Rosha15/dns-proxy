
import java.util.Locale;

public class DomainUtils {

    public static String normalize(String domain) {
        domain = domain.trim().toLowerCase();
        if (domain.endsWith(".")) {
            domain = domain.substring(0, domain.length() - 1);
        }

        return domain;
    }

    public static boolean matches(String domain, String entry) {
        return domain.equals(entry) || domain.endsWith("." + entry);
    }
}
