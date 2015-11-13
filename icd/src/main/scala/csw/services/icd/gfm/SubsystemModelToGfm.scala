package csw.services.icd.gfm

import csw.services.icd.model.SubsystemModel

/**
 * Converts a SubsystemModel instance to a GFM formatted string
 */
case class SubsystemModelToGfm(m: SubsystemModel, level: Level) extends Gfm {

  import Gfm._

  private val head = mkHeading(level, 1, m.title)

  private val desc = mkParagraph(m.description)

  val gfm = s"$head\n$desc"
}