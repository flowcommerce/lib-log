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
  @Assisted legacyMessage: Option[String]
) {

  import RollbarLogger._

  private val logger = LoggerFactory.getLogger("application")

  def withKeyValue[T: Writes](keyValue: (String, T)): RollbarLogger = withKeyValue(keyValue._1, keyValue._2)
  def withKeyValue[T: Writes](key: String, value: T): RollbarLogger = this.copy(attributes = attributes + (key -> Json.toJson(value)))
  def fingerprint(value: String): RollbarLogger = withKeyValue(Keys.Fingerprint, value)
  def organization[T: Writes](value: T): RollbarLogger = withKeyValue(Keys.Organization, value)
  def orderNumber[T: Writes](value: T): RollbarLogger = withKeyValue(Keys.OrderNumber, value)
  def requestId[T: Writes](value: T): RollbarLogger = withKeyValue(Keys.RequestId, value)
  def itemNumber[T: Writes](value: T): RollbarLogger = withKeyValue(Keys.ItemNumber, value)
  def experienceKey[T: Writes](value: T): RollbarLogger = withKeyValue(Keys.ExperienceKey, value)

  def withKeyValues[T: Writes](keyValue: (String, Seq[T])): RollbarLogger = withKeyValues(keyValue._1, keyValue._2)

  /**
    * Accepts a list of values and writes them as individual attributes.
    * for example:
    *   withKeyValues("error", Seq("foo", "bar"))
    *
    * results in the attributes:
    *  - error_1: foo
    *  - error_2: bar
    */
  def withKeyValues[T: Writes](key: String, values: Seq[T]): RollbarLogger = {
    values.zipWithIndex.foldLeft(this) { case (l, pair) =>
      val value = pair._1
      val index = pair._2
      l.withKeyValue(s"${key}_" + (index + 1), value)
    }
  }

  def debug(message: => String): Unit = debug(message, null)
  def info(message: => String): Unit = info(message, null)
  def warn(message: => String): Unit = warn(message, null)
  def error(message: => String): Unit = error(message, null)

  def debug(message: => String, error: => Throwable): Unit = {
    logger.debug(appendEntries(convert(attributes)), legacyMessage.getOrElse(message), error)
    //not sending to rollbar to save quota
  }

  def info(message: => String, error: => Throwable): Unit = {
    logger.info(appendEntries(convert(attributes)), legacyMessage.getOrElse(message), error)
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
