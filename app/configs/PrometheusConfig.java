package configs;

import io.prometheus.client.exporter.HTTPServer;
import io.prometheus.client.hotspot.DefaultExports;

public class PrometheusConfig {
    public static void init() {
        try {
            DefaultExports.initialize();
            HTTPServer server = new HTTPServer(9095);
            System.out.println("Prometheus metrics exposed at http://localhost:9095/metrics");

        } catch (Exception e) {
            System.err.println("Failed to initialize Prometheus server: " + e.getMessage());
        }
    }
}
