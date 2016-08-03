package org.deeplearning4j.streaming.routes;

import kafka.serializer.StringEncoder;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.apache.camel.CamelContext;
import org.apache.camel.Processor;
import org.apache.camel.builder.RouteBuilder;
import org.deeplearning4j.streaming.serde.RecordSerializer;

/**
 * A Camel Java DSL Router
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class CamelKafkaRouteBuilder extends RouteBuilder {
    private String topicName;
    private String kafkaBrokerList;
    private static RecordSerializer serializer = new RecordSerializer();
    private String writableConverter = "org.datavec.api.io.converters.SelfWritableConverter";
    private String datavecMarshaller = "org.datavec.camel.component.csv.marshaller.ListStringInputMarshaller";
    private String inputUri;
    private String inputFormat;
    private Processor processor;
    private String dataTypeUnMarshal;
    private String zooKeeperHost = "localhost";
    private int zooKeeperPort = 2181;
    /**
     * Let's configure the Camel routing rules using Java code...
     */
    @Override
    public void configure() {
        from(inputUri)
                .unmarshal(dataTypeUnMarshal)
                .to(String.format("datavec://%s?inputMarshaller=%s&writableConverter=%s",inputFormat,datavecMarshaller,writableConverter))
                .process(processor)
                .to(String.format("kafka:%s?topic=%s&zookeeperHost=%szookeeperPort=%d&serializerClass=%s&keySerializerClass=%s",
                        kafkaBrokerList,
                        topicName,
                        zooKeeperHost,zooKeeperPort, StringEncoder.class.getName(),StringEncoder.class.getName()));
    }



    public void setContext(CamelContext camelContext) {
        super.setContext(camelContext);
    }



    public static class Builder {
        private String writableConverter = "org.datavec.api.io.converters.SelfWritableConverter";
        private String datavecMarshaller = "org.datavec.camel.component.csv.marshaller.ListStringInputMarshaller";
        private String inputUri;
        private String topicName;
        private String kafkaBrokerList = "localhost:9092";
        private CamelContext camelContext;
        private String inputFormat;
        private Processor processor;
        private String dataTypeUnMarshal;
        private String zooKeeperHost = "localhost";
        private int zooKeeperPort = 2181;

        public Builder zooKeeperHost(String zooKeeperHost) {
            this.zooKeeperHost = zooKeeperHost;
            return this;
        }

        public Builder zooKeeperPort(int zooKeeperPort) {
            this.zooKeeperPort = zooKeeperPort;
            return this;
        }

        public Builder processor(Processor processor) {
            this.processor = processor;
            return this;
        }

        public Builder kafkaBrokerList(String kafkaBrokerList) {
            this.kafkaBrokerList = kafkaBrokerList;
            return this;
        }

        public Builder inputFormat(String inputFormat) {
            this.inputFormat = inputFormat;
            return this;
        }

        public Builder camelContext(CamelContext camelContext) {
            this.camelContext = camelContext;
            return this;
        }

        public Builder inputUri(String inputUri) {
            this.inputUri = inputUri;
            return this;
        }

        public Builder writableConverter(String writableConverter) {
            this.writableConverter = writableConverter;
            return this;
        }


        public Builder datavecMarshaller(String datavecMarshaller) {
            this.datavecMarshaller = datavecMarshaller;
            return this;
        }

        public Builder dataTypeUnMarshal(String dataTypeUnMarshal) {
            this.dataTypeUnMarshal = dataTypeUnMarshal;
            return this;
        }


        public Builder topicName(String topicName) {
            this.topicName = topicName;
            return this;
        }

        private void assertStringNotNUllOrEmpty(String value,String name)  {
            if(value == null || value.isEmpty())
                throw new IllegalStateException(String.format("Please define a %s",name));

        }

        public CamelKafkaRouteBuilder build() {
            CamelKafkaRouteBuilder routeBuilder;
            assertStringNotNUllOrEmpty(inputUri,"input uri");
            assertStringNotNUllOrEmpty(topicName,"topic name");
            assertStringNotNUllOrEmpty(kafkaBrokerList,"kafka broker");
            assertStringNotNUllOrEmpty(inputFormat,"input format");
            routeBuilder = new CamelKafkaRouteBuilder(
                    topicName,
                    kafkaBrokerList,
                    writableConverter,
                    datavecMarshaller,
                    inputUri,
                    inputFormat
                    ,processor,
                    dataTypeUnMarshal,
                    zooKeeperHost,
                    zooKeeperPort);
            if(camelContext != null)
                routeBuilder.setContext(camelContext);
            return routeBuilder;
        }

    }




}
