package controllers

import java.io.ByteArrayOutputStream

import csw.services.icd.IcdToPdf
import csw.services.icd.db.{IcdDbPrinter, IcdDb}
import play.Play
import play.api._
import play.api.mvc._
import play.filters.csrf.CSRFAddToken
import shared.Csrf
import play.api.libs.json._
import play.api.libs.functional.syntax._

object Application extends Controller {
  val databaseName = Play.application().configuration().getString("database.name")
  val db = IcdDb(databaseName)

  def index = CSRFAddToken {
    Action { implicit request =>
      import play.filters.csrf.CSRF
      val token = CSRF.getToken(request).map(t => Csrf(t.value)).getOrElse(Csrf(""))
      Ok(views.html.index(token))
    }
  }

  /**
   * Gets a list of top level ICD names
   */
  def icdNames = Action {
    val names = db.query.getIcdNames
    Ok(Json.toJson(names))
  }

  /**
   * Gets a list of components belonging the given ICD
   */
  def icdComponents(name: String) = Action {
    val names = db.query.getComponentNames(name)
    Ok(Json.toJson(names))
  }


  // Gets the HTML for the named ICD (without inserting any CSS)
  private def getAsPlainHtml(name: String): String = {
    val html = IcdDbPrinter(db.query).getAsPlainHtml(name)
    html
  }

  // Gets the HTML for the named ICD
  private def getAsHtml(name: String): String = {
    val html = IcdDbPrinter(db.query).getAsHtml(name)
    html
  }

  /**
   * Returns the HTML for the ICD with the given name
   * @param name the ICD name
   */
  def icdHtml(name: String) = Action {
    Ok(getAsPlainHtml(name)).as(HTML)
  }

  /**
   * Returns the PDF for the ICD with the given name
   * @param name the ICD name
   */
  def icdPdf(name: String) = Action {
    val out = new ByteArrayOutputStream()
    IcdToPdf.saveAsPdf(out, getAsHtml(name))
    val bytes = out.toByteArray
    Ok(bytes).as("application/pdf")
  }

}
