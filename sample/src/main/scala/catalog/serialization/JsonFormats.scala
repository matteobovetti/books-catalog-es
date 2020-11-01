package catalog.serialization

import catalog.authors._
import catalog.books._
import catalog.booksCards._
import es4kafka.serialization.CommonJsonFormats
import spray.json._

trait JsonFormats extends CommonJsonFormats {
  // book
  implicit val BookFormat: RootJsonFormat[Book] = jsonFormat3(Book.apply)

  // book commands
  implicit val CreateBookFormat: RootJsonFormat[CreateBook] = jsonFormat2(CreateBook)
  implicit val SetBookAuthorFormat: RootJsonFormat[SetBookAuthor] = jsonFormat2(SetBookAuthor)
  implicit object BookCommandFormat extends RootJsonFormat[BookCommand] {
    def write(value: BookCommand): JsValue = {
      val fields = value match {
        case e: CreateBook => e.toJson.asJsObject.fields
        case e: SetBookAuthor => e.toJson.asJsObject.fields
      }
      val extendedFields = fields ++ Seq(
        "_type" -> JsString(value.className),
      )
      JsObject(extendedFields)
    }

    override def read(json: JsValue): BookCommand = {
      // TODO Why nameOfType doesn't work
      // import com.github.dwickern.macros.NameOf._

      json match {
        case jsObj: JsObject => jsObj.fields.getOrElse("_type", JsNull) match {
          case JsString("CreateBook") => jsObj.convertTo[CreateBook]
          case JsString("SetBookAuthor") => jsObj.convertTo[SetBookAuthor]
          case cmdType => throw DeserializationException(s"Command type not valid: $cmdType")
        }
        case _ => throw DeserializationException("Expected json object")
      }
    }
  }

  // book events
  implicit val bookCreatedFormat: RootJsonFormat[BookCreated] = jsonFormat2(BookCreated)
  implicit val bookAuthorSetFormat: RootJsonFormat[BookAuthorSet] = jsonFormat2(BookAuthorSet)
  implicit object BookEventFormat extends RootJsonFormat[BookEvent] {
    def write(value: BookEvent): JsValue = {
      val fields = value match {
        case e: BookCreated => e.toJson.asJsObject.fields
        case e: BookAuthorSet => e.toJson.asJsObject.fields
      }
      val extendedFields = fields ++ Seq(
        "_type" -> JsString(value.className),
        "_isError" -> JsBoolean(value.isError),
      )
      JsObject(extendedFields)
    }

    override def read(json: JsValue): BookEvent = {
      // TODO Why nameOfType doesn't work
      // import com.github.dwickern.macros.NameOf._

      json match {
        case jsObj: JsObject => jsObj.fields.getOrElse("_type", JsNull) match {
          case JsString("BookCreated") => jsObj.convertTo[BookCreated]
          case JsString("BookAuthorSet") => jsObj.convertTo[BookAuthorSet]
          case evType => throw DeserializationException(s"Event type not valid: $evType")
        }
        case _ => throw DeserializationException("Expected json object")
      }
    }
  }

  // author
  implicit val AuthorStateFormat: RootJsonFormat[AuthorStates.AuthorState] = new EnumJsonConverter(AuthorStates)
  implicit val AuthorFormat: RootJsonFormat[Author] = jsonFormat4(Author.apply)

  // author commands
  implicit val CreateAuthorFormat: RootJsonFormat[CreateAuthor] = jsonFormat3(CreateAuthor)
  implicit val UpdateAuthorFormat: RootJsonFormat[UpdateAuthor] = jsonFormat3(UpdateAuthor)
  implicit val DeleteAuthorFormat: RootJsonFormat[DeleteAuthor] = jsonFormat1(DeleteAuthor)
  implicit object AuthorCommandFormat extends RootJsonFormat[AuthorCommand] {
    def write(value: AuthorCommand): JsValue = {
      val fields = value match {
        case e: CreateAuthor => e.toJson.asJsObject.fields
        case e: UpdateAuthor => e.toJson.asJsObject.fields
        case e: DeleteAuthor => e.toJson.asJsObject.fields
      }
      val extendedFields = fields ++ Seq(
        "_type" -> JsString(value.className),
      )
      JsObject(extendedFields)
    }

    override def read(json: JsValue): AuthorCommand = {
      // TODO Why nameOfType doesn't work
      // import com.github.dwickern.macros.NameOf._

      json match {
        case jsObj: JsObject => jsObj.fields.getOrElse("_type", JsNull) match {
          case JsString("CreateAuthor") => jsObj.convertTo[CreateAuthor]
          case JsString("UpdateAuthor") => jsObj.convertTo[UpdateAuthor]
          case JsString("DeleteAuthor") => jsObj.convertTo[DeleteAuthor]
          case cmdType => throw DeserializationException(s"Command type not valid: $cmdType")
        }
        case _ => throw DeserializationException("Expected json object")
      }
    }
  }

  // author events
  implicit val authorCreatedFormat: RootJsonFormat[AuthorCreated] = jsonFormat3(AuthorCreated)
  implicit val authorUpdateFormat: RootJsonFormat[AuthorUpdated] = jsonFormat3(AuthorUpdated)
  implicit val authorDeletedFormat: RootJsonFormat[AuthorDeleted] = jsonFormat1(AuthorDeleted)
  implicit val authorErrorFormat: RootJsonFormat[AuthorError] = jsonFormat2(AuthorError)
  implicit object AuthorEventFormat extends RootJsonFormat[AuthorEvent] {
    def write(value: AuthorEvent): JsValue = {
      val fields = value match {
        case e: AuthorCreated => e.toJson.asJsObject.fields
        case e: AuthorUpdated => e.toJson.asJsObject.fields
        case e: AuthorDeleted => e.toJson.asJsObject.fields
        case e: AuthorError => e.toJson.asJsObject.fields
      }
      val extendedFields = fields ++ Seq(
        "_type" -> JsString(value.className),
        "_isError" -> JsBoolean(value.isError),
      )
      JsObject(extendedFields)
    }

    override def read(json: JsValue): AuthorEvent = {
      // TODO Why nameOfType doesn't work
      // import com.github.dwickern.macros.NameOf._

      json match {
        case jsObj: JsObject => jsObj.fields.getOrElse("_type", JsNull) match {
          case JsString("AuthorCreated") => jsObj.convertTo[AuthorCreated]
          case JsString("AuthorUpdated") => jsObj.convertTo[AuthorUpdated]
          case JsString("AuthorDeleted") => jsObj.convertTo[AuthorDeleted]
          case JsString("AuthorError") => jsObj.convertTo[AuthorError]
          case evType => throw DeserializationException(s"Event type not valid: $evType")
        }
        case _ => throw DeserializationException("Expected json object")
      }
    }
  }

  // book card
  implicit val BookCardFormat: RootJsonFormat[BookCard] = jsonFormat2(BookCard)
}