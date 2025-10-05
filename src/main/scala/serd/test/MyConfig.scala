package serd.test

class MyConfig(env: String) {
  private val bootstrap_dev =
    "kafka-1.kafka.dev.analytics.in.cld:9092,kafka-2.kafka.dev.analytics.in.cld:9092,kafka-3.kafka.dev.analytics.in.cld:9092"
  private val bootstrap_local       = "localhost:9092"
  private val schema_registry_dev   = "https://kafka-schema-registry.kafka.dev.analytics.in.cld"
  private val schema_registry_local = "http://localhost:8081"

//  val topic = "my_test_topic"
  val topic = "chen_test_topic"

  val bootstrap: String = env match {
    case "dev"   => bootstrap_dev
    case "local" => bootstrap_local
    case _       => throw new IllegalArgumentException(s"Unknown env: $env")
  }

  val schemaRegistry: String = env match {
    case "dev"   => schema_registry_dev
    case "local" => schema_registry_local
    case _       => throw new IllegalArgumentException(s"Unknown env: $env")
  }
}
