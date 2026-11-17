public final class DnsResponse {
    private final String domain;
    private final String source;
    private final String status;
    private final String ip;
    private final String ttl;

    public DnsResponse(String domain, String source, String status,
                       String ip, String ttl) {
        this.domain = domain;
        this.source = source;
        this.status = status;
        this.ip = ip;
        this.ttl = ttl;
    }

    public static DnsResponse ok(String domain, String source, String ip, int ttl) {
        return new DnsResponse(domain, source, "OK", ip, String.valueOf(ttl));
    }

    public static DnsResponse notFound(String domain) {
        return new DnsResponse(domain, "UPSTREAM", "NOT_FOUND", "-", "-");
    }

    public static DnsResponse serverError(String domain) {
        return new DnsResponse(domain, "UPSTREAM", "SERVER_ERROR", "-", "-");
    }

    public static DnsResponse invalidInput(String domain) {
        return new DnsResponse(domain, "NONE", "INVALID_INPUT", "-", "-");
    }

    public String getDomain() { return domain; }
    public String getSource() { return source; }
    public String getStatus() { return status; }
    public String getIp() { return ip; }
    public String getTtl() { return ttl; }
}
