public final class RequestStats {
    private long total;
    private long adBlock;
    private long sanction;
    private long cache;
    private long upstream;
    private long errors;

    public void record(DnsResponse result) {
        if (result == null) {
            throw new IllegalArgumentException("result must not be null");
        }
        total++;
        switch (result.getSource()) {
            case "AD_BLOCK" -> adBlock++;
            case "SANCTION" -> sanction++;
            case "CACHE" -> cache++;
            case "UPSTREAM" -> upstream++;
            default -> { }
        }
        if ("INVALID_INPUT".equals(result.getStatus())
                || "SERVER_ERROR".equals(result.getStatus())) {
            errors++;
        }
    }

    public String format() {
        return "Total: " + total
                + " | AD_BLOCK: " + adBlock
                + " | SANCTION: " + sanction
                + " | CACHE: " + cache
                + " | UPSTREAM: " + upstream
                + " | ERRORS: " + errors;
    }
}
