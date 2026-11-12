public class DomainMatcher {
    public static boolean matches(String name, String entry) {
        if (name == null || entry == null) {
            return false;
        }
        String normName = DomainNormalizer.normalize(name);
        String normEntry = DomainNormalizer.normalize(entry);


        return normName.equals(normEntry) || normName.endsWith("." + normEntry);
    }
}