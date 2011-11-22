/**
 * Copyright (C) 2009-2011 Typesafe Inc. <http://www.typesafe.com>
 */
package akka.testkit

import akka.actor.ActorSystem
import akka.actor.ExtensionKey
import akka.actor.Extension
import akka.actor.ActorSystemImpl
import com.typesafe.config.Config
import com.typesafe.config.ConfigFactory
import com.typesafe.config.ConfigParseOptions
import com.typesafe.config.ConfigRoot
import akka.util.Duration
import java.util.concurrent.TimeUnit.MILLISECONDS

object TestKitExtension extends ExtensionKey[TestKitExtension] {
  def apply(system: ActorSystem): TestKitExtension = {
    if (!system.hasExtension(TestKitExtension)) {
      system.registerExtension(new TestKitExtension)
    }
    system.extension(TestKitExtension)
  }
}

class TestKitExtension extends Extension[TestKitExtension] {
  private var _settings: Settings = _

  def init(_system: ActorSystemImpl): ExtensionKey[TestKitExtension] = {
    _settings = new Settings(_system.applicationConfig)
    TestKitExtension
  }

  def settings: Settings = _settings

  class Settings(cfg: Config) {
    private def referenceConfig: Config =
      ConfigFactory.parseResource(classOf[ActorSystem], "/akka-testkit-reference.conf",
        ConfigParseOptions.defaults.setAllowMissing(false))
    val config: ConfigRoot = ConfigFactory.emptyRoot("akka-testkit").withFallback(cfg).withFallback(referenceConfig).resolve()

    import config._

    val TestTimeFactor = Duration.Dilation(getDouble("akka.test.timefactor"))
    val SingleExpectDefaultTimeout = Duration(getMilliseconds("akka.test.single-expect-default"), MILLISECONDS)
    val TestEventFilterLeeway = Duration(getMilliseconds("akka.test.filter-leeway"), MILLISECONDS)

  }

}