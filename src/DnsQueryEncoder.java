import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

//Team member 1: convert an ASCII domain name into a binary DNS A question
public final class DnsQueryEncoder {
    private DnsQueryEncoder() {}
    static byte[] encodeQuery(String domain, int id) throws IOException {
        String[] labels = domain.split("\\.", -1);
        int nameBytes = 1;
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        DataOutputStream out = new DataOutputStream(bytes);
        out.writeShort(id);
        out.writeShort(0x0100); // standard query, recursion desired
        out.writeShort(1);      // one question
        out.writeShort(0);
        out.writeShort(0);
        out.writeShort(0);
        for (String label : labels) {
            byte[] labelBytes = label.getBytes(StandardCharsets.US_ASCII);
            if (labelBytes.length < 1 || labelBytes.length > 63) {
                throw new IOException("Invalid DNS label length");
            }
            nameBytes += labelBytes.length + 1;
            if (nameBytes > 255) throw new IOException("DNS name is too long");
            out.writeByte(labelBytes.length);
            out.write(labelBytes);
        }
        out.writeByte(0);
        out.writeShort(1); // QTYPE A
        out.writeShort(1); // QCLASS IN
        out.flush();
        return bytes.toByteArray();
    }

}
