package io.flow.log

import com.google.inject.assistedinject.{AssistedInject, FactoryModuleBuilder}
import com.google.inject.{AbstractModule, Provider}
import com.rollbar.api.payload.data.Data
import com.rollbar.notifier.Rollbar
import com.rollbar.notifier.config.ConfigBuilder
import com.rollbar.notifier.fingerprint.FingerprintGenerator
import com.rollbar.notifier.sender.{BufferedSender, SyncSender}
import io.flow.util.{Config, FlowEnvironment}
import javax.inject.{Inject, Singleton}
import net.codingwell.scalaguice.ScalaModule


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
  override def get(): RollbarLogger = factory.rollbar()
}

// Necessary evil to allow us to copy instances of RollbarLogger, letting us have
// nice methods like `withKeyValue`
@Singleton
class RollbarFactory @Inject()(
  rollbarProvider: Provider[Option[Rollbar]]
) extends RollbarLogger.Factory {
  @AssistedInject
  def rollbar(
    attributes: Map[String, AnyRef] = Map.empty[String, AnyRef],
    legacyMessage: Option[String] = None
  ): RollbarLogger = RollbarLogger(
    rollbarProvider.get(),
    attributes,
    legacyMessage
  )
}

// Common method to get rollbar Config
object RollbarProvider {
  def logger(
    token: String,
    attributes: Map[String, AnyRef] = Map.empty[String, AnyRef],
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

    ConfigBuilder.withAccessToken(token)
      .handleUncaughtErrors(true)
      .language("scala")
      .fingerPrintGenerator(fingerprintGenerator)
      .sender(
        new BufferedSender.Builder()
          .sender(
            new SyncSender.Builder()
              .accessToken(token)
              .build()
          )
          .build()
      )
      .environment(FlowEnvironment.Current.toString)
      .build()
  }
}