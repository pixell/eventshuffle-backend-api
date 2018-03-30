package models

import java.time.LocalDate

import org.scalatest.concurrent.ScalaFutures
import play.api.Application
import play.api.db.slick.DatabaseConfigProvider
import slick.jdbc.H2Profile

abstract class DatabaseFixtures(app: Application)
  extends Database
    with ScalaFutures
{
  protected val dbConfig = app.injector.instanceOf[DatabaseConfigProvider].get[H2Profile]

  import dbConfig._
  import profile.api._

  def clearDatabase = db.run {
    DBIO.seq(
      Votes.delete,
      EventDates.delete,
      Events.delete
    ).transactionally
  }.futureValue

  def run[A](action: DBIO[A]): A = db.run(action).futureValue

  def event(name: String) = run {
    Events.insert += (name)
  }

  def eventDate(eventId: Long, date: LocalDate) = run {
    EventDates.insert += (eventId, date)
  }

  def vote(eventDateId: Long, voter: String) = run {
    Votes.insert += (eventDateId, voter)
  }

}
