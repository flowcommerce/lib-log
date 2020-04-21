package io.flow.log

import com.google.inject.{AbstractModule, Guice}
import io.flow.util.Config
import org.scalatest.wordspec.AnyWordSpec
import net.codingwell.scalaguice.InjectorExtensions._
import net.codingwell.scalaguice.ScalaModule
import org.scalatest.matchers.should.Matchers

class RollbarModuleSpec extends AnyWordSpec with Matchers {

  private class MockConfigModule extends AbstractModule with ScalaModule {
    override def configure(): Unit =
      bind[Config].toInstance(new Config {
        override protected def get(name: String): Option[String] = None
        override def optionalList(name: String): Option[Seq[String]] = None
        override def optionalMap(name: String): Option[Map[String, Seq[String]]] = None
      })
  }

  "RollbarModule" should {
    "inject rollbar" in {
      val inj = Guice.createInjector(new RollbarModule, new MockConfigModule)
      noException should be thrownBy inj.instance[RollbarLogger]
    }
  }

}
