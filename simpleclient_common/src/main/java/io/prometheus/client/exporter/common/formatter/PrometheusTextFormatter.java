package io.prometheus.client.exporter.common.formatter;

import io.prometheus.client.*;
import io.prometheus.client.exporter.common.TextFormat;

import java.io.IOException;
import java.util.Enumeration;
import java.util.*;

import static io.prometheus.client.exporter.common.TextFormat.*;

/**
 * Implement TextFormat#write004.
 */
public class PrometheusTextFormatter extends TextFormatter {

    public static final List<Collector.Type> SUPPORTED_TYPES =
            Arrays.asList(
                    Collector.Type.COUNTER,
                    Collector.Type.GAUGE,
                    Collector.Type.SUMMARY,
                    Collector.Type.HISTOGRAM);

    private final MetricsWriter subWriter;

    public PrometheusTextFormatter(MetricsWriter writer, MetricsWriter subWriter) {
        super(writer);
        this.subWriter = subWriter;
    }

    @Override
    @SuppressWarnings("unchecked")
    public void format(MetricSnapshotSamples samples) throws IOException {
        Collector.Type type = samples.type;
        if (!SUPPORTED_TYPES.contains(type)) {
            return;
        }

        final String name = samples.name;
        final String help = samples.help;
        final List<String> labelNames = samples.labelNames;

        outputHeader(writer, samples.name, samples.help, type);

        if (type.equals(Collector.Type.GAUGE)) {
            Map<List<String>, Gauge.Child> children = (Map<List<String>, Gauge.Child>) samples.children;
            for (Map.Entry<List<String>, Gauge.Child> entry : children.entrySet()) {
                outputValue(writer, name, labelNames, entry.getKey(), entry.getValue().get());
            }
        } else if (type.equals(Collector.Type.COUNTER)) {
            final String _total = name + "_total";
            final String _created = name + "_created";

            outputHeader(subWriter, _created, help, Collector.Type.GAUGE);

            Map<List<String>, Counter.Child> children = (Map<List<String>, Counter.Child>) samples.children;
            for (Map.Entry<List<String>, Counter.Child> entry : children.entrySet()) {
                Counter.Child child = entry.getValue();
                List<String> labelValues = entry.getKey();
                outputValue(writer, _total, labelNames, labelValues, child.get());
                outputValue(subWriter, _created, labelNames, labelValues, child.created() / 1000.0);
            }
        } else if (type.equals(Collector.Type.HISTOGRAM)) {
            final String _sum = name + "_sum";
            final String _created = name + "_created";
            final String _bucket = name + "_bucket";
            final String _count = name + "_count";

            outputHeader(subWriter, _created, help, Collector.Type.GAUGE);

            HistogramMetricSnapshotSamples hsamples = (HistogramMetricSnapshotSamples) samples;
            double[] buckets = hsamples.buckets;
            Map<List<String>, Histogram.Child> children = (Map<List<String>, Histogram.Child>) hsamples.children;
            List<String> newLabelNames = new ArrayList<String>();
            List<Object> newLabelValues = new ArrayList<Object>();
            for (Map.Entry<List<String>, Histogram.Child> entry : children.entrySet()) {
                Histogram.Child.Value value = entry.getValue().get();
                for (int i = 0; i < value.buckets.length; ++i) {
                    append(newLabelNames, labelNames, "le");
                    append(newLabelValues, entry.getKey(), buckets[i]);
                    outputValue(writer, _bucket, newLabelNames, newLabelValues, value.buckets[i]);
                    newLabelNames.clear();
                    newLabelValues.clear();
                }

                List<String> labelValues = entry.getKey();
                outputValue(writer, _count, labelNames, labelValues, value.buckets[buckets.length - 1]);
                outputValue(writer, _sum, labelNames, labelValues, value.sum);
                outputValue(subWriter, name, labelNames, labelValues, value.created / 1000.0);
            }
        } else if (type.equals(Collector.Type.SUMMARY)) {
            Map<List<String>, Summary.Child> children = (Map<List<String>, Summary.Child>) samples.children;
            final String _sum = name + "_sum";
            final String _count = name + "_count";
            final String _created = name + "_created";

            outputHeader(subWriter, _created, help, Collector.Type.GAUGE);

            List<String> newLabelNames = new ArrayList<String>();
            List<Object> newLabelValues = new ArrayList<Object>();
            for (Map.Entry<List<String>, Summary.Child> entry : children.entrySet()) {
                Summary.Child.Value value = entry.getValue().get();
                for (Map.Entry<Double, Double> q : value.quantiles.entrySet()) {
                    append(newLabelNames, labelNames, "quantile");
                    append(newLabelValues, entry.getKey(), q.getValue());
                    outputValue(writer, name, newLabelNames, newLabelValues, q.getValue());
                    newLabelNames.clear();
                    newLabelValues.clear();
                }

                List<String> labelValues = entry.getKey();
                outputValue(writer, _count, labelNames, labelValues, value.count);
                outputValue(writer, _sum, labelNames, labelValues, value.sum);
                outputValue(subWriter, _created, labelNames, labelValues, value.created / 1000.0);
            }
        }
    }

    @Override
    public void format(Enumeration<Collector.MetricFamilySamples> mfs) throws IOException {
        TextFormat.write004(this.writer, mfs);
    }

    @Override
    protected void flush() throws IOException {
        this.writer.append(this.subWriter);
    }

    private static void outputValue(MetricsWriter writer, String name, List<?> labelNames,
                                    List<?> labelValues, double value) throws IOException {
        writer.write(name);
        if (labelNames.size() > 0) {
            writer.write('{');
            for (int i = 0; i < labelNames.size(); ++i) {
                writer.write(labelNames.get(i).toString());
                writer.write("=\"");

                Object v = labelValues.get(i);
                if (v instanceof Double) {
                    DoubleUtil.append(writer, (Double) v);
                } else {
                    writeEscapedLabelValue(writer, labelValues.get(i).toString());
                }
                writer.write("\",");
            }
            writer.write('}');
        }
        writer.write(' ');
        DoubleUtil.append(writer, value);
        writer.write('\n');
    }

    private static void outputHeader(MetricsWriter writer, String name, String help, Collector.Type type)
            throws IOException {
        writer.write("# HELP ");
        writer.write(name);
        if (type == Collector.Type.COUNTER) {
            writer.write("_total");
        }
        if (type == Collector.Type.INFO) {
            writer.write("_info");
        }
        writer.write(' ');
        writeEscapedHelp(writer, help);
        writer.write('\n');

        writer.write("# TYPE ");
        writer.write(name);
        if (type == Collector.Type.COUNTER) {
            writer.write("_total");
        }
        if (type == Collector.Type.INFO) {
            writer.write("_info");
        }
        writer.write(' ');
        writer.write(typeString(type));
        writer.write('\n');
    }


    private static void append(List<String> newList, List<String> oldList, String extra) {
        for (String n : oldList) {
            newList.add(n);
        }
        newList.add(extra);
    }

    private static void append(List<Object> newList, List<String> oldList, double extra) {
        for (String n : oldList) {
            newList.add(n);
        }
        newList.add(extra);
    }
}
