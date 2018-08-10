package io.flow.log

import com.google.gson.{GsonBuilder, JsonParser}
import com.google.inject.assistedinject.{Assisted, AssistedInject, FactoryModuleBuilder}
import com.google.inject.{AbstractModule, Provider}
import com.rollbar.api.payload.data.Data
import com.rollbar.notifier.Rollbar
import com.rollbar.notifier.config.ConfigBuilder
import com.rollbar.notifier.fingerprint.FingerprintGenerator
import io.flow.util.{Config, FlowEnvironment}
import javax.inject.{Inject, Singleton}
import net.codingwell.scalaguice.ScalaModule
import org.slf4j.LoggerFactory
import play.api.libs.json.{JsValue, Json, Writes}

import scala.collection.JavaConverters._
import scala.util.Try

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

    ConfigBuilder.withAccessToken(token)
      .handleUncaughtErrors(true)
      .language("scala")
      .fingerPrintGenerator(fingerprintGenerator)
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
  val gson = new GsonBuilder().disableHtmlEscaping().create()
  val jsonParser = new JsonParser()

  // TODO: See if there's a less expensive way to perform this conversion
  def convert(attributes: Map[String, JsValue]): java.util.Map[String, Object] = (attributes map { case (key, value) =>
    key -> gson.toJson(jsonParser.parse(Json.toJson(value).toString)).asInstanceOf[Object]
  }).asJava
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
    logger.info(legacyMessage.getOrElse(message))
    //not sending to rollbar to save quota
  }

  def error(message: => String, error: => Throwable): Unit = {
    logger.error(appendEntries(convert(attributes)), legacyMessage.getOrElse(message))
    rollbar.foreach(_.error(error, convert(attributes), message))
  }
}
