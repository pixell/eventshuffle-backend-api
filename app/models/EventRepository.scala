package models

import java.time.LocalDate

import javax.inject.Singleton

import scala.collection.SortedMap
import scala.concurrent.ExecutionContext.Implicits.global

@Singleton
class EventRepository extends Database
{
  import profile.api._

  def list: DBIO[Seq[Event]] = Events.sortBy(_.id).result

  def create(name: String, dates: LocalDate*): DBIO[Event] = {
    for {
      event <- Events.insert += name
      _     <- DBIO.seq(dates.map(date => EventDates.insert += (event.id, date)) :_*)
    } yield event
  }

  def get(eventId: Long): DBIO[Option[(Event, SortedMap[EventDate, Seq[Vote]])]] = {
    getWithVotes(eventId, datesOnlyWithAllVoters = false)
  }

  def vote(voter: String, eventDateIds: Set[Long]): DBIO[Seq[Vote]] = {
    Votes.insert ++= eventDateIds.toSeq.map(id => (id, voter))
  }

  def showResults(eventId: Long): DBIO[Option[(Event, SortedMap[EventDate, Seq[Vote]])]] = {
    getWithVotes(eventId, datesOnlyWithAllVoters = true)
  }

  implicit def eventDateOrdering: Ordering[EventDate] = Ordering.fromLessThan(_.date isBefore _.date)

  private def getWithVotes(eventId: Long, datesOnlyWithAllVoters: Boolean) =
  {
    //
    // Using views instead of CTE's as support for the latter is not great in H2.
    //
    sql"""
      select
        e.id, e.name,
        ed.id, ed.date,
        v.id, v.voter
      from
        events e
        join event_dates ed on e.id = ed.event
        left outer join votes v on ed.id = v.event_date
      where
        e.id = ${eventId}
        and
        (${!datesOnlyWithAllVoters}
         or
         not exists (
          (select voter from all_voters_per_event where event = e.id)
          except
          (select voter from voters_per_event_date where event_date = ed.id)
         ))
      order by
        e.id,
        ed.date,
        v.voter
      """
      .as[(Long, String, Long, LocalDate, Option[Long], Option[String])]
      .map { rows =>
        rows.headOption.map { case (eventId, name, _, _, _, _) =>
          (Event(eventId, name), rows.foldLeft(SortedMap.empty[EventDate, Seq[Vote]]) {
            case (map, (_, _, eventDateId, date, Some(voteId), Some(voter))) =>
              val eventDate = EventDate(eventDateId, eventId, date)
              val votes = map.getOrElse(eventDate, Seq.empty)
              map + (eventDate -> (votes :+ Vote(voteId, eventDateId, voter)))

            case (map, (_, _, eventDateId, date, _, _)) if !datesOnlyWithAllVoters =>
              val eventDate = EventDate(eventDateId, eventId, date)
              val votes = map.getOrElse(eventDate, Seq.empty)
              map + (eventDate -> votes)

            case (map, _) => map
          })
        }
      }
  }
}
