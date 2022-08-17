package io.prometheus.client;

import java.io.IOException;
import java.io.Writer;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;

public abstract class TextFormatter {

    protected final MetricsWriter writer;

    public TextFormatter(MetricsWriter writer) {
        if (null == writer) {
            throw new IllegalArgumentException();
        }

        this.writer = writer;
    }

    public abstract <T extends MetricSnapshot<?>> void format(T snapshot) throws IOException;

    public abstract void format(Enumeration<Collector.MetricFamilySamples> mfs) throws IOException;

    protected void flush() throws IOException {
        // noop
    }

    public static abstract class MetricSnapshot<C> {
        public final String name;
        public final String unit;
        public final Collector.Type type;
        public final String help;
        public final List<String> labelNames;
        public final Map<List<String>, C> children;

        public MetricSnapshot(
                String name,
                String unit,
                Collector.Type type,
                String help,
                List<String> labelNames,
                Map<List<String>, C> children) {
            this.name = name;
            this.unit = unit;
            this.type = type;
            this.help = help;
            this.labelNames = labelNames;
            this.children = children;
        }
    }

    public static class GaugeMetricSnapshot extends MetricSnapshot<Gauge.Child> {

        public GaugeMetricSnapshot(
                String name,
                String unit,
                Collector.Type type,
                String help,
                List<String> labelNames,
                Map<List<String>, Gauge.Child> children) {
            super(name, unit, type, help, labelNames, children);
        }
    }

    public static class CounterMetricSnapshot extends MetricSnapshot<Counter.Child> {

        public CounterMetricSnapshot(
                String name,
                String unit,
                Collector.Type type,
                String help,
                List<String> labelNames,
                Map<List<String>, Counter.Child> children) {
            super(name, unit, type, help, labelNames, children);
        }
    }

    public static class SummaryMetricSnapshot extends MetricSnapshot<Summary.Child> {

        public SummaryMetricSnapshot(
                String name,
                String unit,
                Collector.Type type,
                String help,
                List<String> labelNames,
                Map<List<String>, Summary.Child> children) {
            super(name, unit, type, help, labelNames, children);
        }
    }


    public static class HistogramMetricSnapshot extends MetricSnapshot<Histogram.Child> {
        public final double[] buckets;

        public HistogramMetricSnapshot(
                String name,
                String unit,
                Collector.Type type,
                String help,
                List<String> labelNames,
                Map<List<String>, Histogram.Child> children,
                double[] buckets) {
            super(name, unit, type, help, labelNames, children);
            this.buckets = buckets;
        }
    }

    public abstract static class MetricsWriter extends Writer {
        public void write(byte[] bytes) throws IOException {
            this.write(bytes, 0, bytes.length);
        }

        public abstract void write(byte[] bytes, int offset, int length) throws IOException;

        public <T> T getBuffer() {
            throw new UnsupportedOperationException();
        }

        public MetricsWriter append(MetricsWriter other) throws IOException {
            throw new UnsupportedOperationException();
        }
    }
}
