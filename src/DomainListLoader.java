import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class DomainListLoader {
    public static Set<String> loadList(String filename) {
        Set<String> domains = new HashSet<>();
        Path path = Path.of(filename);

        if (!Files.exists(path)) {
            System.err.println("Config error: List file not found: " + filename);
            System.exit(1);
        }

        try {
            List<String> lines = Files.readAllLines(path);
            for (String line : lines) {
                line = line.trim();
                if (line.isEmpty() || line.startsWith("#")) {
                    continue;
                }

                if (line.contains(" ") || line.contains("\t")) {
                    System.out.println("Warning: bad line in " + filename + ": " + line);
                    continue;
                }
                domains.add(DomainNormalizer.normalize(line));
            }
        } catch (Exception e) {
            System.err.println("Error reading file " + filename + ": " + e.getMessage());
        }

        return domains;
    }
}