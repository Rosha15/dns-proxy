import java.io.IOException;
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

    public String queryUpstream(String normalizedDomain) {
        try (DatagramSocket socket = new DatagramSocket()) {
            socket.setSoTimeout(config.upstreamTimeoutSec * 1000);

            InetAddress serverIp = InetAddress.getByName(config.upstreamIp);
            byte[] requestBytes = ("QUERY " + normalizedDomain)
                    .getBytes(StandardCharsets.UTF_8);
            DatagramPacket request = new DatagramPacket(
                    requestBytes, requestBytes.length, serverIp, config.upstreamPort);
            socket.send(request);

            byte[] responseBuffer = new byte[1024];
            DatagramPacket response = new DatagramPacket(
                    responseBuffer, responseBuffer.length);
            socket.receive(response);
            if (!response.getAddress().equals(serverIp)
                    || response.getPort() != config.upstreamPort) {
                return "SERVER_ERROR";
            }

            return new String(response.getData(), response.getOffset(),
                    response.getLength(), StandardCharsets.UTF_8).trim();

        } catch (SocketTimeoutException e) {
            return "SERVER_ERROR";
        } catch (IOException | IllegalArgumentException e) {
            return "SERVER_ERROR";
        }
    }
}
