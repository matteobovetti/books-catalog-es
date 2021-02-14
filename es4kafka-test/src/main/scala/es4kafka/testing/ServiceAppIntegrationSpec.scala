package es4kafka.testing

import akka.Done
import akka.actor.ActorSystem
import akka.kafka.Subscriptions
import akka.stream.scaladsl.{Sink, Source}
import akka.testkit.TestKit
import com.google.inject.Injector
import es4kafka.akkaStream.kafka.KafkaGraphDsl._
import es4kafka.ServiceApp
import es4kafka.configs.ServiceConfig
import es4kafka.kafka.{ConsumerFactory, ProducerFactory}
import es4kafka.modules.Module
import net.codingwell.scalaguice.InjectorExtensions._
import net.manub.embeddedkafka.{EmbeddedKafka, EmbeddedKafkaConfig}
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.common.serialization.Serde
import org.scalatest._
import org.scalatest.funspec.AsyncFunSpecLike
import org.scalatest.matchers.should.Matchers

import java.util.UUID
import scala.concurrent._
import scala.concurrent.duration._

abstract class ServiceAppIntegrationSpec(
    name: String
) extends TestKit(ActorSystem(name)) with AsyncFunSpecLike with Matchers with BeforeAndAfterAll {
  override def afterAll(): Unit = {
    TestKit.shutdownActorSystem(system)
  }

  /**
   * Override the default test execution context to allow parallels code.
   * Otherwise tests will not work due to the Await used here.
   */
  override implicit def executionContext: ExecutionContext = ExecutionContext.global

  /**
   * Start Kafka and the specified installers (typically from a service)
   * and then execute the `body` function
   */
  protected def withRunningService(
      serviceConfig: ServiceConfig,
      installers: Seq[Module.Installer],
      init: () => Unit,
      testTimeout: FiniteDuration = 30.seconds,
  )(body: Injector => Future[Assertion]): Assertion = {
    implicit val config: EmbeddedKafkaConfig = EmbeddedKafkaConfig(kafkaPort = 9092)

    EmbeddedKafka.start()
    try {
      val injector = ServiceApp.createInjector(serviceConfig, installers)
      import net.codingwell.scalaguice.InjectorExtensions._
      val service = injector.instance[ServiceApp]

      service.start(init)
      try {
        Await.result(body(injector), testTimeout)
      } finally {
        service.shutDown("stop from test")
      }
    } finally {
      EmbeddedKafka.stop()
    }
  }


  protected def writeKafkaRecords[K: Serde, V: Serde](injector: Injector, topic: String, records: Seq[(K, V)]): Future[Done] = {
    val producerFactory = injector.instance[ProducerFactory]
    def toRecord(keyValue: (K, V)): ProducerRecord[K, V] = {
      val (key, value) = keyValue
      new ProducerRecord(topic, key, value)
    }
    Source(records)
      .via(producerFactory.producerFlowT(toRecord))
      .run()
  }

  protected def readAllKafkaRecords[K: Serde, V: Serde](
      injector: Injector,
      topic: String,
      take: Long,
      within: FiniteDuration = 30.seconds,
  ): Future[Seq[(K, V)]] = {
    val consumerFactory = injector.instance[ConsumerFactory]
    val groupId = UUID.randomUUID().toString
    consumerFactory
      .plainSourceFromEarliest[K, V](groupId, Subscriptions.topics(topic))
      .map(r => r.key() -> r.value())
      .take(take)
      .takeWithin(within)
      .runWith(Sink.seq)
  }
}