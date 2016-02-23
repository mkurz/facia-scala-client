import sbt._

object Dependencies {
  val awsSdk = "com.amazonaws" % "aws-java-sdk-s3" % "1.10.54"
  val commonsIo = "org.apache.commons" % "commons-io" % "1.3.2"
  val contentApi = "com.gu" %% "content-api-client" % "7.27"
  val mockito = "org.mockito" % "mockito-all" % "1.10.19" % "test"
  val playJson = "com.typesafe.play" %% "play-json" % "2.4.6"
  val scalaTest = "org.scalatest" %% "scalatest" % "2.2.6" % "test"
  val specs2 = "org.specs2" %% "specs2" % "3.7" % "test"
}
