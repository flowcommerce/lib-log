package io.flow.log

import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.databind.{ObjectMapper, SerializerProvider}
import com.fasterxml.jackson.databind.module.SimpleModule
import com.google.inject.assistedinject.{Assisted, AssistedInject, FactoryModuleBuilder}
import com.google.inject.{AbstractModule, Provider}
import com.rollbar.api.payload.Payload
import com.rollbar.api.payload.data.Data
import com.rollbar.notifier.Rollbar
import com.rollbar.notifier.config.ConfigBuilder
import com.rollbar.notifier.fingerprint.FingerprintGenerator
import com.rollbar.notifier.sender.result.Result
import com.rollbar.notifier.sender.{BufferedSender, SyncSender}
import io.flow.util.{Config, FlowEnvironment}
import javax.inject.{Inject, Singleton}
import net.codingwell.scalaguice.ScalaModule
import org.slf4j.LoggerFactory
import play.api.libs.json.jackson.PlayJsonModule
import play.api.libs.json._

import scala.collection.JavaConverters._

class RollbarModule extends AbstractModule with ScalaModule {
  override def configure(): Unit = {
    bind[Option[Rollbar]].toProvider[RollbarProvider]
    install(new FactoryModuleBuilder().build(classOf[RollbarLogger.Factory]))
    bind[RollbarLogger].toProvider[RollbarLoggerProvider]
  }
}

/**
  * We use a provider because we don't want to keep initializing a new Rollbar notifier
  * every time an instance of the logger is instantiated.
  *
  * Returns an optional Rollbar instance so a key isn't required in test and development mode.
  */
@Singleton
class RollbarProvider @Inject() (
  config: Config
) extends Provider[Option[Rollbar]] {
  val baseConfig = config.optionalString("rollbar.token") map { token =>

    val fingerprintGenerator = new FingerprintGenerator {
      override def from(data: Data): String = {
        Option(data.getCustom)
          .flatMap(custom => Option(custom.get(RollbarLogger.Keys.Fingerprint)))
          .map(_.toString)
          .orNull
      }
    }

    // give a jackson serializer to rollbar, instead of using rollbar's hand-rolled serializer
    val jacksonSerializer = new com.rollbar.notifier.sender.json.JsonSerializer {
      // plain old jackson serializer
      val mapper = new ObjectMapper()

      // de/serialize play-json types
      mapper.registerModule(PlayJsonModule)

      // serialize Rollbar JsonSerializable types
      mapper.registerModule(new SimpleModule() {
        addSerializer(
          classOf[com.rollbar.api.json.JsonSerializable],
          (value: com.rollbar.api.json.JsonSerializable, gen: JsonGenerator, serializers: SerializerProvider) => {
            serializers.defaultSerializeValue(value.asJson(), gen)
          }
        )
      })

      override def toJson(payload: Payload): String = {
        mapper.writeValueAsString(payload)
      }

      case class ResultObj(code: Int = -1, message: Option[String], uuid: Option[String])

      override def resultFrom(response: String): Result = {
        Json.parse(response).validate(Json.reads[ResultObj]) match {
          case JsSuccess(obj, _) =>
            val builder = new Result.Builder().code(obj.code)
            if (obj.code == 0)
              builder.body(obj.uuid.get)
            else
              builder.body(obj.message.get)
            builder.build()
          case _ =>
            new Result.Builder().code(-1).body("Didn't get an object").build()
        }
      }
    }

    ConfigBuilder.withAccessToken(token)
      .handleUncaughtErrors(true)
      .language("scala")
      .fingerPrintGenerator(fingerprintGenerator)
      .sender(
        new BufferedSender.Builder()
          .sender(
            new SyncSender.Builder()
              .accessToken(token)
              .jsonSerializer(jacksonSerializer)
              .build()
          )
          .build()
      )
      .environment(FlowEnvironment.Current.toString)
      .build()
  }

  override def get(): Option[Rollbar] = baseConfig.map(Rollbar.init)
}

// Allows RollbarLogger to be injected directly instead of creating one with the factory
@Singleton
class RollbarLoggerProvider @Inject() (
  factory: RollbarFactory
) extends Provider[RollbarLogger] {
  override def get(): RollbarLogger = factory.rollbar()
}

// Necessary evil to allow us to copy instances of RollbarLogger, letting us have
// nice methods like `withKeyValue`
@Singleton
class RollbarFactory @Inject()(
  rollbarProvider: Provider[Option[Rollbar]],
  config: Config
) extends RollbarLogger.Factory {
  @AssistedInject
  def rollbar(
    attributes: Map[String, JsValue] = Map.empty[String, JsValue],
    legacyMessage: Option[String] = None
  ): RollbarLogger = RollbarLogger(
    rollbarProvider.get(),
    attributes,
    legacyMessage,
    config
  )
}

object RollbarLogger {
  trait Factory {
    @AssistedInject
    def rollbar(
      attributes: Map[String, JsValue],
      legacyMessage: Option[String]
    ): RollbarLogger
  }

  object Keys {
    val Organization = "organization"
    val OrderNumber = "order_number"
    val Fingerprint = "fingerprint"
  }

  def convert(attributes: Map[String, JsValue]): java.util.Map[String, Object] =
    attributes.asJava.asInstanceOf[java.util.Map[String, Object]]
}

case class RollbarLogger @AssistedInject() (
  rollbar: Option[Rollbar],
  @Assisted attributes: Map[String, JsValue],
  @Assisted legacyMessage: Option[String],
  config: Config
) {
  import RollbarLogger._
  import net.logstash.logback.marker.Markers.appendEntries

  private val logger = LoggerFactory.getLogger("application")

  def withKeyValue[T: Writes](keyValue: (String, T)): RollbarLogger = withKeyValue(keyValue._1, keyValue._2)
  def withKeyValue[T: Writes](key: String, value: T): RollbarLogger = this.copy(attributes = attributes + (key -> Json.toJson(value)))

  def fingerprint(value: String) = withKeyValue(Keys.Fingerprint, value)
  def organization(value: String) = withKeyValue(Keys.Organization, value)
  def orderNumber(value: String) = withKeyValue(Keys.OrderNumber, value)

  // Used to preserve any existing messages tied to Sumo alerts until fully migrated to Rollbar
  def legacyMessage(value: String): RollbarLogger = this.copy(legacyMessage = Some(value))

  def warn(message: => String): Unit = warn(message, null)
  def error(message: => String): Unit = error(message, null)

  def warn(message: => String, error: => Throwable): Unit = {
    logger.warn(appendEntries(convert(attributes)), legacyMessage.getOrElse(message), error)
    rollbar.foreach(_.warning(error, convert(attributes), message))
  }

  def info(message: => String): Unit = {
    logger.info(appendEntries(convert(attributes)), legacyMessage.getOrElse(message))
    //not sending to rollbar to save quota
  }

  def error(message: => String, error: => Throwable): Unit = {
    logger.error(appendEntries(convert(attributes)), legacyMessage.getOrElse(message))
    rollbar.foreach(_.error(error, convert(attributes), message))
  }
}
