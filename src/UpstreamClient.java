import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.nio.charset.StandardCharsets;

public class UpstreamClient {
    private final Config config;

    public UpstreamClient(Config config) {
        this.config = config;
    }

    public DnsResponse query(String normalizedDomain) {
        try (DatagramSocket socket = new DatagramSocket()) {
            socket.setSoTimeout(config.upstreamTimeoutSec * 1000);

            byte[] data = ("QUERY " + normalizedDomain).getBytes(StandardCharsets.UTF_8);
            InetAddress serverAddress = InetAddress.getByName(config.upstreamIp);
            DatagramPacket request = new DatagramPacket(
                    data, data.length, serverAddress, config.upstreamPort);
            socket.send(request);

            byte[] buffer = new byte[1024];
            DatagramPacket replyPacket = new DatagramPacket(buffer, buffer.length);
            socket.receive(replyPacket);

            // Do not accept a packet from another address/port as the response.
            if (!replyPacket.getAddress().equals(serverAddress)
                    || replyPacket.getPort() != config.upstreamPort) {
                return serverError(normalizedDomain);
            }

            String reply = new String(replyPacket.getData(), replyPacket.getOffset(),
                    replyPacket.getLength(), StandardCharsets.UTF_8).trim();
            return parseReply(normalizedDomain, reply);

        } catch (IOException | IllegalArgumentException ex) {
            // Includes timeout, unreachable upstream and invalid socket parameters.
            return serverError(normalizedDomain);
        }
    }

    static DnsResponse parseReply(String domain, String responseText) {
        if ("NOT_FOUND".equals(responseText)) {
            return new DnsResponse(domain, "UPSTREAM", "NOT_FOUND", "-", "-");
        }

        String[] fields = responseText.split("\\s+");
        if (fields.length != 3 || !"OK".equals(fields[0])
                || !isValidIpv4(fields[1])) {
            return serverError(domain);
        }

        try {
            int ttl = Integer.parseInt(fields[2]);
            if (ttl < 0) {
                return serverError(domain);
            }
            return new DnsResponse(domain, "UPSTREAM", "OK", fields[1], String.valueOf(ttl));
        } catch (NumberFormatException e) {
            return serverError(domain);
        }
    }

    static boolean isValidIpv4(String ip) {
        if (ip == null) {
            return false;
        }
        String[] parts = ip.split("\\.", -1);
        if (parts.length != 4) {
            return false;
        }
        for (String part : parts) {
            if (part.isEmpty() || part.length() > 3) {
                return false;
            }
            int number = 0;
            for (int i = 0; i < part.length(); i++) {
                char c = part.charAt(i);
                if (c < '0' || c > '9') {
                    return false;
                }
                number = number * 10 + (c - '0');
            }
            if (number > 255) {
                return false;
            }
        }
        return true;
    }

    private static DnsResponse serverError(String domain) {
        return new DnsResponse(domain, "UPSTREAM", "SERVER_ERROR", "-", "-");
    }
}
