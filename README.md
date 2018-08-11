# Simple-env-config
Simple-env-config is a small, lightweight strongly typed configuration library for Scala biased towards Docker microservices that use environment variables for configuration following [Twelve-Factor App](https://12factor.net/) principals.

While Simple-env-config is designed around system envrironment variables it is compatible with any input source where you can generate a `Map[String, String]`.

## Why use Simple-env-config
Other Scala configuration libraries promote some bad practices:

- Passing around untyped configuration files where `config.get("Key")` can result in runtime exceptions. 
- Hard to reason about configuration values. Merging multiple files, JVM properties and system environment variables makes it hard to understand the final configuration values.
- With Docker and environment variables config files are used as a thin mapping essentially a thin mapping to `sys.env` which means essentially declaring config multiple times:
`application.conf`
```
aws-api-key=${AWS_API_KEY}
aws-api-secret=${AWS_API_SECRET}
```

`docker.compose`
```
version: "3"

services:
  myservice:
    image: mymicroservice:latest
    environment:
      - AWS_API_KEY:some-value
      - AWS_API_SECRET:some-value
```

`MyApp.scala`
```
Config conf = ConfigFactory.load();
val apiKey = conf.getString("AWS_API_KEY"); // BOOM runtime exception if env variable not set
```

Simple-env-config solves some of these issues by:
- Safely loading config eagerly providing a `NonEmptyList[ConfigError] \/ A` where `A` is a validated real type.
- Clean error messages
- No duplicating config definitions to do `sys.env`
- Reporting of captured values
- Providing a simple set of combinators

Example usage:
```
case class Config(awsApiKey: String, awsSecret: String)

object Config {
  def load (ConfigReport, NonEmptyList[ConfigError] \/ Config) =
    (ConfigAction.read[String]("AWS_API_KEY") |@| 
    ConfigAction.read[String]("AWS_API_SECRET")(Config.apply _).disjunctioned(sys.env)
}

object MyApp extends Application {
  val (configReport, config) = Config.load
  
  config match {
    case -\/(errors) =>
      println(errors)
      sys.exit(1)
    case \/-(c) =>
      println(configReport.prettyPrint)
      ....
  }

}
``` 

`ConfigReport` contains all successfully read values and provides functions for printing them as shown above. 
`prettyPrintMaskPasswords: String` and `prettyPrintWithMask(maskPredicate: String => Boolean): String` allow masking of sensitive data.
