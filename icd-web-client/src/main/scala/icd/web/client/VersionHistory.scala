package icd.web.client

import icd.web.shared._
import org.scalajs.dom
import org.scalajs.dom.ext.Ajax
import org.scalajs.dom.raw.HTMLInputElement
import play.api.libs.json._

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global
import org.querki.jquery._
import org.scalajs.dom.html.Button

import scalatags.JsDom.all._

object VersionHistory {
  // Split a version string into (maj.min)
  private def splitVersion(v: String): (String, String) = {
    val ar = v.split('.')
    if (ar.length == 2)
      (ar(0), ar(1))
    else
      ("", "")
  }

  // Compare two version strings (maj.min)
  private def compareVersions(v1: String, v2: String): Boolean = {
    val (maj1, min1) = splitVersion(v1)
    val (maj2, min2) = splitVersion(v2)
    maj1 < maj2 || maj1 == maj2 && min1 < min2
  }
}

/**
  * Manages the main content section
  */
case class VersionHistory(mainContent: MainContent) extends Displayable {

  import VersionHistory._
  import icd.web.shared.JsonSupport._

  // Main version div
  private val contentDiv = div(id := "versionHistory").render

  // Displays the Compare button
  private def compareButton(subsystem: String): Button = {
    import scalatags.JsDom.all._
    button(
      title := "Compare two selected subsystem versions",
      onclick := compareHandler(subsystem) _
    )("Compare").render
  }

  // Displays the diff of two versions
  private val diffDiv = div(id := "versionDiff").render

  // Called when the Compare button is pressed
  private def compareHandler(subsystem: String)(e: dom.Event): Unit = {
    val checked = $("input[name='version']:checked")
    if (checked.length == 2) {
      val versions = checked.mapElems(elem => elem.asInstanceOf[HTMLInputElement].value).sortWith(compareVersions).toList
      val route = Routes.diff(subsystem, versions)
      Ajax.get(route).map { r =>
        val list = Json.fromJson[List[DiffInfo]](Json.parse(r.responseText)).getOrElse(Nil)
        diffDiv.innerHTML = ""
        diffDiv.appendChild(markupDiff(subsystem, list))
      }
    }
  }

  // Display the results of comparing two versions
  private def markupDiff(subsystem: String, list: List[DiffInfo]) = {

    def jsonDiffMarkup(infoList: List[JsonDiff]) = {
      import scalacss.ScalatagsCss._
      val headings = List("Operation", "Path", "Old Value", "New Value")

      // Display quoted strings as just the text, but display json objects as objects
      def displayJson(json: String) = {
        if (json.startsWith("\"")) {
          div(json.substring(1, json.length-1).replace("\\n", "\n").trim.split("\n").map(s => p(s)))
        } else {
          pre(code(Styles.unstyledPre, Json.prettyPrint(Json.parse(json))))
        }
      }

      table(attr("data-toggle") := "table",
        thead(
          tr(headings.map(th(_)))
        ),
        tbody(
          for (i <- infoList) yield {
            tr(
              td(p(i.op)),
              td(p(i.path)),
              td(div(Styles.scrollableDiv, displayJson(i.old))),
              td(div(Styles.scrollableDiv, displayJson(i.value)))
            )
          }
        ))

    }

    def formatPath(path: String): String = {
      val l = path.split('.').tail
      if (l.length == 2) {
        val (component, section) = (l(0), l(1))
        s"Changes to component $component in the $section section"
      } else path
    }

    def diffInfoMarkup(diffInfo: DiffInfo) = {
      val infoList = Json.fromJson[List[JsonDiff]](Json.parse(diffInfo.jsonDiff)) match {
        case JsSuccess(list: List[JsonDiff], _: JsPath) =>
          list
        case e: JsError =>
          println(s"${JsError.toJson(e).toString()}")
          Nil
      }
      div(
        h3(formatPath(diffInfo.path)),
        jsonDiffMarkup(infoList)
      )
    }

    div(
      h2(s"Changes to $subsystem"),
      list.map(diffInfoMarkup)
    ).render
  }

  // Called when one of the version checkboxes is clicked to update the enabled state of the compare
  // button when exactly two items are selected
  private def checkboxListener(compButton: Button)(e: dom.Event): Unit = {
    val checked = $("input[name='version']:checked")
    compButton.disabled = checked.length != 2
  }

  // Returns a checkbox displaying the version (select two to compare versions)
  private def makeVersionCheckBox(version: Option[String], compButton: Button) = {
    div(cls := "checkbox")(
      label(
        input(
          name := "version",
          title := s"Select this version for comparison",
          tpe := "checkbox",
          onchange := checkboxListener(compButton) _,
          value := version.getOrElse("")
        ),
        version
      )
    )
  }

  // Returns the markup for displaying a table of version information for a subsystem
  private def markupSubsystemVersionInfo(subsystem: String, list: List[VersionInfo]) = {
    import scalacss.ScalatagsCss._
    if (list.isEmpty) div(p(em(s"No published versions found for $subsystem."))).render
    else {
      val compButton = compareButton(subsystem)
      div(
        table(Styles.componentTable, attr("data-toggle") := "table",
          thead(
            tr(
              th(subsystem, br, "Version"),
              th("User"),
              th("Date"),
              th("Comment")
            )
          ),
          tbody(
            for (v <- list) yield {
              tr(
                td(makeVersionCheckBox(v.version, compButton)),
                td(v.user),
                td(Styles.noWrapTableColumn, v.date),
                td(v.comment)
              )
            }
          )), compButton, diffDiv
      ).render
    }

  }

  // Returns the markup for displaying a table of version information for an ICD
  private def markupIcdVersionInfo(icdName: IcdName, list: List[IcdVersionInfo]) = {
    import scalacss.ScalatagsCss._
    if (list.isEmpty) div().render
    else div(
      table(Styles.componentTable, attr("data-toggle") := "table",
        thead(
          tr(
            th("ICD", br, "Version"),
            th(icdName.subsystem, br, "Version"),
            th(icdName.target, br, "Version"),
            th("User"),
            th("Date"),
            th("Comment")
          )
        ),
        tbody(
          for (v <- list) yield {
            val icdVersion = v.icdVersion
            tr(
              td(icdVersion.icdVersion),
              td(icdVersion.subsystemVersion),
              td(icdVersion.targetVersion),
              td(v.user),
              td(Styles.noWrapTableColumn, v.date),
              td(v.comment)
            )
          }
        ))
    ).render

  }

  // Gets the subsystem version info from the server
  private def getSubsystemVersionInfo(subsystem: String): Future[List[VersionInfo]] =
    Ajax.get(Routes.versions(subsystem)).map { r =>
      Json.fromJson[List[VersionInfo]](Json.parse(r.responseText)) match {
        case JsSuccess(list: List[VersionInfo], _: JsPath) =>
          list
        case e: JsError =>
          mainContent.displayInternalError(JsError.toJson(e).toString())
          Nil
      }
    }.recover {
      case ex =>
        mainContent.displayInternalError(ex)
        Nil
    }

  // Gets the ICD version info from the server
  private def getIcdVersionInfo(icdName: IcdName): Future[List[IcdVersionInfo]] = {
    import play.api.libs.json._
    Ajax.get(Routes.icdVersions(icdName)).map { r =>
      Json.fromJson[List[IcdVersionInfo]](Json.parse(r.responseText)) match {
        case JsSuccess(list: List[IcdVersionInfo], _: JsPath) =>
          list
        case e: JsError =>
          mainContent.displayInternalError(JsError.toJson(e).toString())
          Nil
      }
    }.recover {
      case ex =>
        mainContent.displayInternalError(ex)
        Nil
    }
  }

  def setSubsystem(subsystem: String): Unit = {
    getSubsystemVersionInfo(subsystem).foreach { list =>
      diffDiv.innerHTML = ""
      contentDiv.innerHTML = ""
      contentDiv.appendChild(markupSubsystemVersionInfo(subsystem, list))
    }
  }

  def setIcd(icdName: IcdName): Unit = {
    getIcdVersionInfo(icdName).foreach { list =>
      diffDiv.innerHTML = ""
      contentDiv.innerHTML = ""
      contentDiv.appendChild(markupIcdVersionInfo(icdName, list))
    }
  }

  def markup() = contentDiv
}
