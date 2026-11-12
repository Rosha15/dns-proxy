public class CacheEntry {
    public String ip;
    public int ttl;
    public long savedAtMillis;

    public CacheEntry(String ip, int ttl) {
        this.ip = ip;
        this.ttl = ttl;
        this.savedAtMillis = System.currentTimeMillis();
    }

    public int getRemainingTtl() {
        long elapsed = (System.currentTimeMillis() - savedAtMillis) / 1000;
        return (int) (ttl - elapsed);
    }
}