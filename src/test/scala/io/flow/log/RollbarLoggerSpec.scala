package io.flow.log

import org.scalatest.matchers.must.Matchers
import org.scalatest.wordspec.AnyWordSpec

import scala.concurrent.duration._

class RollbarLoggerSpec extends AnyWordSpec with Matchers {

  "atMostEvery" should {

    "default to no time throttle" in {
      RollbarLogger.SimpleLogger.interval mustBe None
    }

    "allow the first call immediately" in {
      val logger = RollbarLogger.SimpleLogger.atMostEvery(1.second)
      logger.shouldLog mustBe true
    }

    "suppress a second call within the interval" in {
      val logger = RollbarLogger.SimpleLogger.atMostEvery(10.seconds)
      logger.shouldLog mustBe true
      logger.shouldLog mustBe false
    }

    "allow a call after the interval has elapsed" in {
      val logger = RollbarLogger.SimpleLogger.atMostEvery(50.millis)
      logger.shouldLog mustBe true
      Thread.sleep(100)
      logger.shouldLog mustBe true
    }

    "share throttle state across builder calls" in {
      val base = RollbarLogger.SimpleLogger.atMostEvery(10.seconds)
      val derived = base.withKeyValue("key", "value")
      (base.interval.get._2 eq derived.interval.get._2) mustBe true

      base.shouldLog mustBe true
      derived.shouldLog mustBe false
    }

    "combine correctly with withFrequency" in {
      val logger = RollbarLogger.SimpleLogger
        .withFrequency(100)
        .atMostEvery(1.second)
      logger.frequency mustBe 100L
      logger.interval.map(_._1) mustBe Some(1.second)
    }
  }
}
