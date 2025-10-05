package serd.test

import cats.effect.{IO, Resource}
import fs2.kafka.*
import io.confluent.kafka.serializers.KafkaAvroDeserializer
import org.apache.avro.generic.GenericRecord

import java.util.Properties

class ProcessGenericRecord(config: MyConfig) {

  private val consumerSettings: ConsumerSettings[IO, Array[Byte], Array[Byte]] =
    ConsumerSettings[IO, Array[Byte], Array[Byte]]
      .withBootstrapServers(config.bootstrap)
      .withGroupId("binary-gr-group")
      .withAutoOffsetReset(AutoOffsetReset.Earliest)
      .withProperty("auto.register.schemas", "true")

  // Configure the Confluent Avro deserializer
  private val deserializerProps = new Properties()
  deserializerProps.put("schema.registry.url", config.schemaRegistry)
  deserializerProps.put("specific.avro.reader", "false")

  private val avroDeserializer: KafkaAvroDeserializer = new KafkaAvroDeserializer()
  avroDeserializer.configure(deserializerProps.asInstanceOf[java.util.Map[String, AnyRef]], false)

  val myStream: fs2.Stream[IO, CommittableConsumerRecord[IO, Array[Byte], Array[Byte]]] =
    KafkaConsumer.stream(consumerSettings).subscribeTo(config.topic).records

  def run(): IO[Unit] =
    myStream.evalTap { (x: CommittableConsumerRecord[IO, Array[Byte], Array[Byte]]) =>
      val record: GenericRecord = decodeValue(avroDeserializer, x.record.topic, x.record.value)
      IO.println(s"Decoded GenericRecord Using Confluent KafkaAvroDeserializer: $record")
    }.compile.drain

  def decodeValue(deserializer: KafkaAvroDeserializer, topic: String, bytes: Array[Byte]): GenericRecord =
    deserializer.deserialize(topic, bytes).asInstanceOf[GenericRecord]

}
