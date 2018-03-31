package models

import scala.collection.SortedMap

case class EventResults(event: Event,
                        votesPerDay: SortedMap[EventDate, Seq[Vote]])
