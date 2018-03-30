package util

import java.time.LocalDate

import play.api.libs.json._
import play.api.libs.ws.{WSClient, WSResponse}
import play.api.test.Helpers

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

object ApiClient {

  val baseUrl = s"http://localhost:${Helpers.testServerPort}/api/v1"

  implicit class RichWSResponse(response: WSResponse) {
    def asJson[T](implicit fjs: Reads[T]) = {
      Json.fromJson[T](response.json) match {
        case JsSuccess(value, _) => value
        case e: JsError          => throw new RuntimeException("Failed to decode body as JSON: " + e)
      }
    }
  }

  implicit class RichWSClient(client: WSClient) {

    def createEvent(name: String, dates: LocalDate*): Future[(WSResponse, Long)] = {
      client
        .url(s"${baseUrl}/event")
        .post(Json.obj(
          "name"  -> name,
          "dates" -> dates
        ))
        .map { resp => (resp, (resp.json \ "id").as[Long]) }
    }

    def listEvents: Future[(WSResponse, Seq[JsValue])] = {
      client
        .url(s"${baseUrl}/event/list")
        .get
        .map { resp => (resp, (resp.json \ "events").as[Seq[JsValue]]) }
    }

    def showEvent(eventId: Long): Future[(WSResponse, JsObject)] = {
      client
        .url(s"${baseUrl}/event/${eventId}")
        .get
        .map { resp => (resp, resp.json.as[JsObject]) }
    }

    def showResults(eventId: Long): Future[(WSResponse, JsObject)] = {
      client
        .url(s"${baseUrl}/event/${eventId}/results")
        .get
        .map { resp => (resp, resp.json.as[JsObject]) }
    }

    def vote(eventId: Long, voter: String, dates: LocalDate*): Future[(WSResponse, JsObject)] = {
      client
        .url(s"${baseUrl}/event/${eventId}/vote")
        .post(Json.obj(
          "name"  -> voter,
          "votes" -> dates
        ))
        .map { resp => (resp, resp.json.as[JsObject]) }
    }

  }

}
