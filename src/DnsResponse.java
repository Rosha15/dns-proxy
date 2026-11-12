public class DnsResponse {
    public String domain;
    public String source;
    public String status;
    public String ip;
    public String ttl;

    public DnsResponse(String domain, String source, String status, String ip, String ttl) {
        this.domain = domain;
        this.source = source;
        this.status = status;
        this.ip = ip;
        this.ttl = ttl;
    }

    public void printResult() {
        System.out.println("Domain: " + (domain.isEmpty() ? "(empty)" : domain));
        System.out.println("Source: " + source);
        System.out.println("Status: " + status);
        System.out.println("IP: " + ip);
        System.out.println("TTL: " + ttl);
        System.out.println();
    }
}