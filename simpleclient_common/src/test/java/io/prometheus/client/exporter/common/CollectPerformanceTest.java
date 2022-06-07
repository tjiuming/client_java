package io.prometheus.client.exporter.common;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.prometheus.client.*;
import io.prometheus.client.exporter.common.formatter.PrometheusTextFormatter;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

public class CollectPerformanceTest {


    @Before
    public void setup() {
        String cluster = "standalone";
        Gauge messageIn =
                Gauge.build()
                        .name("message_queue_message_in")
                        .help("message_queue_message_in")
                        .labelNames("cluster", "topic")
                        .register();
        Gauge messageOut =
                Gauge.build()
                        .name("message_queue_message_out")
                        .help("message_queue_message_out")
                        .labelNames("cluster", "topic")
                        .register();
        Gauge messageDispatched =
                Gauge.build()
                        .name("message_queue_message_dispatched")
                        .help("message_queue_message_dispatched")
                        .labelNames("cluster", "topic")
                        .register();
        Gauge messageAcked =
                Gauge.build()
                        .name("message_queue_message_acked")
                        .help("message_queue_message_acked")
                        .labelNames("cluster", "topic")
                        .register();
        Gauge activeConsumers =
                Gauge.build()
                        .name("message_queue_active_consumers")
                        .help("message_queue_active_consumers")
                        .labelNames("cluster", "topic")
                        .register();

        Counter txnNew =
                Counter.build()
                        .name("message_queue_txn_new")
                        .help("message_queue_txn_new")
                        .labelNames("cluster", "topic")
                        .register();
        Counter txnActive =
                Counter.build()
                        .name("message_queue_txn_active")
                        .help("message_queue_txn_active")
                        .labelNames("cluster", "topic")
                        .register();
        Counter txnAborted =
                Counter.build()
                        .name("message_queue_txn_aborted")
                        .help("message_queue_txn_aborted")
                        .labelNames("cluster", "topic")
                        .register();
        Counter txnCommitted =
                Counter.build()
                        .name("message_queue_txn_committed")
                        .help("message_queue_txn_committed")
                        .labelNames("cluster", "topic")
                        .register();
        Counter txnPending =
                Counter.build()
                        .name("message_queue_txn_pending")
                        .help("message_queue_txn_pending")
                        .labelNames("cluster", "topic")
                        .register();

        for (int a = 0; a < 500000; a++) {
            String topic = UUID.randomUUID().toString();
            messageIn.labels(cluster, topic).inc();
            messageOut.labels(cluster, topic).inc();
            messageDispatched.labels(cluster, topic).inc();
            messageAcked.labels(cluster, topic).inc();
            activeConsumers.labels(cluster, topic).inc();

            txnNew.labels(cluster, topic).inc();
            txnActive.labels(cluster, topic).inc();
            txnAborted.labels(cluster, topic).inc();
            txnCommitted.labels(cluster, topic).inc();
            txnPending.labels(cluster, topic).inc();
        }
    }


    @Test
    public void testMetricsFormatter() throws Exception {
        for (int a = 0; a < 10; a++) {
            System.out.println("collect metrics start");
            ByteBuf buffer = ByteBufAllocator.DEFAULT.compositeDirectBuffer(256);
            TextFormatter.MetricsWriter writer = new ByteBufferMetricsWriter(buffer);
            TextFormat.write004(writer, CollectorRegistry.defaultRegistry.metricFamilySamples());
            writer.close();
            System.out.println("collect metrics finished");
            Thread.sleep(30000);
        }
    }

    @Test
    public void testPrometheusTextFormat() throws Exception {
        for (int a = 0; a < 10; a++) {
            System.out.println("collect metrics start");
            ByteBuf buffer1 = ByteBufAllocator.DEFAULT.compositeDirectBuffer(256);
            TextFormatter.MetricsWriter mainWriter = new ByteBufferMetricsWriter(buffer1);

            ByteBuf buffer2 = ByteBufAllocator.DEFAULT.compositeDirectBuffer(256);
            TextFormatter.MetricsWriter subWriter = new ByteBufferMetricsWriter(buffer2);

            TextFormatter formatter = new PrometheusTextFormatter(mainWriter, subWriter);
            CollectorRegistry.defaultRegistry.collect(formatter, new Predicate<String>() {
                @Override
                public boolean test(String s) {
                    return true;
                }
            });
            mainWriter.close();
            subWriter.close();
            System.out.println("collect metrics finished");
            Thread.sleep(30000);
        }
    }


    public static final class ByteBufferMetricsWriter extends TextFormatter.MetricsWriter {
        private static final Charset CHARSET = StandardCharsets.UTF_8;
        private final ByteBuf buffer;

        public ByteBufferMetricsWriter(ByteBuf buffer) {
            assert buffer != null;
            this.buffer = buffer;
        }

        @Override
        public void write(String str) throws IOException {
            buffer.writeCharSequence(str, CHARSET);
        }

        @Override
        public void write(String str, int off, int len) throws IOException {
            int end = off + len;
            for (int index = off; index < end; index++) {
                this.buffer.writeChar(str.charAt(index));
            }
        }

        @Override
        public void write(byte[] bytes, int offset, int length) throws IOException {
            this.buffer.writeBytes(bytes, offset, length);
        }

        @Override
        public void write(char[] cbuf, int off, int len) throws IOException {
            int end = off + len;
            for (int index = off; index < end; index++) {
                this.buffer.writeChar(cbuf[index]);
            }
        }

        @Override
        public void flush() throws IOException {

        }

        @Override
        public void close() throws IOException {
            if (this.buffer.refCnt() > 0) {
                this.buffer.release();
            }
        }

        @Override
        public <T> T getBuffer() {
            return (T) buffer;
        }

        @Override
        public TextFormatter.MetricsWriter append(TextFormatter.MetricsWriter other) throws IOException {
            ByteBuf buffer = other.getBuffer();
            this.buffer.writeBytes(buffer);
            return this;
        }
    }
}
