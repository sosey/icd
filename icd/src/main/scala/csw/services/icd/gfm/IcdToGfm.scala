package csw.services.icd.gfm

import csw.services.icd.model._

/**
 * Converts an ICD model to "GitHub Flavored Markdown" or GFM.
 */
case class IcdToGfm(p: IcdModels, level: Level = Level()) extends Gfm {

  // Used to increment the level below
  // XXX TODO FIXME: The section numbering needs work!
  // XXX TODO FIXME: Can it be done automatically with numbered lists in HTML with ScalaTags?
  private implicit val counter = (0 to 5).iterator

  // Ignore missing parts for now...
  val parts = List(
    p.subsystemModel.map(SubsystemModelToGfm(_, level).gfm),
    p.componentModel.map(ComponentModelToGfm(_, level.inc2()).gfm),
    p.publishModel.map(PublishModelToGfm(_, level.inc2()).gfm),
    p.subscribeModel.map(SubscribeModelToGfm(_, level.inc2()).gfm),
    p.commandModel.map(CommandModelToGfm(_, level.inc2()).gfm)).flatten

  /**
   * The "GitHub Flavored Markdown" or GFM for the model as a string
   */
  val gfm = parts.mkString("\n\n")
}

object IcdToGfm {
  // Returns a Gfm link to the given Gfm heading for the TOC. For example:
  // * [3.2 Publish](3.2-publish)
  // Note that whitespace is converted to '-', special chars are ignored, and text is converted to lower case in the target.
  private def mkGfmLink(heading: String, indentOffset: Int): String = {
    val indent = "\t" * (heading.takeWhile(_ == '#').length - 2 - indentOffset)
    val s = heading.dropWhile(_ == '#')
    // XXX could also parse target from <a> element
    val text = s.substring(s.lastIndexOf("</a>") + 4)
    val target = "#" + Gfm.headingTargetName(text)
    s"$indent* [$text]($target)"
  }

  /**
   * Returns a TOC for the given GFM body
   */
  def gfmToToc(body: String, includesSubsystem: Boolean): String = {
    val indentOffset = if (includesSubsystem) 0 else 1
    val links = for (line ← body.lines if line.startsWith("#")) yield mkGfmLink(line, indentOffset)
    "## Table of Contents\n\n" + links.toList.mkString("\n")
  }
}
