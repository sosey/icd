package icd.web.client

import org.scalajs.dom
import org.scalajs.dom.raw.{ Node, HTMLInputElement }

import org.scalajs.dom.Element

import scalatags.JsDom.all._

/**
 * Manages the sidebar items
 *
 * @param listener called with the component name and checkbox state
 *                 when one of the component checkboxes is checked or unchecked
 */
case class Sidebar(listener: (String, Boolean) ⇒ Unit) extends Displayable {

  val sidebarList = ul(cls := "nav list-group").render

  // HTML for component
  private def componentCheckBox(compName: String, checkboxListener: dom.Event ⇒ Unit) = {
    import scalacss.ScalatagsCss._

    li(
      a(Styles.listGroupItem)(
        div(cls := "checkbox")(
          label(
            input(tpe := "checkbox", value := compName, checked := true, onchange := checkboxListener),
            compName))))
  }

  /**
   * Returns a list of names of all the components whose checkboxes are checked
   */
  def getSelectedComponents: List[String] = {
    val nodeList = sidebarList.getElementsByTagName("input")
    val result = for (i ← 0 until nodeList.length) yield {
      val elem = nodeList(i).asInstanceOf[HTMLInputElement]
      if (elem.checked) Some(elem.value) else None
    }
    result.toList.flatten
  }

  /**
   * Sets the list of checked components in the sidebar
   */
  def setSelectedComponents(components: List[String]): Unit = {
    val set = components.toSet
    val nodeList = sidebarList.getElementsByTagName("input")
    for (i ← 0 until nodeList.length) {
      val elem = nodeList(i).asInstanceOf[HTMLInputElement]
      val checked = set.contains(elem.value)
      if (elem.checked != checked) elem.checked = checked
    }
  }

  // called when a component is selected or deselected
  private def componentSelected(compName: String)(e: dom.Event): Unit = {
    val checked = e.target.asInstanceOf[HTMLInputElement].checked
    listener(compName, checked)
  }

  // Uncheck all of the checkboxes in the sidebar
  def uncheckAll(): Unit = {
    val nodeList = sidebarList.getElementsByTagName("input")
    for (i ← 0 until nodeList.length) nodeList(i).asInstanceOf[HTMLInputElement].checked = false
  }

  /**
   * Adds an HTML element to the sidebar.
   * @param node a scalatags node
   */
  def addItem(node: Node): Unit = {
    sidebarList.appendChild(node)
  }

  /**
   * Adds an ICD component to the sidebar
   */
  def addComponent(compName: String): Unit = {
    addItem(componentCheckBox(compName, componentSelected(compName)).render)
  }

  /**
   * Removes all the ICD components from the sidebar
   */
  def clearComponents(): Unit = {
    sidebarList.innerHTML = ""
  }

  // Display the subsystem combobox at top, then the list of component checkboxes
  override def markup(): Element = {
    import scalacss.ScalatagsCss._
    div(Styles.sidebarWrapper)(
      div(Styles.sidebar)(
        sidebarList)).render
  }
}

