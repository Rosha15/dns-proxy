
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.nio.charset.StandardCharsets;
import java.io.IOException;

public class MockUpstreamServer {

    public static void main(String[] args) {
        int counter = 0;

        try (DatagramSocket server = new DatagramSocket(5354)) {

            while (true) {
                byte[] buffer = new byte[1024];

                DatagramPacket request =
                        new DatagramPacket(buffer, buffer.length);

                server.receive(request);

                String message = new String(
                        request.getData(),
                        request.getOffset(),
                        request.getLength(),
                        StandardCharsets.UTF_8
                );

                String domain = message.startsWith("QUERY ")
                        ? message.substring(6).trim() : "";

                String reply;

                if (domain.equals("missing.example.net")) {
                    reply = "NOT_FOUND";
                } else {
                    reply = "OK 198.51.100.7 5";
                }

                byte[] replyBytes =
                        reply.getBytes(StandardCharsets.UTF_8);

                DatagramPacket response = new DatagramPacket(
                        replyBytes,
                        replyBytes.length,
                        request.getAddress(),
                        request.getPort()
                );

                server.send(response);

                counter++;

                System.out.println(
                        "Request #" + counter + ": "
                                + domain + " -> " + reply
                );
            }

        } catch (IOException error) {
            System.err.println(
                    "Upstream error: " + error.getMessage()
            );
        }
    }
}
