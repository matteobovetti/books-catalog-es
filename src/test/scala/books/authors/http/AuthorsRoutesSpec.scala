package books.authors.http

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
import akka.http.scaladsl.testkit.ScalatestRouteTest
import books.authors._
import common._
import org.scalamock.scalatest.MockFactory
import org.scalatest.funspec.AnyFunSpec
import org.scalatest.matchers.should.Matchers
import spray.json.DefaultJsonProtocol._

import scala.concurrent.{ExecutionContext, Future}

class AuthorsRoutesSpec extends AnyFunSpec with Matchers with ScalatestRouteTest with MockFactory {
  import AuthorsRoutesJsonFormats._
  import EnvelopJsonFormats._

  private def targetRoute(
                         commandSender: CommandSender[AuthorCommand] = mock[CommandSender[AuthorCommand]],
                         stateReader: SnapshotStateReader[String, Author] = mock[SnapshotStateReader[String, Author]],
                         ) = {
    new AuthorsRoutes(commandSender, stateReader)
      .createRoute()
  }

  describe("AuthorsRoutes") {

    it("should return list of authors") {
      val expectedAuthors = Seq(Author("code1", "name1", "lastName1"))
      val stateReader = mock[SnapshotStateReader[String, Author]]
      (stateReader.fetchAll(_: Boolean)).expects(false).returning(Future(expectedAuthors)).once()

      Get("/authors") ~> targetRoute(stateReader = stateReader) ~> check {
        responseAs[Seq[Author]] shouldEqual expectedAuthors
      }
    }

    it("should return a single author") {
      val expectedAuthor = Author("code1", "name1", "lastName1")
      val stateReader = mock[SnapshotStateReader[String, Author]]
      (stateReader.fetchOne(_: String)).expects("code1").returning(Future(Some(expectedAuthor))).once()

      Get("/authors/code1") ~> targetRoute(stateReader = stateReader) ~> check {
        responseAs[Author] shouldEqual expectedAuthor
      }
    }

    it("should create an author") {
      val cmdSender = mock[CommandSender[AuthorCommand]]
      val msgId = MsgId.random()
      mockCmdSend(cmdSender, msgId)

      val body = CreateAuthor("code1", "name1", "last1")
      Post("/authors", body) ~> targetRoute(commandSender = cmdSender) ~> check {
        responseAs[MsgId] should be(msgId)
      }
    }

    it("should update an author") {
      val cmdSender = mock[CommandSender[AuthorCommand]]
      val msgId = MsgId.random()
      mockCmdSend(cmdSender, msgId)

      val body = UpdateAuthor("name1", "last1")
      Put("/authors/code1", body) ~> targetRoute(commandSender = cmdSender) ~> check {
        responseAs[MsgId] should be(msgId)
      }
    }

    it("should delete an author") {
      val cmdSender = mock[CommandSender[AuthorCommand]]
      val msgId = MsgId.random()
      mockCmdSend(cmdSender, msgId)

      Delete("/authors/code1") ~> targetRoute(commandSender = cmdSender) ~> check {
        responseAs[MsgId] should be(msgId)
      }
    }
  }

  private def mockCmdSend(
                           cmdSender: CommandSender[AuthorCommand],
                           returningMsgId: MsgId,
                           expectedKey: String = "code1"
                         ): Unit = {
    val _ = (cmdSender.send(_: String, _: AuthorCommand)(_: ExecutionContext))
      .expects(expectedKey, *, *)
      .returning(Future(returningMsgId)).once()
  }
}