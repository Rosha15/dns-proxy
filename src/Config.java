public class Config {
    public String upstreamIp = "127.0.0.1";
    public int upstreamPort = 5354;
    public int upstreamTimeoutSec = 2;
    public String sinkholeIp = "0.0.0.0";
    public String proxyIp = "192.0.2.10";
    public int localTtl = 60;
    public String adListFile = "ad_list.txt";
    public String sanctionListFile = "sanction_list.txt";
    public boolean cacheEnabled = true;
}