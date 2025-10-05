package serd.test

import cats.effect.IO
import cats.effect.unsafe.implicits.global
import com.sksamuel.avro4s.{AvroSchema, SchemaFor}
import fs2.kafka.{KafkaProducer, ProducerSettings}
import org.apache.avro.Schema
import org.scalatest.funsuite.AnyFunSuite
import MyCodec.{keyCodec, valueCodec}
import fs2.kafka.vulcan.AvroSerializer
import fs2.kafka.vulcan.*
import fs2.kafka.*
import cats.effect.unsafe.implicits.global
import cats.implicits._


class ProduceTest extends AnyFunSuite {

  test("Produces data") {
//    val config = new MyConfig("dev")
    val config = new MyConfig("dev")

    val kschema: Schema = SchemaFor[MyKey].schema
    val vschema: Schema = SchemaFor[MyRecord].schema
    println(kschema)
    println(vschema)

    val avroSettings: AvroSettings[IO] = AvroSettings(SchemaRegistryClientSettings[IO](config.schemaRegistry))
    val keySerializer                  = AvroSerializer[MyKey].forKey(avroSettings).use(IO.pure).unsafeRunSync()
    val valueSerializer                = AvroSerializer[MyRecord].forValue(avroSettings).use(IO.pure).unsafeRunSync()

    val producerSettings = ProducerSettings[IO, MyKey, MyRecord](keySerializer, valueSerializer)
      .withBootstrapServers(config.bootstrap)
      .withProperty("auto.register.schemas", "false")

    val listOfRec: List[ProducerRecord[MyKey, MyRecord]] =
      List.range(1, 101).map(i => (MyKey(name = "Test"), MyRecord(name = "AAAAAA BBBBBB", age = 30))).map { x =>
        fs2.kafka.ProducerRecord(config.topic, x._1, x._2)
      }

    val producerBatch: IO[Unit] = KafkaProducer
      .stream(producerSettings)
      .evalMap { producer =>
        val records = fs2.kafka.ProducerRecords(listOfRec)
        producer.produce(records)
      }
      .compile
      .drain

    (producerBatch.replicateA(100000)).unsafeRunSync()
  }

}
