package serd.test

import cats.effect.IO
import fs2.kafka.*
import cats.effect.{IO, Resource}
import vulcan.{AvroDeserializer, AvroSerializer, AvroSettings, SchemaRegistryClientSettings}
import serd.test.MyCodec.{keyCodec, valueCodec}

case class MyKey(name: String)
case class MyRecord(name: String, age: Int)

class Process(config: MyConfig) {
  val avroSettings: AvroSettings[IO] = AvroSettings(SchemaRegistryClientSettings[IO](config.schemaRegistry))

  implicit val valueDeserializer: Resource[IO, ValueDeserializer[IO, MyRecord]] = AvroDeserializer[MyRecord].forValue(avroSettings)
  implicit val keyDeserializer: Resource[IO, KeyDeserializer[IO, MyKey]] = AvroDeserializer[MyKey].forKey(avroSettings)
  implicit val valueSerializer: Resource[IO, ValueSerializer[IO, MyRecord]] = AvroSerializer[MyRecord].forValue(avroSettings)
  implicit val keySerializer: Resource[IO, KeySerializer[IO, MyKey]] = AvroSerializer[MyKey].forKey(avroSettings)

  val consumerSettings: ConsumerSettings[IO, MyKey, MyRecord] =
    ConsumerSettings[IO, MyKey, MyRecord](keyDeserializer, valueDeserializer)
      .withAutoOffsetReset(AutoOffsetReset.Earliest)
      .withBootstrapServers(config.bootstrap)
      .withGroupId("MyRecord-Group")
      .withEnableAutoCommit(false)
      .withProperty("auto.register.schemas", "true")

  val myStream: fs2.Stream[IO, KafkaConsumer[IO, MyKey, MyRecord]] =
    KafkaConsumer.stream(consumerSettings).subscribeTo(config.topic)

  def run(): IO[Unit] =
    myStream.records.evalTap { x => IO.println(x.record) }.compile.drain

}
