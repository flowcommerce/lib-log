package io.flow.log

import javax.inject.Inject

class LogUtil @Inject() (logger: RollbarLogger) {

  import RollbarLogger.Keys

  def duration[T](
    info: String,
    fingerprint: String,
    organizationId: String,
    itemNumber: Option[String] = None,
    experienceKey: Option[String] = None,
    orderNumber: Option[String] = None,
    requestId: Option[String] = None,
    data: Option[Map[String, String]] = None
  )(f: => T): T = {
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
  }

}
