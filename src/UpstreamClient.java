import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.nio.charset.StandardCharsets;

public class UpstreamClient {
    private final Config config;

    public UpstreamClient(Config config) {
        this.config = config;
    }

    public String queryUpstream(String normalizedDomain) {
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

            return new String(reply.getData(), 0, reply.getLength(), StandardCharsets.UTF_8).trim();
        } catch (java.net.SocketTimeoutException e) {
            return "SERVER_ERROR";
        } catch (Exception e) {
            return "SERVER_ERROR";
        }
    }
}