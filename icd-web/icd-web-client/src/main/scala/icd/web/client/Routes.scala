package icd.web.client

// XXX TODO: Pass settings from server, see ChatJS.main() for example
object Routes {
  val subsystems = "/subsystems"
  def components(subsystem: String, version: String) = s"/components/$subsystem/$version"
  def componentInfo(subsystem: String, version: String, compName: String) = s"/componentInfo/$subsystem/$version/$compName"
  def apiAsHtml(name: String) = s"/apiAsHtml/$name"
  def apiAsPdf(name: String) = s"/apiAsPdf/$name"
  val uploadFiles = "/uploadFiles"
  def versions(name: String) = s"/versions/$name"
  def versionNames(name: String) = s"/versionNames/$name"
}
