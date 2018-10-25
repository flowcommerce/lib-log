package io.flow.log

import com.google.inject.assistedinject.{Assisted, AssistedInject}
import com.rollbar.notifier.Rollbar
import io.flow.util.Config
import net.logstash.logback.marker.Markers.appendEntries
import org.slf4j.LoggerFactory
import play.api.libs.json.{JsValue, Json, Writes}
import scala.collection.JavaConverters._

object RollbarLogger {
  trait Factory {

    @AssistedInject
    def rollbar(attributes: Map[String, JsValue], legacyMessage: Option[String]): RollbarLogger
  }

  object Keys {
    val RequestId = "request_id"
    val Organization = "organization"
    val OrderNumber = "order_number"
    val Fingerprint = "fingerprint"
    val ItemNumber = "item_number"
    val ExperienceKey = "experience_key"
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

  private val logger = LoggerFactory.getLogger("application")

  def withKeyValue[T: Writes](keyValue: (String, T)): RollbarLogger = withKeyValue(keyValue._1, keyValue._2)
  def withKeyValue[T: Writes](key: String, value: T): RollbarLogger = this.copy(attributes = attributes + (key -> Json.toJson(value)))
  def fingerprint(value: String): RollbarLogger = withKeyValue(Keys.Fingerprint, value)
  def organization(value: String): RollbarLogger = withKeyValue(Keys.Organization, value)
  def orderNumber(value: String): RollbarLogger = withKeyValue(Keys.OrderNumber, value)
  def requestId(value: String): RollbarLogger = withKeyValue(Keys.RequestId, value)
  def itemNumber(value: String): RollbarLogger = withKeyValue(Keys.ItemNumber, value)
  def experienceKey(value: String): RollbarLogger = withKeyValue(Keys.ExperienceKey, value)

  def debug(message: => String): Unit = debug(message, null)
  def info(message: => String): Unit = info(message, null)
  def warn(message: => String): Unit = warn(message, null)
  def error(message: => String): Unit = error(message, null)

  def debug(message: => String, error: => Throwable): Unit = {
    logger.debug(appendEntries(convert(attributes)), legacyMessage.getOrElse(message), error)
    //not sending to rollbar to save quota
  }

  def info(message: => String, error: => Throwable): Unit = {
    logger.debug(appendEntries(convert(attributes)), legacyMessage.getOrElse(message), error)
    //not sending to rollbar to save quota
  }

  def warn(message: => String, error: => Throwable): Unit = {
    logger.warn(appendEntries(convert(attributes)), legacyMessage.getOrElse(message), error)
    rollbar.foreach(_.warning(error, convert(attributes), message))
  }

  def error(message: => String, error: => Throwable): Unit = {
    logger.error(appendEntries(convert(attributes)), legacyMessage.getOrElse(message), error)
    rollbar.foreach(_.error(error, convert(attributes), message))
  }

}
