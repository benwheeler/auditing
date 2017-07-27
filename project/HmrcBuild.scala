/*
 * Copyright 2017 HM Revenue & Customs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import sbt._
import sbt.Keys._
//import uk.gov.hmrc.versioning.SbtGitVersioning

object HmrcBuild extends Build {

//  import uk.gov.hmrc._

  val appName = "auditing"

  lazy val microservice: Project = Project(appName, file("."))
//    .enablePlugins(SbtAutoBuildPlugin, SbtGitVersioning)
    .settings(
      libraryDependencies ++= AppDependencies(),
      scalaVersion := "2.11.7",
      resolvers := Seq(
        Resolver.bintrayRepo("hmrc", "releases"),
        "typesafe-releases" at "http://repo.typesafe.com/typesafe/releases/"
      ),
      unmanagedResourceDirectories in Test += baseDirectory.value / "src" / "test" / "scala" / "assets"
    )
}

private object AppDependencies {

  val compile = Seq(
    "uk.gov.hmrc" %% "http-core" % "0.4.0",
    "org.json4s" %% "json4s-native" % "3.5.1",
    "org.json4s" %% "json4s-ext" % "3.5.1"
  )

  trait TestDependencies {
    lazy val scope: String = "test"
    lazy val test: Seq[ModuleID] = ???
  }

  object Test {
    def apply(): Seq[ModuleID] = new TestDependencies {
      override lazy val test = Seq(
        "com.github.tomakehurst" % "wiremock" % "1.52" % scope,
        "org.mockito" % "mockito-all" % "1.10.19" % scope,
        "org.specs2" %% "specs2-core" % "3.9.1" % scope,
        "org.specs2" %% "specs2-mock" % "3.9.1" % scope
      )
    }.test
  }

  def apply(): Seq[ModuleID] = compile ++ Test()
}
