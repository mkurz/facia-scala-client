import Dependencies._
import sbtrelease.ReleaseStateTransformations._

organization := "com.gu"

name := "facia-api-client"

description := "Scala client for The Guardian's Facia JSON API"

val sonatypeReleaseSettings = Seq(
  licenses := Seq("Apache V2" -> url("http://www.apache.org/licenses/LICENSE-2.0.html")),
  scmInfo := Some(ScmInfo(
    url("https://github.com/guardian/facia-scala-client"),
    "scm:git:git@github.com:guardian/facia-scala-client.git"
  )),
  pomExtra := (
    <url>https://github.com/guardian/facia-scala-client</url>
      <developers>
        <developer>
          <id>janua</id>
          <name>Francis Carr</name>
          <url>https://github.com/janua</url>
        </developer>
        <developer>
          <id>adamnfish</id>
          <name>Adam Fisher</name>
          <url>https://github.com/adamnfish</url>
        </developer>
      </developers>
    ),
  releaseCrossBuild := true, // true if you cross-build the project for multiple Scala versions
  releaseProcess := Seq[ReleaseStep](
    checkSnapshotDependencies,
    inquireVersions,
    runClean,
    runTest,
    setReleaseVersion,
    commitReleaseVersion,
    tagRelease,
    // For non cross-build projects, use releaseStepCommand("publishSigned")
    releaseStepCommandAndRemaining("+publishSigned"),
    releaseStepCommand("sonatypeBundleRelease"),
    setNextVersion,
    commitNextVersion,
    pushChanges
  )
)

lazy val root = (project in file(".")).aggregate(
    fapiClient_core,
    fapiClient_s3_sdk_v2,
    faciaJson_play27,
    faciaJson_play28,
    fapiClient_play27,
    fapiClient_play28
  ).settings(
    publishArtifact := false,
    publish / skip := true,
    sonatypeReleaseSettings
  )

val exactPlayJsonVersions = Map(
  "27" -> "2.7.4",
  "28" -> "2.8.2"
)

val baseSettings = Seq(
  organization := "com.gu",
  resolvers ++= Resolver.sonatypeOssRepos("releases"),
  scalaVersion := "2.13.11",
  crossScalaVersions := Seq(scalaVersion.value, "2.12.18"),
  scalacOptions := Seq(
    "-feature",
    "-deprecation",
    "-Xfatal-warnings"
  ),
  libraryDependencies += scalaTest,
  publishTo := sonatypePublishToBundle.value
) ++ sonatypeReleaseSettings

def playSpecificProject(module: String, majorMinorVersion: String) = Project(s"$module-play$majorMinorVersion", file(s"$module-play$majorMinorVersion"))
  .settings(
    sourceDirectory := baseDirectory.value / s"../$module/src",
    baseSettings
  )

def faciaJson_playJsonVersion(majorMinorVersion: String) = playSpecificProject("facia-json", majorMinorVersion)
  .dependsOn(fapiClient_core)
  .settings(
    libraryDependencies ++= Seq(
      awsS3SdkV1, // ideally, this would be pushed out to a separate FAPI artifact
      commonsIo,
      "com.typesafe.play" %% "play-json" % exactPlayJsonVersions(majorMinorVersion),
      "org.scala-lang.modules" %% "scala-collection-compat" % "2.11.0",
      scalaLogging
    )
  )

def fapiClient_playJsonVersion(majorMinorVersion: String) =  playSpecificProject("fapi-client", majorMinorVersion)
  .settings(
    libraryDependencies ++= Seq(
      contentApi,
      contentApiDefault,
      commercialShared,
      scalaTestMockito,
      mockito
    )
  )

lazy val fapiClient_core = Project("fapi-client-core", file("fapi-client-core")).settings(
  libraryDependencies += eTagCachingS3Base,
  baseSettings
)

lazy val fapiClient_s3_sdk_v2 = Project("fapi-s3-sdk-v2", file("fapi-s3-sdk-v2")).dependsOn(fapiClient_core)
  .settings(
  libraryDependencies += eTagCachingS3SdkV2,
  baseSettings
)

lazy val faciaJson_play27 = faciaJson_playJsonVersion("27")
lazy val faciaJson_play28 = faciaJson_playJsonVersion("28")

lazy val fapiClient_play27 = fapiClient_playJsonVersion("27").dependsOn(faciaJson_play27)
lazy val fapiClient_play28 = fapiClient_playJsonVersion("28").dependsOn(faciaJson_play28)

Test/testOptions += Tests.Argument(
  TestFrameworks.ScalaTest,
  "-u", s"test-results/scala-${scalaVersion.value}", "-o"
)
