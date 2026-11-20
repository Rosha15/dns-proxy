public class RequestStats {
    private long total, adBlock, sanction, cache, upstream, errors;

    public void record(DnsResponse response) {
        total++;
        switch (response.getSource()) {
            case "AD_BLOCK": adBlock++; break;
            case "SANCTION": sanction++; break;
            case "CACHE": cache++; break;
            case "UPSTREAM": upstream++; break;
            default: break;
        }
        if (!"OK".equals(response.getStatus())) errors++;
    }

    public String summary() {
        return "Total: " + total + " | AD_BLOCK: " + adBlock
                + " | SANCTION: " + sanction + " | CACHE: " + cache
                + " | UPSTREAM: " + upstream + " | ERRORS: " + errors;
    }
}
