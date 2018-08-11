package simpleenvconfig

import org.specs2.{ ScalaCheck, Specification }

class ConfigReportSpec extends Specification with ScalaCheck {

  def is = s2"""
    ConfigReport should
      support appending config values $append
      support pretty printing all values $prettyPrintAll
      support pretty printing masking secrets and passwords $prettyPrintMaskingSecrets
      support pretty printing with mask preficate $prettyPrintMaskPredicate
    """

  def append =
    prop { (a: Map[String, String], b: Map[String, String]) ⇒
      val configReport = b.foldLeft(ConfigReport(a)) { _ + _ }
      configReport.readValues mustEqual (a ++ b)
    }

  def prettyPrintAll = {
    val report = ConfigReport(Map("a" -> "a", "b" -> "b", "password" -> "changeit", "secret" -> "changeit")).prettyPrint

    report.split(System.lineSeparator) mustEqual (Array(
      "Config:",
      "  - a: a",
      "  - b: b",
      "  - password: changeit",
      "  - secret: changeit"
    ))

  }

  def prettyPrintMaskingSecrets = {
    val report = ConfigReport(Map("a" -> "a", "b" -> "b", "password" -> "changeit", "secret" -> "changeit")).prettyPrintMaskPasswords

    report.split(System.lineSeparator) mustEqual (Array(
      "Config:",
      "  - a: a",
      "  - b: b",
      "  - password: ******",
      "  - secret: ******"
    ))

  }

  def prettyPrintMaskPredicate = {
    val report = ConfigReport(Map("a" -> "a", "b" -> "b", "password" -> "changeit", "secret" -> "changeit")).prettyPrintWithMask(_ != "a")

    report.split(System.lineSeparator) mustEqual (Array(
      "Config:",
      "  - a: a",
      "  - b: ******",
      "  - password: ******",
      "  - secret: ******"
    ))

  }

}
