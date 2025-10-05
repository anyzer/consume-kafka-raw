package serd.test

import _root_.vulcan.generic.*
import cats.effect.{IO, Resource}
import fs2.kafka.vulcan.{AvroDeserializer, AvroSerializer, AvroSettings, SchemaRegistryClientSettings}
import vulcan.Codec
import fs2.kafka.*


object MyCodec {
  implicit val valueCodec: Codec[MyRecord] = Codec.derive[MyRecord]
  implicit val keyCodec: Codec[MyKey] = Codec.derive[MyKey]

  val avroSettings: AvroSettings[IO] = AvroSettings(SchemaRegistryClientSettings[IO]("http://localhost:8081"))

  implicit val valueDeserializer: Resource[IO, ValueDeserializer[IO, MyRecord]] = AvroDeserializer[MyRecord].forValue(avroSettings)
  implicit val keyDeserializer: Resource[IO, KeyDeserializer[IO, MyKey]] = AvroDeserializer[MyKey].forKey(avroSettings)
  implicit val valueSerializer: Resource[IO, ValueSerializer[IO, MyRecord]] = AvroSerializer[MyRecord].forValue(avroSettings)
  implicit val keySerializer: Resource[IO, KeySerializer[IO, MyKey]] = AvroSerializer[MyKey].forKey(avroSettings)

  val consumerSettings: ConsumerSettings[IO, MyKey, MyRecord] =
    ConsumerSettings[IO, MyKey, MyRecord](keyDeserializer, valueDeserializer)
      .withAutoOffsetReset(AutoOffsetReset.Earliest)
      .withBootstrapServers("localhost:9092")
      .withGroupId("MyRecord-Group")
      .withEnableAutoCommit(false)

}
