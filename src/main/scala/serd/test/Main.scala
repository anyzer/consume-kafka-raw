package serd.test

import cats.effect.{IO, IOApp}

object Main extends IOApp.Simple {

  val run: IO[Unit] =
    for {
      config <- IO(new MyConfig("dev"))
      _ <- IO.println(s"Config: bootstrap=${config.bootstrap},\nschemaRegistry=${config.schemaRegistry},\ntopic=${config.topic}")
//      _ <- new Process(config).run()
//      _ <- new ProcessBinary(config).run()
//      _ <- new ProcessGenericRecord(config).run()
      _ <- new ProcessGenericRecordManualDes(config).run()
    } yield ()

}
