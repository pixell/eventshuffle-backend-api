package util

import org.scalatestplus.play.guice.GuiceFakeApplicationFactory
import play.api.Application
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.test.Helpers

trait InMemoryDatabase extends GuiceFakeApplicationFactory {

  override def fakeApplication(): Application = {
    new GuiceApplicationBuilder()
      .configure(
        Map(
          "slick.dbs.default.db.url" -> ("jdbc:h2:mem:play-test;DB_CLOSE_DELAY=-1;DATABASE_TO_UPPER=FALSE;MVCC=true;LOCK_TIMEOUT=5000")
        )
      )
      .build()
  }

}
