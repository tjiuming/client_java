package io.prometheus.client.exporter.common.formatter.prometheus;

import io.prometheus.client.Collector;
import io.prometheus.client.Counter;
import io.prometheus.client.TextFormatter;
import io.prometheus.client.exporter.common.formatter.MetricsSnapshotParser;

import java.io.IOException;
import java.util.List;
import java.util.Map;

final class CounterMetricsSnapshotParser implements MetricsSnapshotParser<TextFormatter.CounterMetricSnapshot> {

    static final CounterMetricsSnapshotParser INSTANCE = new CounterMetricsSnapshotParser();

    private CounterMetricsSnapshotParser() {
    }

    @Override
    public void parse(TextFormatter.CounterMetricSnapshot snapshot, TextFormatter.MetricsWriter... writers) throws IOException {
        final String name = snapshot.name;
        final String help = snapshot.help;
        final Collector.Type type = snapshot.type;
        final List<String> labelNames = snapshot.labelNames;
        final Map<List<String>, Counter.Child> children = snapshot.children;
        final TextFormatter.MetricsWriter writer = writers[0];
        final TextFormatter.MetricsWriter writer0 = writers[1];

        PrometheusTextFormatter.formatHeader(writer, name, help, type);

        final String _total = name + "_total";
        final String _created = name + "_created";

        PrometheusTextFormatter.formatHeader(writer0, _created, help, Collector.Type.GAUGE);

        for (Map.Entry<List<String>, Counter.Child> entry : children.entrySet()) {
            Counter.Child child = entry.getValue();
            List<String> labelValues = entry.getKey();
            PrometheusTextFormatter.formatValue(writer, _total, labelNames, labelValues, child.get());
            PrometheusTextFormatter.formatValue(writer0, _created, labelNames, labelValues, child.created() / 1000.0);
        }

    }
}
