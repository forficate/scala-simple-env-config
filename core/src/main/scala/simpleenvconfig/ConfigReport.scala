package simpleenvconfig

import scala.annotation.tailrec

object ConfigReport {
  def empty: ConfigReport =
    ConfigReport(Map.empty[String, String])
}

final case class ConfigReport(readValues: Map[String, String]) {
  def +(other: (String, String)): ConfigReport =
    copy(readValues + other)

  def prettyPrint: String =
    prettyPrintWithMask(_ ⇒ false)

  def prettyPrintMaskPasswords: String =
    prettyPrintWithMask(s ⇒ List("secret", "password").exists(s.toLowerCase.contains))

  @SuppressWarnings(Array("org.wartremover.warts.NonUnitStatements"))
  def prettyPrintWithMask(maskPredicate: String ⇒ Boolean): String = {
    val sb = new StringBuilder
    sb.append("Config:").append(System.lineSeparator)

    @tailrec def loop(item: List[(String, String)]): Unit =
      item match {
        case (key, value) :: xs ⇒
          sb.append("  - ").append(key).append(": ")
          if (maskPredicate(value)) sb.append("******") else sb.append(value)
          if (xs.nonEmpty) sb.append(System.lineSeparator)
          loop(xs)
        case Nil ⇒
          ()
      }

    loop(readValues.toList.sortBy(_._1))

    sb.toString()
  }
}
