package io.prometheus.client.exporter.common.formatter.prometheus;

import io.prometheus.client.Collector;
import io.prometheus.client.TextFormatter;
import io.prometheus.client.exporter.common.TextFormat;
import io.prometheus.client.exporter.common.formatter.DoubleUtil;

import java.io.IOException;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.List;

import static io.prometheus.client.exporter.common.TextFormat.*;

/**
 * Implement TextFormat#write004.
 */
public class PrometheusTextFormatter extends TextFormatter {
    public static final List<Collector.Type> TYPE_SUPPORTED =
            Arrays.asList(
                    Collector.Type.GAUGE,
                    Collector.Type.COUNTER,
                    Collector.Type.SUMMARY,
                    Collector.Type.HISTOGRAM);


    private final MetricsWriter writer0;
    private final MetricsWriter[] writers;

    public PrometheusTextFormatter(MetricsWriter writer, MetricsWriter writer0) {
        super(writer);
        this.writer0 = writer0;
        this.writers = new MetricsWriter[]{writer, writer0};
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T extends MetricSnapshot<?>> void format(T snapshot) throws IOException {
        Collector.Type type = snapshot.type;
        if (!TYPE_SUPPORTED.contains(type)) {
            return;
        }

        if (type.equals(Collector.Type.GAUGE)) {
            GaugeMetricsSnapshotParser.INSTANCE.parse((GaugeMetricSnapshot) snapshot, this.writers);
        } else if (type.equals(Collector.Type.COUNTER)) {
            CounterMetricsSnapshotParser.INSTANCE.parse((CounterMetricSnapshot) snapshot, this.writers);
        } else if (type.equals(Collector.Type.SUMMARY)) {
            SummaryMetricsSnapshotParser.INSTANCE.parse((SummaryMetricSnapshot) snapshot, this.writers);
        } else if (type.equals(Collector.Type.HISTOGRAM)) {
            HistogramMetricsSnapshotParser.INSTANCE.parse((HistogramMetricSnapshot) snapshot, this.writers);
        }
    }

    @Override
    public void format(Enumeration<Collector.MetricFamilySamples> mfs) throws IOException {
        TextFormat.write004(this.writer, mfs);
    }

    @Override
    protected void flush() throws IOException {
        this.writer.append(this.writer0);
    }

    static void formatValue(MetricsWriter writer, String name, List<?> labelNames,
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

    static void formatHeader(MetricsWriter writer, String name, String help, Collector.Type type)
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
}
