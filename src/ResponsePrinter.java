public final class ResponsePrinter {
    private ResponsePrinter() { }

    public static void print(DnsResponse response) {
        System.out.println("Domain : " + response.getDomain());
        System.out.println("Source : " + response.getSource());
        System.out.println("Status : " + response.getStatus());
        System.out.println("IP : " + response.getIp());
        System.out.println("TTL : " + response.getTtl());
        System.out.println();
    }
}
