package io.flow.log

import cats.data.NonEmptyChain
import com.google.inject.assistedinject.{Assisted, AssistedInject}
import com.rollbar.notifier.Rollbar
import io.flow.util.FlowEnvironment
import net.logstash.logback.marker.Markers.appendEntries
import org.slf4j.LoggerFactory
import play.api.libs.json.{JsValue, Json, Writes}

import scala.jdk.CollectionConverters._
import scala.util.control.NonFatal
import scala.util.{Random, Try}

object RollbarLogger {

  /** Simple [[RollbarLogger]] that does not log to Rollbar. Useful for tests requiring a [[RollbarLogger]].
    */
  val SimpleLogger: RollbarLogger =
    RollbarLogger(rollbar = None, attributes = Map.empty, legacyMessage = None, shouldSendToRollbar = false)

  private def rollbarToken(): String = {
    sys.env
      .get("ROLLBAR_TOKEN")
      .getOrElse {
        sys.error("missing env variable: ROLLBAR_TOKEN")
      }
  }

  lazy val Default: RollbarLogger = {
    if (FlowEnvironment.Current == FlowEnvironment.Production) {
      Try {
        val rollbar = RollbarProvider.rollbar(rollbarToken())
        RollbarLogger.SimpleLogger.copy(
          rollbar = Some(rollbar),
          shouldSendToRollbar = true,
        )
      }.recover { case NonFatal(e) =>
        Console.err.println(s"WARN Failed to load Rollbar logger, using simple logger: $e")
        RollbarLogger.SimpleLogger
      }.get
    } else {
      RollbarLogger.SimpleLogger
    }
  }

  trait Factory {
    @AssistedInject
    def rollbar(
      attributes: Map[String, JsValue],
      legacyMessage: Option[String],
      shouldSendToRollbar: Boolean,
      frequency: Long,
    ): RollbarLogger
  }

  object Keys {
    val RequestId = "request_id"
    val Organization = "organization"
    val ChannelId = "channel_id"
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
  @Assisted shouldSendToRollbar: Boolean = true,
  @Assisted frequency: Long = 1L,
) {

  private[this] val MaxValuesToWrite = 10

  import RollbarLogger._

  private val logger = LoggerFactory.getLogger("application")

  /** Log once per frequency. For instance, 100 means that the message will be logged once every 100 calls on average.
    */
  def withFrequency(frequency: Long): RollbarLogger = this.copy(frequency = frequency)

  def withKeyValue[T: Writes](keyValue: (String, T)): RollbarLogger = withKeyValue(keyValue._1, keyValue._2)
  def withKeyValue[T: Writes](key: String, value: T): RollbarLogger =
    this.copy(attributes = attributes + (key -> Json.toJson(value)))
  def fingerprint(value: String): RollbarLogger = withKeyValue(Keys.Fingerprint, value)
  def organization(value: String): RollbarLogger = withKeyValue(Keys.Organization, value)
  def channelId(value: String): RollbarLogger = withKeyValue(Keys.ChannelId, value)
  def orderNumber(value: String): RollbarLogger = withKeyValue(Keys.OrderNumber, value)
  def orderNumber(value: Option[String]): RollbarLogger = withKeyValue(Keys.OrderNumber, value)
  def requestId(value: String): RollbarLogger = withKeyValue(Keys.RequestId, value)
  def itemNumber(value: String): RollbarLogger = withKeyValue(Keys.ItemNumber, value)
  def experienceKey(value: String): RollbarLogger = withKeyValue(Keys.ExperienceKey, value)

  /** Use for warnings or errors that:
    *   - are very high volume
    *   - should be recorded for audit purposes but no action needs to be taken
    *
    * Structured errors will still be sent to Sumo.
    */
  def withSendToRollbar(sendToRollbar: Boolean): RollbarLogger = this.copy(shouldSendToRollbar = sendToRollbar)

  def withKeyValues[T: Writes](keyValue: (String, Seq[T])): RollbarLogger = withKeyValues(keyValue._1, keyValue._2)

  /** Accepts a list of values and writes them as individual attributes. for example: withKeyValues("error", Seq("foo",
    * "bar"))
    *
    * results in the attributes:
    *   - error_1: foo
    *   - error_2: bar
    */
  def withKeyValues[T: Writes](key: String, values: Seq[T])(implicit
    maxValues: Int = MaxValuesToWrite,
  ): RollbarLogger = {
    val logger = values.take(maxValues).zipWithIndex.foldLeft(this) { case (l, pair) =>
      val value = pair._1
      val index = pair._2
      l.withKeyValue(s"${key}_" + (index + 1), value)
    }
    if (values.length > maxValues) {
      // Include the total number of entries
      logger.withKeyValue(s"${key}_number", values.length)
    } else {
      logger
    }
  }

  def withKeyValues[T: Writes](key: String, values: NonEmptyChain[T]): RollbarLogger =
    withKeyValues(key, values.toNonEmptyList.toList)

  def debug(message: => String): Unit = debug(message, null)
  def info(message: => String): Unit = info(message, null)
  def warn(message: => String): Unit = warn(message, null)
  def error(message: => String): Unit = error(message, null)

  def debug(message: => String, error: => Throwable): Unit =
    if (shouldLog && logger.isDebugEnabled) {
      logger.debug(appendEntries(convert(attributes)), legacyMessage.getOrElse(message), error)
      // not sending to rollbar to save quota
    }

  def info(message: => String, error: => Throwable): Unit =
    if (shouldLog && logger.isInfoEnabled) {
      logger.info(appendEntries(convert(attributes)), legacyMessage.getOrElse(message), error)
      // not sending to rollbar to save quota
    }

  def warn(message: => String, error: => Throwable): Unit =
    if (shouldLog) {
      if (logger.isWarnEnabled)
        logger.warn(appendEntries(convert(attributes)), legacyMessage.getOrElse(message), error)
      if (shouldSendToRollbar)
        rollbar.foreach(_.warning(error, convert(attributes), message))
    }

  def error(message: => String, error: => Throwable): Unit =
    if (shouldLog) {
      if (logger.isErrorEnabled)
        logger.error(appendEntries(convert(attributes)), legacyMessage.getOrElse(message), error)
      if (shouldSendToRollbar)
        rollbar.foreach(_.error(error, convert(attributes), message))
    }

  private def shouldLog: Boolean =
    frequency == 1L || (Random.nextInt() % frequency == 0)

}
