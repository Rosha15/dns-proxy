import java.util.Locale;

public class DomainNormalizer {
    public static String normalize(String domain) {
        if (domain == null) {
            return "";
        }
        String normalized = domain.trim().toLowerCase(Locale.ROOT);

        if (normalized.endsWith(".") && normalized.length() > 1) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }
        return normalized;
    }
}
