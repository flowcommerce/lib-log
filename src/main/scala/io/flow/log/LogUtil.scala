package io.flow.log

import javax.inject.Inject

import scala.concurrent.{ExecutionContext, Future}
import scala.util.Random

class LogUtil @Inject() (logger: RollbarLogger) {

  import RollbarLogger.Keys

  /**
    * @param frequency Log once per frequency. For instance, 100 means that the duration will be logged once every 100
    *                  calls on average.
    */
  def duration[T](
    info: String,
    fingerprint: String,
    organizationId: String,
    itemNumber: Option[String] = None,
    experienceKey: Option[String] = None,
    orderNumber: Option[String] = None,
    requestId: Option[String] = None,
    data: Option[Map[String, String]] = None,
    frequency: Long = 1L,
  )(f: => T): T = {
    if (frequency == 1L || Random.nextLong() % frequency == 0) {
      val start = System.currentTimeMillis()
      try {
        f
      } finally {
        val end = System.currentTimeMillis()

        logger
          .fingerprint(fingerprint)
          .organization(organizationId)
          .withKeyValue(Keys.RequestId, requestId)
          .withKeyValue(Keys.ExperienceKey, experienceKey)
          .withKeyValue(Keys.ItemNumber, itemNumber)
          .withKeyValue(Keys.OrderNumber, orderNumber)
          .withKeyValue("data", data)
          .withKeyValue("duration", end - start)
          .info(info)
      }
    } else
      f
  }

  /**
    * @param frequency Log once per frequency. For instance, 100 means that the duration will be logged once every 100
    *                  calls on average.
    */
  def durationF[T](
    info: String,
    fingerprint: String,
    organizationId: String,
    itemNumber: Option[String] = None,
    experienceKey: Option[String] = None,
    orderNumber: Option[String] = None,
    requestId: Option[String] = None,
    data: Option[Map[String, String]] = None,
    frequency: Long = 1L,
  )(f: => Future[T])(implicit ec: ExecutionContext): Future[T] = {
    if (frequency == 1L || Random.nextLong() % frequency == 0) {
      val start = System.currentTimeMillis()
      f.onComplete { _ =>
        val end = System.currentTimeMillis()

        logger
          .fingerprint(fingerprint)
          .organization(organizationId)
          .withKeyValue(Keys.RequestId, requestId)
          .withKeyValue(Keys.ExperienceKey, experienceKey)
          .withKeyValue(Keys.ItemNumber, itemNumber)
          .withKeyValue(Keys.OrderNumber, orderNumber)
          .withKeyValue("data", data)
          .withKeyValue("duration", end - start)
          .info(info)
      }
      f
    } else
      f
  }

}
