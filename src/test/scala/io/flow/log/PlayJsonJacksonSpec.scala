package io.flow.log

import org.scalatest.matchers.must.Matchers
import org.scalatest.wordspec.AnyWordSpec
import play.api.libs.json._

case class Foo(s: String, i: Int, b: Boolean)
case class Bar(f: Option[Foo])

class PlayJsonJacksonSpec extends AnyWordSpec with Matchers {
  "Play-json" when {
    "basic reading and writing" should {
      "return the same object as the input" in {
        val jsString = """{"foo":{"str":"hello","int":42,"bool":true}}"""
        val json = Json.parse(jsString)
        Json.stringify(json) mustBe jsString
      }

      "traverse the js path" in {
        val json = Json.parse("""{"foo":{"str":"hello","int":42,"bool":true}}""")

        (json \ "foo" \ "str").as[String] mustBe "hello"
        (json \ "foo" \ "str").asOpt[String] mustBe Some("hello")
        (json \ "foo" \ "boo").asOpt[String] mustBe None

        val values = json \\ "int"
        values.size mustBe 1
        values.toList(0).as[Int] mustBe 42
      }

      "validate" in {
        val json = Json.parse("""{"foo":{"str":"hello","int":42,"bool":true}}""")

        (json \ "foo" \ "str").validate[String] mustBe JsSuccess("hello")
        (json \ "foo" \ "boo").validate[String] mustBe a[JsError]
        (json \ "foo" \ "str").validate[Int] mustBe a[JsError]
      }

      "construct json manually" in {
        val json = Json.obj(
          "foo" -> Json.obj(
            "str" -> "hello",
            "int" -> 42,
            "bool" -> true,
            "missing" -> null
          ),
        )

        val bar = (json \ "foo").as[JsObject]
        (bar \ "str").as[String] mustBe "hello"
        (bar \ "int").as[Int] mustBe 42
        (bar \ "bool").as[Boolean] mustBe true
        (bar \ "missing").get mustBe JsNull
        (bar \ "missing") mustBe a[JsDefined]
        a[NoSuchElementException] must be thrownBy (bar \ "undefined").get
        (bar \ "undefined") mustBe a[JsUndefined]
      }
    }

    "serializing objects" should {
      "support explicit Writes" in {
        implicit val fooWrites: Writes[Foo] = { foo =>
          Json.obj(
            "str" -> JsString(foo.s),
            "int" -> JsNumber(foo.i),
            "bool" -> JsBoolean(foo.b),
          )
        }

        implicit val barWrites: Writes[Bar] = { bar =>
          Json.obj(
            "foo" -> bar.f.fold(JsNull: JsValue)(Json.toJson)
          )
        }

        Json.stringify(Json.toJson(Bar(None))) mustBe """{"foo":null}"""

        val json = Json.toJson(Bar(Some(Foo("hello", 42, true))))
        val str = Json.stringify(json)
        str mustBe """{"foo":{"str":"hello","int":42,"bool":true}}"""
      }

      "support automatic conversion" in {
        implicit val fooWrites = Json.writes[Foo]
        implicit val barWrites = Json.writes[Bar]

        Json.stringify(Json.toJson(Bar(None))) mustBe "{}"

        val json = Json.toJson(Bar(Some(Foo("hello", 42, true))))
        val str = Json.stringify(json)
        str mustBe """{"f":{"s":"hello","i":42,"b":true}}"""
      }
    }

    "deserializing objects" should {

      "support explicit Reads" in {
        implicit val fooReads: Reads[Foo] = for {
          s <- (__ \ "str").read[String]
          i <- (__ \ "int").read[Int]
          b <- (__ \ "bool").read[Boolean]
        } yield Foo(s, i, b)

        implicit val barReads: Reads[Bar] = for {
          foo <- (__ \ "foo").readNullable[Foo]
        } yield Bar(foo)

        Json.parse("{}").as[Bar] mustBe Bar(None)

        val json = Json.parse("""{"foo":{"str":"hello","int":42,"bool":true}}""")
        json.as[Bar] mustBe Bar(Some(Foo("hello", 42, true)))
      }

      "support automatic conversion" in {
        implicit val fooReads = Json.reads[Foo]
        implicit val barReads = Json.reads[Bar]

        Json.parse("{}").as[Bar] mustBe Bar(None)

        val json = Json.parse("""{"f":{"s":"hello","i":42,"b":true}}""")
        json.as[Bar] mustBe Bar(Some(Foo("hello", 42, true)))
      }
    }
  }

}
