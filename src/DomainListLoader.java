import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;

public class DomainListLoader {
    public static Set<String> loadList(String filename) {
        try {
            return readList(filename);
        } catch (IOException | RuntimeException e) {
            System.err.println("Config error: Cannot read list file " + filename + ": " + e.getMessage());
            System.exit(1);
            return null; // Unreachable after exit.
        }
    }

    public static Set<String> readList(String filename) throws IOException {
        Path path = Path.of(filename);
        if (!Files.isRegularFile(path)) {
            throw new IOException("List file not found: " + filename);
        }
        Set<String> domains = new HashSet<>();
        for (String originalLine : Files.readAllLines(path, StandardCharsets.UTF_8)) {
            String line = originalLine.trim();
            if (line.isEmpty() || line.startsWith("#")) {
                continue;
            }
            if (!DomainValidator.isValidDomain(line)) {
                System.out.println("Warning: bad line in " + filename + ": " + line);
                continue;
            }
            domains.add(DomainNormalizer.normalize(line));
        }
        return domains;
    }
}
