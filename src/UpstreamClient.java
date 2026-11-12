import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketTimeoutException;
import java.nio.charset.StandardCharsets;

public class UpstreamClient {
    private final Config config;

    public UpstreamClient(Config config) {
        this.config = config;
    }

    public DnsResponse query(String normalizedDomain) {
        try (DatagramSocket socket = new DatagramSocket()) {
            socket.setSoTimeout(config.upstreamTimeoutSec * 1000);

            String message = "QUERY " + normalizedDomain;
            byte[] data = message.getBytes(StandardCharsets.UTF_8);
            InetAddress host = InetAddress.getByName(config.upstreamIp);

            DatagramPacket packet = new DatagramPacket(data, data.length, host, config.upstreamPort);
            socket.send(packet);

            byte[] buffer = new byte[1024];
            DatagramPacket replyPacket = new DatagramPacket(buffer, buffer.length);
            socket.receive(replyPacket);
            String responseText = new String(replyPacket.getData(), 0, replyPacket.getLength(), StandardCharsets.UTF_8).trim();
            if (responseText.startsWith("OK")) {
                String[] parts = responseText.split("\\s+");
                if (parts.length >= 3) {
                    String ip = parts[1];
                    String ttl = parts[2];
                    return new DnsResponse(normalizedDomain, "UPSTREAM", "OK", ip, ttl);
                }
                return new DnsResponse(normalizedDomain, "UPSTREAM", "SERVER_ERROR", "-", "-");
            } else if (responseText.equals("NOT_FOUND")) {
                return new DnsResponse(normalizedDomain, "UPSTREAM", "NOT_FOUND", "-", "-");
            } else {
                return new DnsResponse(normalizedDomain, "UPSTREAM", "SERVER_ERROR", "-", "-");
            }
        } catch (SocketTimeoutException e) {
            return new DnsResponse(normalizedDomain, "UPSTREAM", "SERVER_ERROR", "-", "-");
        } catch (Exception e) {
            return new DnsResponse(normalizedDomain, "UPSTREAM", "SERVER_ERROR", "-", "-");
        }
    }
}