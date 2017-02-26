name := "scala-aws-hosted-es"

version := "1.0"

scalaVersion := "2.11.8"

resolvers ++= Seq(
  Resolver.jcenterRepo
)

libraryDependencies ++= {
  Seq(
    "ch.qos.logback" % "logback-classic" % "1.1.8",
    "com.gilt" %% "gfc-guava" % "0.2.5",
    "com.sksamuel.elastic4s" %% "elastic4s-http" % "5.2.8",
    "com.sksamuel.elastic4s" %% "elastic4s-jackson" % "5.2.8",
    "com.typesafe" % "config" % "1.3.0",
    "org.slf4j" % "jcl-over-slf4j" % "1.7.21",
    "org.slf4j" % "log4j-over-slf4j" % "1.7.21",
    "org.scalatest" %% "scalatest" % "3.0.0" % Test,
    "vc.inreach.aws" % "aws-signing-request-interceptor" % "0.0.16"
  )
}
