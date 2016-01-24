import play.twirl.sbt.Import.TwirlKeys
import sbt.Keys._
import sbt._

object ReactiveTodosBuild extends Build {

  object V {
    val play = "2.4.4"
    val phantom = "1.12.2"
    val opRabbit = "1.2.1"
    val akkaStreams = "2.0.1"
  }

  val appName         = "reactive-todos"
  val appVersion      = "1.0.0-SNAPSHOT"

  val appDependencies = Seq(
    "com.websudos"      %% "phantom-dsl"           % V.phantom  withSources(),
    "com.typesafe.akka" %% "akka-slf4j"            % "2.4.0",
    "com.eaio.uuid"     %  "uuid"                  % "3.2"      withSources(),
    "com.spingo"        %% "op-rabbit-core"        % V.opRabbit withSources(),
    "com.spingo"        %% "op-rabbit-play-json"   % V.opRabbit withSources(),
    "com.spingo"        %% "op-rabbit-json4s"      % V.opRabbit withSources(),
    "com.spingo"        %% "op-rabbit-airbrake"    % V.opRabbit withSources(),
    "com.spingo"        %% "op-rabbit-akka-stream" % V.opRabbit
  )

  val applicationSettings: Seq[Setting[_]] = Seq(
    version := appVersion,
    libraryDependencies ++= appDependencies,
    scalaVersion        := "2.11.6",
    resolvers           ++= Seq("SpinGo OSS" at "http://spingo-oss.s3.amazonaws.com/repositories/releases", Resolver.mavenLocal),
    scalacOptions       := Seq("-unchecked", "-deprecation", "-feature", "-language:implicitConversions", "-language:postfixOps"),
    javaOptions in Test += "-Dconfig.file=conf/test-application.conf",
    fork in test := true,
    javaOptions in test += "-XX:MaxPermSize=512M -Xmx1024M -Xms1024M -Duser.timezone=UTC -Djava.library.path=/usr/local/lib",
    TwirlKeys.templateImports += "controllers._",
    resourceDirectory in Test <<= baseDirectory apply { (baseDir: File) => baseDir / "test" / "resources" }
  )

  val main = Project(appName, file("."))
    .enablePlugins(play.sbt.Play)
    .settings(applicationSettings: _*)
}
