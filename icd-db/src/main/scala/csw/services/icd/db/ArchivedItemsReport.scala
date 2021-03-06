package csw.services.icd.db

import java.io.{File, FileOutputStream}

import csw.services.icd.IcdToPdf
import csw.services.icd.db.ArchivedItemsReport.ArchiveInfo
import csw.services.icd.html.IcdToHtml
import icd.web.shared.IcdModels.{ArchivedNameDesc, ComponentModel}

object ArchivedItemsReport {

  case class ArchiveInfo(component: String,
                         prefix: String,
                         eventType: String,
                         name: String,
                         description: String)

}

case class ArchivedItemsReport(db: IcdDb, subsystemOpt: Option[String]) {
  val query = new CachedIcdDbQuery(db.db)

  // Returns true if the given subsystem should be included in the report
  private def subsystemFilter(subsystem: String): Boolean = {
    if (subsystemOpt.isDefined) subsystemOpt.contains(subsystem)
    else !subsystem.startsWith("TEST")
  }

  // Gets all the archived items
  private def getArchivedItems: List[ArchiveInfo] = {
    // Gets the archived items from the list
    def getItems(c: ComponentModel,
                 eventType: String,
                 list: List[ArchivedNameDesc]): List[ArchiveInfo] = {
      val comp = c.component.replace("-", "-\n") // save horizontal space
      list
        .filter(_.archive)
        .map(e => ArchiveInfo(comp, c.prefix, eventType, e.name, e.description))
    }
    val result = for {
      component <- query.getComponents
      if subsystemFilter(component.subsystem)
      publishModel <- query.getPublishModel(component)
    } yield {
      getItems(component, "Alarm", publishModel.alarmList) ++
        getItems(component, "Events", publishModel.eventList) ++
        getItems(component, "EventStreams", publishModel.eventStreamList) ++
        getItems(component, "Telemetry", publishModel.telemetryList)
    }
    result.flatten
  }

  // Generates the HTML for the report
  private def makeReport(): String = {
    import scalatags.Text.all._

    def firstParagraph(s: String): String = {
      val i = s.indexOf("</p>")
      if (i == -1) s else s.substring(0, i + 4)
    }

    val markup = html(
      head(
        scalatags.Text.tags2.title("Archived Items"),
        scalatags.Text.tags2.style(scalatags.Text.RawFrag(IcdToHtml.getCss))
      ),
      body(
        h2("Archived Items"),
        div(
          table(
            thead(
              tr(
                th("Component"),
                th("Prefix"),
                th("Type"),
                th("Name"),
                th("Description")
              )
            ),
            tbody(
              for {
                item <- getArchivedItems
              } yield {
                tr(td(p(item.component)),
                   td(p(item.prefix)),
                   td(p(item.eventType)),
                   td(p(item.name)),
                   td(raw(firstParagraph(item.description))))
              }
            )
          )
        )
      )
    )
    markup.render
  }

  /**
    * Saves the report in HTML or PDF, depending on the file suffix
    */
  def saveToFile(file: File): Unit = {

    def saveAsHtml(html: String): Unit = {
      val out = new FileOutputStream(file)
      out.write(html.getBytes)
      out.close()
    }

    def saveAsPdf(html: String): Unit =
      IcdToPdf.saveAsPdf(file, html, showLogo = false)

    val html = makeReport()
    file.getName.split('.').drop(1).lastOption match {
      case Some("html") => saveAsHtml(html)
      case Some("pdf")  => saveAsPdf(html)
      case _            => println(s"Unsupported output format: Expected *.html or *.pdf")
    }
  }
}
