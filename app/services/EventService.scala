package services

import java.time.LocalDate

import javax.inject.{Inject, Singleton}
import models.{Event, EventDate, EventRepository, Vote}
import slick.dbio.DBIO

import scala.collection.SortedMap
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class EventService @Inject() (db: DatabaseService,
                              eventRepository: EventRepository)
                             (implicit ec: ExecutionContext) {
  import EventService._

  def list: Future[Seq[Event]] = db.run(eventRepository.list)

  def create(event: CreateEventRequest): Future[Event] =
    db.run(eventRepository.create(event.name, event.dates: _*))

  def get(eventId: Long): Future[Option[(Event, SortedMap[EventDate, Seq[Vote]])]] =
    db.run(eventRepository.get(eventId))

  def vote(eventId: Long, vote: VoteRequest): Future[Option[ResultsResponse]] =
    db.runTransactionally {
      eventRepository.get(eventId).flatMap {
        _ match {
          case Some((_, votesPerEventDate)) =>
            val dates = votesPerEventDate.keySet.map(_.date)

            // Event exists so check that all votes are given for days that exist in the event.
            vote.votes.filterNot(dates.contains) match {
              case nonExistingDates if nonExistingDates.nonEmpty =>
                DBIO.failed(ServiceException("Event does not contain following days: ${nonExistingDates}"))

              case _ =>
                // Filter out votes that already exist.
                val eventDateIds = votesPerEventDate.foldLeft(Set.empty[Long]) {
                  case (ids, (eventDate, votes)) if vote.votes.contains(eventDate.date) =>
                    if (votes.exists(_.voter == vote.name)) ids else (ids + eventDate.id)
                  case (ids, _) => ids
                }

                // Add votes for days that have not yet been voted for.
                val voting =
                  if (eventDateIds.nonEmpty) {
                    eventRepository.vote(vote.name, eventDateIds)
                  } else {
                    DBIO.successful(())
                  }

                // Finally return whole event.
                voting.flatMap(_ => eventRepository.get(eventId))
            }

          case _ => DBIO.failed(NotFoundException(s"No event found for ${eventId}"))
        }
      }
    }

  def showResults(eventId: Long): Future[Option[ResultsResponse]] =
    db.run(eventRepository.showResults(eventId))

}

object EventService {

  case class CreateEventRequest(name: String, dates: Seq[LocalDate])

  case class VoteRequest(name: String, votes: Seq[LocalDate])

  type ResultsResponse = (Event, SortedMap[EventDate, Seq[Vote]])

  case class ServiceException(message: String) extends Exception(message)

  case class NotFoundException(message: String) extends Exception(message)

}