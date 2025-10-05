package serd.test

import cats.effect.{IO, Resource}
import fs2.kafka.vulcan.*
import fs2.kafka.*
import MyCodec.{keyCodec, valueCodec}

class ProcessBinary(config: MyConfig) {

  val avroSettings: AvroSettings[IO] = AvroSettings(SchemaRegistryClientSettings[IO](config.schemaRegistry))

  val valueDeserializer: Resource[IO, ValueDeserializer[IO, MyRecord]] = AvroDeserializer[MyRecord].forValue(avroSettings)
  val keyDeserializer: Resource[IO, KeyDeserializer[IO, MyKey]] = AvroDeserializer[MyKey].forKey(avroSettings)

  private val consumerSettings: ConsumerSettings[IO, Array[Byte], Array[Byte]] =
    ConsumerSettings[IO, Array[Byte], Array[Byte]]
      .withBootstrapServers(config.bootstrap)
      .withGroupId("binary-group")
      .withAutoOffsetReset(AutoOffsetReset.Earliest)

  val myStream: fs2.Stream[IO, KafkaConsumer[IO, Array[Byte], Array[Byte]]] =
    KafkaConsumer.stream(consumerSettings).subscribeTo(config.topic)

  def run(): IO[Unit] =
    valueDeserializer.use{ (vd: ValueDeserializer[IO, MyRecord]) =>
      
      myStream.records.evalTap { (x: CommittableConsumerRecord[IO, Array[Byte], Array[Byte]]) =>
        val k: Array[Byte] = x.record.key
        val v: Array[Byte] = x.record.value

        decodeValue(vd, x.record.topic, x.record.headers, v).flatMap {
          case Right(record) => IO.println(s"Decoded record: $record")
          case Left(err)    => IO.println(s"Failed to decode record: $err")
        }
      }.compile.drain
      
    }

  def decodeValue(deserializer: ValueDeserializer[IO, MyRecord], topic: String, headers: fs2.kafka.Headers, bytes: Array[Byte]): IO[Either[Throwable, MyRecord]] =
    deserializer.deserialize(topic, headers, bytes).attempt
}
