import java.io.PrintWriter

import sbt._
import Keys._
import play.PlayImport.PlayKeys._

object ReactiveTodosBuild extends Build {

  def writeToFile(fileName: String, value: String) = {
    val file = new PrintWriter(new File(fileName))
    try { file.print(value) } finally { file.close() }
  }

  object V {
    val phantom = "1.8.9"
    val play = "2.4.0"
  }

  val appName         = "reactive-todos"
  val appVersion      = "2.1.0-SNAPSHOT"

  val appDependencies = Seq(
    "com.websudos"  %% "phantom-dsl" % V.phantom,
    "com.websudos"  %% "phantom-testkit" % V.phantom
  )

  val applicationSettings: Seq[Setting[_]] = Seq(
    version := appVersion,
    libraryDependencies ++= appDependencies,
    scalaVersion        := "2.11.6",
    resolvers           += Resolver.mavenLocal,
//    routesImport        ++= Seq("extensions.Binders._", "java.util.UUID"),
    scalacOptions       := Seq("-unchecked", "-deprecation", "-feature", "-language:implicitConversions", "-language:postfixOps"),
    javaOptions in Test += "-Dconfig.file=conf/test-application.conf",
    fork in test := true,
    javaOptions in test += "-XX:MaxPermSize=512M -Xmx1024M -Xms1024M -Duser.timezone=UTC -Djava.library.path=/usr/local/lib",
    resourceDirectory in Test <<= (baseDirectory) apply {(baseDir: File) => baseDir / "test" / "resources"}
  )

  val main = Project(appName, file("."))
    .enablePlugins(play.PlayScala)
    .settings(applicationSettings: _*)

  val appVersionWithHash = "%s-%s-%s".format(appVersion,
    "git rev-parse --abbrev-ref HEAD".!!.trim, "git rev-parse --short HEAD".!!.trim)

  writeToFile("conf/app.version", appVersionWithHash)
}
