package io.flow.log

import com.fasterxml.jackson.databind.MappingJsonFactory
import net.logstash.logback.decorate.JsonFactoryDecorator
import play.api.libs.json.jackson.PlayJsonModule

// allows you to pass play-json types to logstash-logback-encoder
class SerializePlayJson extends JsonFactoryDecorator {
  override def decorate(factory: MappingJsonFactory): MappingJsonFactory = {
    @silent
    factory.getCodec.registerModule(PlayJsonModule)
    factory
  }
}
