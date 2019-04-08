package io.flow.log

import com.fasterxml.jackson.databind.MappingJsonFactory
import com.github.ghik.silencer.silent
import net.logstash.logback.decorate.JsonFactoryDecorator
import play.api.libs.json.jackson.PlayJsonModule

// allows you to pass play-json types to logstash-logback-encoder
class SerializePlayJson extends JsonFactoryDecorator {
  override def decorate(factory: MappingJsonFactory): MappingJsonFactory = {
    factory.getCodec.registerModule(PlayJsonModule): @silent //TODO: please remove once PlayJsonModule(jsonParsersettings) is accessible outside of the jackson package
    factory
  }
}
