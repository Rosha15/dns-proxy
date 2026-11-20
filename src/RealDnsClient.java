import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.concurrent.ThreadLocalRandom;

// B6: minimal RFC 1035 DNS A query over UDP. No external dependency needed
public class RealDnsClient {
    private final Config config;

    public RealDnsClient(Config config) {
        this.config = config;
    }

    public DnsResponse query(String domain) {
        try {
            int transactionId = ThreadLocalRandom.current().nextInt(0x10000);
            byte[] query = DnsQueryEncoder.encodeQuery(domain, transactionId);
            InetAddress address = InetAddress.getByName(config.upstreamIp);
            try (DatagramSocket socket = new DatagramSocket()) {
                // Connected UDP discards packets from unrelated sources.
                socket.connect(address, config.upstreamPort);
                socket.setSoTimeout(config.upstreamTimeoutSec * 1000);
                socket.send(new DatagramPacket(query, query.length));

                byte[] data = new byte[4096];
                DatagramPacket reply = new DatagramPacket(data, data.length);
                socket.receive(reply);
                return DnsResponseDecoder.decodeResponse(domain, data, reply.getLength(), transactionId);
            }
        } catch (IOException | RuntimeException ex) {
            return DnsResponse.serverError(domain);
        }
    }

}
