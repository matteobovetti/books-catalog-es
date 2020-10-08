package es4kafka

import java.util.UUID

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
import akka.http.scaladsl.model.{HttpRequest, StatusCodes}
import akka.http.scaladsl.unmarshalling.Unmarshal
import akka.kafka.ProducerSettings
import akka.kafka.scaladsl.SendProducer
import com.davideicardi.kaa.SchemaRegistry
import com.davideicardi.kaa.kafka.GenericSerde
import com.sksamuel.avro4s.{Decoder, Encoder, SchemaFor}
import es4kafka.http.RpcActions
import es4kafka.streaming.{MetadataService, MetadataStoreInfo, StateStores}
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.common.serialization.Serdes
import org.apache.kafka.streams.KafkaStreams
import org.apache.kafka.streams.scala.Serdes.String
import org.apache.kafka.streams.state.QueryableStoreTypes
import spray.json._

import scala.concurrent.duration._
import scala.concurrent.{Await, ExecutionContext, Future}

class DefaultCommandSender[TCommand >: Null : SchemaFor : Encoder : Decoder, TEvent](
                                actorSystem: ActorSystem,
                                schemaRegistry: SchemaRegistry,
                                serviceConfig: ServiceConfig,
                                aggregateConfig: AggregateConfig,
                                metadataService: MetadataService,
                                streams: KafkaStreams,
                                eventJsonFormat: RootJsonFormat[TEvent],
                              ) extends CommandSender[TCommand, TEvent] {
  private implicit val commandSerde: GenericSerde[Envelop[TCommand]] = new GenericSerde(schemaRegistry)
  private implicit val entityJsonFormat: RootJsonFormat[TEvent] = eventJsonFormat

  private implicit val system: ActorSystem = actorSystem
  private implicit val executionContext: ExecutionContext = system.dispatcher

  private val producerSettings = ProducerSettings(actorSystem, String.serializer(), commandSerde.serializer())
    .withBootstrapServers(serviceConfig.kafka_brokers)
  private val producer = SendProducer(producerSettings)(actorSystem)
  private val msgIdSerde = Serdes.UUID()

  def send(key: String, command: TCommand): Future[MsgId] = {
    val msgId = MsgId.random()
    val envelop = Envelop(msgId, command)
    producer
      .send(new ProducerRecord(aggregateConfig.topicCommands, key, envelop))
      .map(_ => msgId)
  }

  override def wait(
                     id: MsgId,
                     retries: Int = 10,
                     delay: FiniteDuration = 500.milliseconds): Future[Option[TEvent]] = {
    retry
      .Backoff(max = retries, delay = delay)
      .apply(() => fetchEvent(id))(retry.Success.option, executionContext)
  }

  def close(): Unit = {
    val _ = Await.result(producer.close(), 1.minute)
  }

  private def fetchEvent(key: MsgId): Future[Option[TEvent]] = {
    val hostForStore = metadataService.hostForStoreAndKey(
      aggregateConfig.storeEventsByMsgId,
      key.uuid,
      msgIdSerde.serializer()
    )

    hostForStore.map(metadata => {
      // store is hosted on another process, REST Call
      if (metadata.isLocal)
        fetchEventLocal(key)
      else
        fetchEventRemote(metadata, key)
    }).getOrElse(Future(None))
  }

  private def fetchEventRemote(host: MetadataStoreInfo, key: MsgId): Future[Option[TEvent]] = {
    val requestPath = s"${aggregateConfig.httpPrefix}/${RpcActions.events}/${RpcActions.one}/$key"
    val requestUri = s"http://${host.host}:${host.port}/$requestPath"
    Http().singleRequest(HttpRequest(uri = requestUri))
      .flatMap { response =>
        if (response.status == StatusCodes.NotFound)
          Future(None)
        else
          Unmarshal(response.entity)
            .to[TEvent]
            .map(Some(_))
      }
  }

  private def fetchEventLocal(key: MsgId): Future[Option[TEvent]] = Future {
    val store = StateStores.waitUntilStoreIsQueryable(
      aggregateConfig.storeEventsByMsgId,
      QueryableStoreTypes.keyValueStore[UUID, TEvent](),
      streams
    )

    store.flatMap(s => Option(s.get(key.uuid)))
  }
}