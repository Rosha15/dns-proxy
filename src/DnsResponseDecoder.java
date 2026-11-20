import java.io.IOException;
import java.nio.charset.StandardCharsets;

//Team member 2: safely parse a DNS A response (including compressed names).
public final class DnsResponseDecoder {
    private DnsResponseDecoder() {}
    static DnsResponse decodeResponse(String domain, byte[] data, int len, int expectedId) {
        try {
            if (len < 12) return DnsResponse.serverError(domain);
            if (u16(data, 0) != expectedId) return DnsResponse.serverError(domain);
            int flags = u16(data, 2);
            if ((flags & 0x8000) == 0 || (flags & 0x7800) != 0 || (flags & 0x0200) != 0) {
                return DnsResponse.serverError(domain); // not response, non-query opcode or truncated
            }
            int rcode = flags & 0x000F;
            if (rcode == 3) return DnsResponse.notFound(domain); // NXDOMAIN
            if (rcode != 0) return DnsResponse.serverError(domain);

            int questionCount = u16(data, 4);
            int answerCount = u16(data, 6);
            if (questionCount != 1) return DnsResponse.serverError(domain);

            int offset = 12;
            NamePosition question = parseName(data, len, offset);
            offset = question.next;
            if (offset + 4 > len) return DnsResponse.serverError(domain);
            int questionType = u16(data, offset);
            int questionClass = u16(data, offset + 2);
            if (!question.name.equalsIgnoreCase(domain)
                    || questionType != 1 || questionClass != 1) {
                return DnsResponse.serverError(domain);
            }
            offset += 4;

            for (int i = 0; i < answerCount; i++) {
                NamePosition answerName = parseName(data, len, offset);
                offset = answerName.next;
                if (offset + 10 > len) return DnsResponse.serverError(domain);
                int type = u16(data, offset);
                int recordClass = u16(data, offset + 2);
                long ttl = u32(data, offset + 4);
                int dataLength = u16(data, offset + 8);
                offset += 10;
                if (offset + dataLength > len) return DnsResponse.serverError(domain);
                if (type == 1 && recordClass == 1 && dataLength == 4) {
                    String ip = (data[offset] & 255) + "." + (data[offset + 1] & 255)
                            + "." + (data[offset + 2] & 255) + "." + (data[offset + 3] & 255);
                    return new DnsResponse(domain, "UPSTREAM", "OK", ip, Long.toString(ttl));
                }
                offset += dataLength;
            }
            return DnsResponse.notFound(domain);
        } catch (IOException | RuntimeException malformed) {
            return DnsResponse.serverError(domain);
        }
    }

    private static int u16(byte[] data, int pos) {
        return ((data[pos] & 255) << 8) | (data[pos + 1] & 255);
    }

    private static long u32(byte[] data, int pos) {
        return ((long) (data[pos] & 255) << 24)
                | ((long) (data[pos + 1] & 255) << 16)
                | ((long) (data[pos + 2] & 255) << 8)
                | (data[pos + 3] & 255L);
    }

    private static NamePosition parseName(byte[] data, int length, int offset) throws IOException {
        int at = offset;
        int next = -1;
        int jumps = 0;
        int nameLength = 0;
        StringBuilder name = new StringBuilder();
        while (true) {
            if (++jumps > 128 || at >= length) throw new IOException("Malformed DNS name");
            int b = data[at] & 255;
            if ((b & 0xC0) == 0xC0) {
                if (at + 1 >= length) throw new IOException("Truncated DNS pointer");
                int pointer = ((b & 0x3F) << 8) | (data[at + 1] & 255);
                if (pointer >= length) throw new IOException("Invalid DNS pointer");
                if (next < 0) next = at + 2;
                at = pointer;
                continue;
            }
            if ((b & 0xC0) != 0) throw new IOException("Invalid DNS label kind");
            at++;
            if (b == 0) {
                if (next < 0) next = at;
                break;
            }
            if (b > 63 || at + b > length) throw new IOException("Invalid DNS label");
            if (name.length() > 0) name.append('.');
            name.append(new String(data, at, b, StandardCharsets.US_ASCII));
            nameLength += b + 1;
            if (nameLength > 255) throw new IOException("Oversized DNS name");
            at += b;
        }
        return new NamePosition(name.toString(), next);
    }

    private static class NamePosition {
        final String name;
        final int next;
        NamePosition(String name, int next) {
            this.name = name;
            this.next = next;
        }
    }
}
