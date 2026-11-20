import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;


public class RequestLogger {
    private static final DateTimeFormatter FORMAT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private final Path logPath;

    public RequestLogger(Path logPath) {
        this.logPath = logPath;
    }

    public void log(DnsResponse response) throws IOException {
        String line = LocalDateTime.now().format(FORMAT) + " | "
                + sanitize(response.getDomain()) + " | "
                + response.getSource() + " | " + response.getStatus() + " | "
                + response.getIp() + " | " + response.getTtl()
                + System.lineSeparator();
        Files.writeString(logPath, line, StandardCharsets.UTF_8,
                StandardOpenOption.CREATE, StandardOpenOption.APPEND);
    }

    private static String sanitize(String value) {
        return value.replace('|', '_').replace('\r', '_').replace('\n', '_');
    }
}
