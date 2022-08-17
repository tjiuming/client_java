package io.prometheus.client.exporter.common.formatter.prometheus;

import io.prometheus.client.Collector;
import io.prometheus.client.Gauge;
import io.prometheus.client.TextFormatter;
import io.prometheus.client.exporter.common.formatter.MetricsSnapshotParser;

import java.io.IOException;
import java.util.List;
import java.util.Map;

final class GaugeMetricsSnapshotParser implements MetricsSnapshotParser<TextFormatter.GaugeMetricSnapshot> {
    static final GaugeMetricsSnapshotParser INSTANCE = new GaugeMetricsSnapshotParser();

    private GaugeMetricsSnapshotParser() {
    }

    @Override
    public void parse(TextFormatter.GaugeMetricSnapshot snapshot, TextFormatter.MetricsWriter... writers) throws IOException {
        final String name = snapshot.name;
        final String help = snapshot.help;
        final Collector.Type type = snapshot.type;
        final List<String> labelNames = snapshot.labelNames;
        final Map<List<String>, Gauge.Child> children = snapshot.children;
        final TextFormatter.MetricsWriter writer = writers[0];

        PrometheusTextFormatter.formatHeader(writer, name, help, type);

        for (Map.Entry<List<String>, Gauge.Child> entry : children.entrySet()) {
            PrometheusTextFormatter.formatValue(writer, name, labelNames, entry.getKey(), entry.getValue().get());
        }
    }
}
