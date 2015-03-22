package csw.services.icd.gfm

import csw.services.icd.model.JsonSchemaModel

/**
 * Converts a list of event descriptions to a GFM formatted string
 */
case class EventListToGfm(list: List[JsonSchemaModel], level: Level) extends Gfm {

  import Gfm._

  private val head = mkHeading(level, 3, "Events")

  private val table = JsonSchemaListToGfm(list).gfm

  val gfm = s"$head\n$table"
}