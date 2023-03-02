package io.flow.log

import com.fasterxml.jackson.core.JsonFactory
import com.fasterxml.jackson.databind.ObjectMapper
import net.logstash.logback.decorate.JsonFactoryDecorator
import play.api.libs.json.JsonParserSettings
import play.api.libs.json.jackson.PlayJsonMapperModule

// allows you to pass play-json types to logstash-logback-encoder
class SerializePlayJson extends JsonFactoryDecorator {
  override def decorate(factory: JsonFactory): JsonFactory = {
    /*
    https://github.com/logstash/logstash-logback-encoder/issues/345#issuecomment-512585630
    "It is safe to downcast factory.getCodec() to ObjectMapper as long as you have not configured the encoder to use a non-JSON output format."
     */
    factory.getCodec.asInstanceOf[ObjectMapper].registerModule(new PlayJsonMapperModule(JsonParserSettings()))
    factory
  }
}
