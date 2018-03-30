package models

import java.time.LocalDate

import slick.jdbc.{GetResult, H2Profile}

private[models] trait Database {

  val profile = H2Profile
  import profile.api._

  implicit val localDateColumnType = MappedColumnType.base[LocalDate, String](
    { _.toString },
    { LocalDate.parse }
  )

  implicit val getLocalDateResult = GetResult { r => LocalDate.parse(r.<<) }

  implicit val getLocalDateResultOption = GetResult { _.nextStringOption().map(LocalDate.parse) }

  class EventsTable(tag: Tag) extends Table[Event](tag, "events") {
    val id = column[Long]("id", O.PrimaryKey, O.AutoInc)

    val name = column[String]("name")

    def * = (id, name) <> ((Event.apply _).tupled, Event.unapply _)
  }

  class EventDatesTable(tag: Tag) extends Table[EventDate](tag, "event_dates") {
    val id = column[Long]("id", O.PrimaryKey, O.AutoInc)

    val event = column[Long]("event")

    val date = column[LocalDate]("date")

    lazy val eventFk = foreignKey("event_fkey", event, Events)(_.id, onUpdate = ForeignKeyAction.NoAction, onDelete = ForeignKeyAction.NoAction)

    def * = (id, event, date) <> ((EventDate.apply _).tupled, EventDate.unapply _)
  }

  class VotesTable(tag: Tag) extends Table[Vote](tag, "votes") {
    val id = column[Long]("id", O.PrimaryKey, O.AutoInc)

    val eventDate = column[Long]("event_date")

    val voter = column[String]("voter")

    lazy val eventDateFk = foreignKey("event_date_fkey", eventDate, EventDates)(_.id, onUpdate = ForeignKeyAction.NoAction, onDelete = ForeignKeyAction.NoAction)

    def * = (id, eventDate, voter) <> ((Vote.apply _).tupled, Vote.unapply _)
  }

  lazy val Events = new TableQuery(new EventsTable(_)) {
    val insert = this.map(_.name) returning this.map(_.id) into {
      case (name, id) => Event(id, name)
    }
  }

  lazy val EventDates = new TableQuery(new EventDatesTable(_)) {
    val insert = this.map(ed => (ed.event, ed.date)) returning this.map(_.id) into {
      case ((event, date), id) => EventDate(id, event, date)
    }
  }

  lazy val Votes = new TableQuery(new VotesTable(_)) {
    val insert = this.map(v => (v.eventDate, v.voter)) returning this.map(_.id) into {
      case ((eventDate, voter), id) => Vote(id, eventDate, voter)
    }
  }

}
