package icd.web.client

import org.scalajs.dom
import org.scalajs.dom._
import upickle._
import BrowserHistory._
import Subsystem._

object BrowserHistory {

  // Type of a view in the application, used to restore the view
  sealed trait ViewType

  // Viewing components based on checkbox states in sidebar
  case object ComponentView extends ViewType

  // Viewing a single component from a publisher/subscriber/command link
  case object ComponentLinkView extends ViewType

  // Uploading ICD files
  case object UploadView extends ViewType

  // Publishing an API or ICD
  case object PublishView extends ViewType

  // Result of View menu => Static API as HTML Document
  case object HtmlView extends ViewType

  // Result of View menu => Static API as PDF Document
  case object PdfView extends ViewType

  // Viewing the version history
  case object VersionView extends ViewType

  // Gets  BrowserHistory from the event
  def popState(e: PopStateEvent): Option[BrowserHistory] = {
    if (e.state == null) None
    else Some(read[BrowserHistory](e.state.toString))
  }
}

/**
 * Object used to keep track of browser history for back button
 *
 * @param sourceSubsystem source subsystem selected in the left box
 * @param targetSubsystem target subsystem selected in the right box
 * @param sourceComponents source subsystem components whose checkboxes are checked
 * @param linkComponent set to the name of the component displayed via a subscriber/publisher/command link
 * @param viewType indicates the type of data being displayed
 */
case class BrowserHistory(sourceSubsystem: SubsystemWithVersion, targetSubsystem: SubsystemWithVersion,
                          sourceComponents: List[String], linkComponent: Option[String],
                          viewType: ViewType) {

  // Pushes the current application history state (Note that the title is ignored in some browsers)
  def pushState(): Unit = {
    val json = write(this)
    dom.history.pushState(json, dom.document.title, dom.document.documentURI)
  }
}

