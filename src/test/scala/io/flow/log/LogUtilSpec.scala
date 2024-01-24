package io.flow.log

import java.util.concurrent.atomic.AtomicInteger

import org.scalatest.matchers.must.Matchers
import org.scalatest.wordspec.AnyWordSpec

import scala.concurrent.{Await, Future}
import scala.concurrent.duration._

class LogUtilSpec extends AnyWordSpec with Matchers {

  implicit val ec: scala.concurrent.ExecutionContext = scala.concurrent.ExecutionContext.global

  "LogUtil" when {
    val logUtil = new LogUtil(RollbarLogger.SimpleLogger)

    "log duration" should {
      "only execute function once" in {
        val callCount = new AtomicInteger(0)
        def f = {
          callCount.incrementAndGet()
        }
        logUtil.duration(message = "test", fingerprint = "test", organizationId = "test")(f)
        callCount.get must be(1)
      }
    }

    "log duration future" should {
      "only execute future once" in {
        val callCount = new AtomicInteger(0)
        def f = Future {
          callCount.incrementAndGet()
        }
        Await.result(
          logUtil.durationF(message = "test", fingerprint = "test", organizationId = "test")(f),
          10.millis
        )
        callCount.get must be(1)
      }
    }
  }
}
