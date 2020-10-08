package common.http

import akka.http.scaladsl.model._
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
import common.streaming._
import spray.json.DefaultJsonProtocol._

import scala.concurrent.ExecutionContext

class MetadataRoutes(metadataService: MetadataService) extends RouteController {
  import common.CommonJsonFormats._

  def createRoute()(implicit executionContext: ExecutionContext): Route = {
    val storeNameRegexPattern = """\w+""".r

    path("instances") {
      get {
        complete(StatusCodes.OK, metadataService.allHosts())
      }
    } ~
      path("instances" / storeNameRegexPattern) { storeName =>
        get {
          complete(StatusCodes.OK, metadataService.hostsForStore(storeName))
        }
      }
  }
}
