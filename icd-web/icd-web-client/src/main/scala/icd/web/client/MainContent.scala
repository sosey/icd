package icd.web.client

import org.scalajs.dom.Element

import scalatags.JsDom.all._

/**
 * Manages the main content section, which displays information on the
 * selected subsystem and components
 */
case class MainContent() extends Displayable {
  // Title to display
  private val contentTitle = h3(cls := "page-header")().render

  // Optional description
  private val contentDescription = p.render

  // Holds the content to display
  private val contentDiv = {
    import scalacss.ScalatagsCss._
    div(Styles.contentDiv, id := "content").render
  }

  /**
   * Sets the title and HTML content of the main section of the page
   * @param content the content to display
   * @param title the title to display
   */
  def setContent(content: String, title: String): Unit = {
    setTitle(title)
    this.contentDiv.innerHTML = content
  }

  /**
   * Sets the title and HTML content of the main section of the page
   * @param displayable the content to display
   * @param title the title to display
   */
  def setContent(displayable: Displayable, title: String): Unit = {
    setTitle(title)
    contentDiv.innerHTML = ""
    contentDiv.appendChild(displayable.markup())
  }

  /**
   * Sets the title and optional subtitle of the main section of the page
   */
  def setTitle(title: String, subtitleOpt: Option[String] = None, descriptionOpt: Option[String] = None): Unit = {
    subtitleOpt match {
      case Some(subtitle) ⇒
        contentTitle.innerHTML = s"$title<br><small>$subtitle</small>"
      case None ⇒
        contentTitle.textContent = title
    }
    setDescription(descriptionOpt.getOrElse(""))
  }

  /**
   * Gets the title of the page (excluding the subtitle, if there is one)
   */
  def getTitle: String = {
    val s = contentTitle.innerHTML
    s.indexOf("<br>") match {
      case -1 ⇒ s
      case n  ⇒ s.substring(0, n)
    }
  }

  /**
   * Sets the optional description text below the title
   */
  def setDescription(s: String): Unit = {
    contentDescription.innerHTML = s
  }

  /**
   * Gets the description being displayed
   */
  def getDescription: String = contentDescription.innerHTML

  /**
   * Clear out the title and content
   */
  def clearContent(): Unit = setContent("", "")

  /**
   * Appends the element to the content
   */
  def appendElement(element: Element): Unit = contentDiv.appendChild(element)

  /**
   * Removes the element from the content
   */
  def removeElement(element: Element): Unit = contentDiv.removeChild(element)

  /**
   * Replace the old element with the new one
   */
  def replaceElement(oldElement: Element, newElement: Element): Unit = contentDiv.replaceChild(newElement, oldElement)

  /**
   * Displays an error message for the exception
   */
  def displayInternalError(ex: Throwable): Unit = {
    // Display an error message
    println(s"Internal error: $ex")
    setContent(errorDiv("Internal error. The database may be down."), "Internal Error")
  }

  /**
   * Scroll the title to the top
   */
  def scrollToTop(): Unit = {
    contentTitle.scrollTop = 0
  }

  def markup() = {
    import scalacss.ScalatagsCss._
    div(Styles.mainContent)(
      div(Styles.main)(contentTitle, contentDescription, contentDiv)).render
  }
}