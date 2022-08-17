package io.prometheus.client.exporter.common.formatter;

import io.prometheus.client.TextFormatter;

import java.io.IOException;

public interface MetricsSnapshotParser<T> {
    void parse(T snapshot, TextFormatter.MetricsWriter... writers) throws IOException;
}
