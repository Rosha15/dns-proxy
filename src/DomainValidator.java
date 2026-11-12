public class DomainValidator {
    public static boolean isValidDomain(String input) {
        if (input == null || input.isEmpty()) {
            return false;
        }

        if (input.contains(" ") || input.contains("..")) {
            return false;
        }

        if (input.startsWith(".")) {
            return false;
        }


        for (int i = 0; i < input.length(); i++) {
            char c = input.charAt(i);
            boolean isLetter = (c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z');
            boolean isDigit = (c >= '0' && c <= '9');
            boolean isHyphen = (c == '-');
            boolean isDot = (c == '.');
            if (!isLetter && !isDigit && !isHyphen && !isDot) {
                return false;
            }
        }
        return true;
    }
}