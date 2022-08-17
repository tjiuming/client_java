package io.prometheus.client.exporter.common.formatter.prometheus;

import io.prometheus.client.Collector;
import io.prometheus.client.Histogram;
import io.prometheus.client.TextFormatter;
import io.prometheus.client.exporter.common.formatter.MetricsSnapshotParser;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

final class HistogramMetricsSnapshotParser implements MetricsSnapshotParser<TextFormatter.HistogramMetricSnapshot> {
    static final HistogramMetricsSnapshotParser INSTANCE = new HistogramMetricsSnapshotParser();

    private HistogramMetricsSnapshotParser() {
    }

    @Override
    public void parse(TextFormatter.HistogramMetricSnapshot snapshot, TextFormatter.MetricsWriter... writers) throws IOException {
        final String name = snapshot.name;
        final String help = snapshot.help;
        final double[] buckets = snapshot.buckets;
        final Collector.Type type = snapshot.type;
        final List<String> labelNames = snapshot.labelNames;
        final Map<List<String>, Histogram.Child> children = snapshot.children;
        final TextFormatter.MetricsWriter writer = writers[0];
        final TextFormatter.MetricsWriter writer0 = writers[1];

        PrometheusTextFormatter.formatHeader(writer, name, help, type);

        final String _sum = name + "_sum";
        final String _created = name + "_created";
        final String _bucket = name + "_bucket";
        final String _count = name + "_count";

        PrometheusTextFormatter.formatHeader(writer0, _created, help, Collector.Type.GAUGE);

        List<String> labelNames0 = new ArrayList<String>();
        List<Object> labelValues0 = new ArrayList<Object>();
        for (Map.Entry<List<String>, Histogram.Child> entry : children.entrySet()) {
            Histogram.Child.Value value = entry.getValue().get();
            for (int i = 0; i < value.buckets.length; ++i) {
                this.appendLabelNames(labelNames0, labelNames);
                this.appendLabelValues(labelValues0, entry.getKey(), buckets[i]);
                PrometheusTextFormatter.formatValue(writer, _bucket, labelNames0, labelValues0, value.buckets[i]);
                labelNames0.clear();
                labelValues0.clear();
            }

            List<String> labelValues = entry.getKey();
            PrometheusTextFormatter.formatValue(writer, _count, labelNames, labelValues, value.buckets[buckets.length - 1]);
            PrometheusTextFormatter.formatValue(writer, _sum, labelNames, labelValues, value.sum);
            PrometheusTextFormatter.formatValue(writer0, name, labelNames, labelValues, value.created / 1000.0);
        }
    }

    private void appendLabelNames(List<String> target, List<String> source) {
        target.addAll(source);
        target.add("le");
    }

    private void appendLabelValues(List<Object> target, List<String> source, double value) {
        target.addAll(source);
        target.add(value);
    }
}
