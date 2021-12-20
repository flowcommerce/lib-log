package io.flow.log

import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.databind.module.SimpleModule
import com.fasterxml.jackson.databind.{ObjectMapper, SerializerProvider}
import com.google.inject.assistedinject.{AssistedInject, FactoryModuleBuilder}
import com.google.inject.{AbstractModule, Provider}
import com.rollbar.api.payload.Payload
import com.rollbar.api.payload.data.Data
import com.rollbar.notifier.Rollbar
import com.rollbar.notifier.config.ConfigBuilder
import com.rollbar.notifier.fingerprint.FingerprintGenerator
import com.rollbar.notifier.sender.result.Result
import io.flow.util.{Config, FlowEnvironment}

import javax.inject.{Inject, Singleton}
import net.codingwell.scalaguice.ScalaModule
import play.api.libs.json._
import play.api.libs.json.jackson.PlayJsonModule

import scala.util.Try

class RollbarModule extends AbstractModule with ScalaModule {
  override def configure(): Unit = {
    bind[Option[Rollbar]].toProvider[RollbarProvider]
    install(new FactoryModuleBuilder().build(classOf[RollbarLogger.Factory]))
    bind[RollbarLogger].toProvider[RollbarLoggerProvider]
    ()
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
  override def get(): Option[Rollbar] = config.optionalString("rollbar.token").map(RollbarProvider.rollbar)
}

// Allows RollbarLogger to be injected directly instead of creating one with the factory
@Singleton
class RollbarLoggerProvider @Inject() (
  factory: RollbarFactory
) extends Provider[RollbarLogger] {
  private[this] lazy val logger = factory.rollbar()
  override def get(): RollbarLogger = logger
}

// Necessary evil to allow us to copy instances of RollbarLogger, letting us have
// nice methods like `withKeyValue`
@Singleton
class RollbarFactory @Inject()(
  rollbarProvider: Provider[Option[Rollbar]]
) extends RollbarLogger.Factory {
  @AssistedInject
  def rollbar(
    attributes: Map[String, JsValue] = Map.empty[String, JsValue],
    legacyMessage: Option[String] = None,
    shouldSendToRollbar: Boolean = true,
    frequency: Long = 1L
  ): RollbarLogger = RollbarLogger(
    rollbarProvider.get(),
    attributes,
    legacyMessage,
    shouldSendToRollbar,
    frequency
  )
}

// Common method to get rollbar Config
object RollbarProvider {
  def logger(
    token: String,
    attributes: Map[String, JsValue] = Map.empty[String, JsValue],
    legacyMessage: Option[String] = None
  ): RollbarLogger = {
    val rb = Some(rollbar(token))
    RollbarLogger(rb, attributes, legacyMessage)
  }

  def rollbar(token: String): Rollbar = {
    val baseConfig = RollbarProvider.baseConfig(token)
    Rollbar.init(baseConfig)
  }

  def baseConfig(token: String): com.rollbar.notifier.config.Config = {
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
      mapper.registerModule(new PlayJsonModule(JsonParserSettings()))

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

      override def resultFrom(response: String): Result = {
        val resultOpt = for {
          obj <- Json.parse(response).validate[JsObject]
          err <- (obj \ "err").validate[Int]
          content <- {
            if (err == 0)
              (obj \ "result" \ "uuid").validate[String]
            else
              (obj \ "message").validate[String]
          }
        } yield new Result.Builder().code(err).body(content).build

        resultOpt match {
          case JsSuccess(res, _) => res
          case JsError(errors) => new Result.Builder().code(-1).body(
            JsError.toJson(errors).toString
          ).build
        }
      }
    }

    ConfigBuilder
      .withAccessToken(token)
      .handleUncaughtErrors(true)
      .language("scala")
      .fingerPrintGenerator(fingerprintGenerator)
      .jsonSerializer(jacksonSerializer)
      .environment(FlowEnvironment.Current.toString)
      .build()
  }
}
