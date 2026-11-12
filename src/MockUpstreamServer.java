import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.nio.charset.StandardCharsets;

public class MockUpstreamServer {
    public static void main(String[] args) {
        int port = 5354;
        System.out.println("Mock Upstream Server is running on port " + port + "...");
        int requestCount = 0;

        try (DatagramSocket socket = new DatagramSocket(port)) {
            byte[] buffer = new byte[1024];
            while (true) {
                DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                socket.receive(packet);

                String requestText = new String(packet.getData(), 0, packet.getLength(), StandardCharsets.UTF_8).trim();
                requestCount++;

                String domain = "";
                if (requestText.startsWith("QUERY ")) {
                    domain = requestText.substring(6).trim();
                }

                String reply;
                if (domain.equals("missing.example.net")) {
                    reply = "NOT_FOUND";
                } else {
                    reply = "OK 198.51.100.7 5";
                }

                System.out.println("Request #" + requestCount + ": " + domain + " -> " + reply);

                byte[] replyData = reply.getBytes(StandardCharsets.UTF_8);
                DatagramPacket replyPacket = new DatagramPacket(
                        replyData, replyData.length,
                        packet.getAddress(), packet.getPort()
                );
                socket.send(replyPacket);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}