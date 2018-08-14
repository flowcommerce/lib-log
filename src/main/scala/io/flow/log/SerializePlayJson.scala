package io.flow.log

import com.fasterxml.jackson.databind.MappingJsonFactory
import net.logstash.logback.decorate.JsonFactoryDecorator
import play.api.libs.json.jackson.PlayJsonModule

class SerializePlayJson extends JsonFactoryDecorator {
  override def decorate(factory: MappingJsonFactory): MappingJsonFactory = {
    factory.getCodec.registerModule(PlayJsonModule)
    factory
  }
}
