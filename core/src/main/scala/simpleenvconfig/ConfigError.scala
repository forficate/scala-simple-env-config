package simpleenvconfig

import scalaz.Show

final case class ConfigError(key: String, value: Unmarshaller.Error)

object ConfigError extends ConfigErrorInstances

trait ConfigErrorInstances {
  implicit val configErrorShow: Show[ConfigError] =
    Show.show {
      case ConfigError(key, Unmarshaller.Error.MissingEnvValue)                     ⇒ s"Config error, required environment variable ${key} is missing"
      case ConfigError(key, Unmarshaller.Error.InvalidEnvValue(provided, expected)) ⇒ s"Config error, invalid ${key} environment variable. Expected ${expected}, got ${provided}"
    }
}
