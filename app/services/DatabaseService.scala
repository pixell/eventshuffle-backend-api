package services

import javax.inject.{Inject, Singleton}
import play.api.db.slick.DatabaseConfigProvider
import slick.jdbc.H2Profile

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class DatabaseService @Inject() (dbConfigProvider: DatabaseConfigProvider)
                                (implicit ec: ExecutionContext)
{
  protected val dbConfig = dbConfigProvider.get[H2Profile]

  import dbConfig._
  import profile.api._

  def run[T](action: DBIO[T]): Future[T] = db.run(action)

  def runTransactionally[T](action: DBIO[T]): Future[T] = run(action.transactionally)

}
