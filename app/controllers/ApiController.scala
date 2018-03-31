package controllers

import javax.inject._
import models.{EventDate, EventResults, Vote}
import play.api.libs.json._
import play.api.mvc._
import services.EventService
import services.EventService.ServiceException

import scala.collection.SortedMap
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class ApiController @Inject()(eventService: EventService,
                              cc: ControllerComponents
                              )(implicit ec: ExecutionContext)
  extends AbstractController(cc)
{
  import ApiController._

  def index = Action { implicit request =>
    Ok(views.html.index())
  }

  def list = Action.async { implicit request =>
    eventService.list.map { events =>
      val response =
        for {
          event <- events
        } yield Json.obj(
          "id"   -> event.id,
          "name" -> event.name
        )

      Ok(Json.obj("events" -> response))
    }
  }

  def create = Action.async { implicit request =>
    request.body.asJson match {
      case Some(input) =>
        input.validate[EventService.CreateEventRequest].fold(
          error => {
            BadRequest(JsError.toJson(error))
          },
          value => {
            eventService.create(value).map {
              event => Created(Json.obj("id" -> event.id))
            }
          }
        )

      case _ => BadRequest("Body missing")
    }
  }

  def show(eventId: Long) = Action.async { implicit request =>
    eventService.get(eventId).map {
      _ match {
        case Some(results) => resultsToJson(results)
        case None          => NotFound(s"No event found for ${eventId}")
      }
    }
  }

  def vote(eventId: Long) = Action.async { implicit request =>
    request.body.asJson match {
      case Some(input) =>
        input.validate[EventService.VoteRequest].fold(
          error => {
            BadRequest(JsError.toJson(error))
          },
          value => {
            eventService.vote(eventId, value)
              .map {
                case Some(results) => resultsToJson(results)
                case None          => NotFound(s"No event found for ${eventId}")
              }
              .recover {
                case ServiceException(message) => BadRequest(message)
              }
          }
        )

      case _ => BadRequest("Body missing")
    }
  }

  def getResults(eventId: Long) = Action.async { implicit request =>
    eventService.showResults(eventId).map {
      _ match {
        case Some(results) =>
          val response = Json.obj(
            "id"            -> results.event.id,
            "name"          -> results.event.name,
            "suitableDates" -> votesToJson(results.votesPerDay)
          )

          Ok(response)

        case _ => NotFound(s"No event found for ${eventId} 2")
      }
    }
  }

  private def resultsToJson(results: EventResults) = {
    val response = Json.obj(
      "id"    -> results.event.id,
      "name"  -> results.event.name,
      "dates" -> results.votesPerDay.keys.map(_.date),
      "votes" -> votesToJson(results.votesPerDay)
    )

    Ok(response)
  }

  private def votesToJson(votesPerDay: SortedMap[EventDate, Seq[Vote]]) = {
    votesPerDay
      .filter {
        case (_, votes) => votes.nonEmpty
      }
      .map {
        case (eventDate, votes) => Json.obj(
          "date"   -> eventDate.date,
          "people" -> votes.map(_.voter)
        )
}
  }
}

object ApiController {

  implicit val createEventBodyReads: Reads[EventService.CreateEventRequest] = Json.reads

  implicit val voteBodyReads: Reads[EventService.VoteRequest] = Json.reads

  implicit def resultAsFuture(result: Result): Future[Result] = Future.successful(result)

}