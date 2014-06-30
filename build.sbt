organization  := "org.phenoscape"

name          := "owlery"

version       := "0.1"

scalaVersion  := "2.10.3"

scalacOptions := Seq("-unchecked", "-deprecation", "-encoding", "utf8")

libraryDependencies ++= {
  val akkaV = "2.3.0"
  val sprayV = "1.3.1"
  Seq(
    "io.spray"               %   "spray-can"           % sprayV,
    "io.spray"               %   "spray-routing"       % sprayV,
    "io.spray"               %   "spray-testkit"       % sprayV  % "test",
    "com.typesafe.akka"      %%  "akka-actor"          % akkaV,
    "com.typesafe.akka"      %%  "akka-testkit"        % akkaV   % "test",
    "org.specs2"             %%  "specs2-core"         % "2.3.7" % "test",
    "net.sourceforge.owlapi" %   "owlapi-distribution" % "3.5.0",
    "commons-io"             %   "commons-io"          % "2.4"
  )
}

Revolver.settings
