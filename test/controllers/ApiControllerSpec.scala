package controllers

import java.time.LocalDate

import models.DatabaseFixtures
import org.scalatest.BeforeAndAfter
import org.scalatest.concurrent.ScalaFutures
import org.scalatestplus.play._
import org.scalatestplus.play.guice._
import play.api.libs.json.{JsArray, Json}
import play.api.libs.ws.WSClient
import play.api.test._
import util.ApiClient._
import util.InMemoryDatabase

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.concurrent.duration._

class ApiControllerSpec
  extends PlaySpec
    with BeforeAndAfter
    with GuiceOneServerPerSuite
    with Injecting
    with InMemoryDatabase
    with ScalaFutures
{
  override implicit def patienceConfig = PatienceConfig(scaled(5 seconds), scaled(20 millis))

  val client = inject[WSClient]

  val date = LocalDate.of(_: Int, _: Int, _: Int)

  "API" should {
    "return empty list when no event exists" in {
      whenReady(client.listEvents) { case (response, events) =>
        response.status must be(200)
        events must be (empty)
      }
    }

    "return event list" in new Fixtures {
      whenReady(client.listEvents) { case (response, events) =>
        response.status must be (200)
        events must have size (3)
        events.map(e => (e \ "name").as[String]) must contain only (
          "Jake's secret party",
          "Bowling night",
          "Tabletop gaming"
        )
      }
    }

    "create new event and show it" in {
      val name = "Jake's secret party"
      val dates = date(2014, 1, 1) :: date(2014, 1, 5) :: date(2014, 1, 12) :: Nil

      whenReady(client.createEvent(name, dates :_*)) { case (createResponse, id) =>
        createResponse.status must be (201)
        id must be > 0l

        whenReady(client.showEvent(id)) { case (showResponse, event) =>
          showResponse.status must be (200)

          (event \ "id").as[Long] must be (id)
          (event \ "name").as[String] must be (name)
          (event \ "dates").as[Array[LocalDate]] must contain only (dates :_*)
          (event \ "votes").as[JsArray].value must be (empty)
        }
      }
    }

    "register vote for an event" in new Fixtures {
      val voter = "Dick"
      val dates = date(2014, 1, 1) :: date(2014, 1, 5) :: Nil

      whenReady(client.vote(base.event1.id, voter, dates :_*)) { case (response, event) =>
        response.status must be (200)

        (event \ "id").as[Long] must be (base.event1.id)
        (event \ "name").as[String] must be ("Jake's secret party")
        (event \ "dates").as[Array[LocalDate]] must contain only (date(2014, 1, 12) :: dates :_*)
        (event \ "votes").as[JsArray] must be (Json.arr(
          Json.obj(
            "date"   -> date(2014, 1, 1),
            "people" -> (base.votes.map(_.voter) :+ voter).sorted
          ),
          Json.obj(
            "date"   -> date(2014, 1, 5),
            "people" -> (voter :: Nil)
          )
        ))
      }
    }

    "show results for an event" in new Fixtures {
      whenReady(client.showResults(base.event1.id)) { case (response, results) =>
        response.status must be (200)

        (results \ "id").as[Long] must be (base.event1.id)
        (results \ "name").as[String] must be ("Jake's secret party")
        (results \ "suitableDates").as[JsArray] must be (Json.arr(
          Json.obj(
            "date"   -> date(2014, 1, 1),
            "people" -> base.votes.map(_.voter).sorted
          )
        ))
      }
    }

    "create event, register votes and show results" in {
      val name = "Jedi Days"
      val dates = date(2018, 4, 9) :: date(2018, 4, 12) :: date(2018, 4, 16) :: Nil
      val voters = "Luke" :: "Obi-wan" :: "Yoda" :: Nil

      val wholeFlow =
        for {
          (_, eventId) <- client.createEvent(name, dates :_*)
          _            <- Future.sequence(
            for {
              (voter, index) <- voters.zipWithIndex
            } yield client.vote(eventId, voter, dates.filter(_.getDayOfMonth % (index + 1) == 0) :_*)
          )
          (_, results) <- client.showResults(eventId)
        } yield (eventId, results)

      whenReady(wholeFlow) { case (eventId, results) =>
        (results \ "id").as[Long] must be (eventId)
        (results \ "name").as[String] must be ("Jedi Days")
        (results \ "suitableDates").as[JsArray] must be (Json.arr(
          Json.obj(
            "date"   -> date(2018, 4, 12),
            "people" -> voters.sorted
          )
        ))
      }
    }
  }

  class Fixtures extends DatabaseFixtures(app) {
    clearDatabase

    val base = new {
      val event1 = event("Jake's secret party")
      val event1Dates = (date(2014, 1, 1) :: date(2014, 1, 5) :: date(2014, 1, 12) :: Nil).map(date => eventDate(event1.id, date))

      val event2 = event("Bowling night")
      val event2Dates = (date(2014, 3, 1) :: date(2014, 3, 2) :: Nil).map(date => eventDate(event2.id, date))

      val event3 = event("Tabletop gaming")
      val event3Dates = (date(2014, 3, 1) :: date(2014, 3, 2) :: date(2014, 3, 3) :: Nil).map(date => eventDate(event3.id, date))

      val votes = ("John" :: "Julia" :: "Paul" :: "Daisy" :: Nil).map(voter => vote(event1Dates.head.id, voter))
    }
  }
}
