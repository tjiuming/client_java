package io.prometheus.client.exporter.common.formatter.prometheus;

import io.prometheus.client.Collector;
import io.prometheus.client.Summary;
import io.prometheus.client.TextFormatter;
import io.prometheus.client.exporter.common.formatter.MetricsSnapshotParser;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

final class SummaryMetricsSnapshotParser implements MetricsSnapshotParser<TextFormatter.SummaryMetricSnapshot> {
    static final SummaryMetricsSnapshotParser INSTANCE = new SummaryMetricsSnapshotParser();

    private SummaryMetricsSnapshotParser() {
    }

    @Override
    public void parse(TextFormatter.SummaryMetricSnapshot snapshot, TextFormatter.MetricsWriter... writers) throws IOException {
        final String name = snapshot.name;
        final String help = snapshot.help;
        final Collector.Type type = snapshot.type;
        final List<String> labelNames = snapshot.labelNames;
        final Map<List<String>, Summary.Child> children = snapshot.children;
        final TextFormatter.MetricsWriter writer = writers[0];
        final TextFormatter.MetricsWriter writer0 = writers[1];

        PrometheusTextFormatter.formatHeader(writer, name, help, type);

        final String _sum = name + "_sum";
        final String _count = name + "_count";
        final String _created = name + "_created";

        PrometheusTextFormatter.formatHeader(writer0, _created, help, Collector.Type.GAUGE);

        List<String> labelNames0 = new ArrayList<String>();
        List<Object> labelValues0 = new ArrayList<Object>();
        for (Map.Entry<List<String>, Summary.Child> entry : children.entrySet()) {
            Summary.Child.Value value = entry.getValue().get();
            for (Map.Entry<Double, Double> q : value.quantiles.entrySet()) {
                this.appendLabelNames(labelNames0, labelNames);
                this.appendLabelValues(labelValues0, entry.getKey(), q.getValue());
                PrometheusTextFormatter.formatValue(writer, name, labelNames0, labelValues0, q.getValue());
                labelNames0.clear();
                labelValues0.clear();
            }

            List<String> labelValues = entry.getKey();
            PrometheusTextFormatter.formatValue(writer, _count, labelNames, labelValues, value.count);
            PrometheusTextFormatter.formatValue(writer, _sum, labelNames, labelValues, value.sum);
            PrometheusTextFormatter.formatValue(writer0, _created, labelNames, labelValues, value.created / 1000.0);
        }
    }

    private void appendLabelNames(List<String> target, List<String> source) {
        target.addAll(source);
        target.add("quantile");
    }

    private void appendLabelValues(List<Object> target, List<String> source, double value) {
        target.addAll(source);
        target.add(value);
    }
}
