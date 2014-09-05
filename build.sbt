import com.typesafe.sbt.SbtNativePackager._
import NativePackagerKeys._

organization  := "org.phenoscape"

name          := "owlery"

version       := "0.9"

packageArchetype.java_server

packageDescription in Debian := "Owlery"

maintainer in Debian := "James Balhoff"

maintainer in Linux := "James Balhoff <balhoff@nescent.org>"

packageSummary in Linux := "Owlery OWL server"

packageDescription := "A web api for OWL API reasoners"

daemonUser in Linux := normalizedName.value // user which will execute the application

daemonGroup in Linux := normalizedName.value    // group which will execute the application

scalaVersion  := "2.10.3"

scalacOptions := Seq("-unchecked", "-deprecation", "-encoding", "utf8")

resolvers += "Phenoscape Maven repository" at "http://phenoscape.svn.sourceforge.net/svnroot/phenoscape/trunk/maven/repository"

libraryDependencies ++= {
  val akkaV = "2.3.0"
  val sprayV = "1.3.1"
  Seq(
    "io.spray"               %   "spray-can"           % sprayV,
    "io.spray"               %   "spray-routing"       % sprayV,
    "io.spray"               %   "spray-json_2.10"     % "1.2.6",
    "io.spray"               %   "spray-testkit"       % sprayV  % "test",
    "com.typesafe.akka"      %%  "akka-actor"          % akkaV,
    "com.typesafe.akka"      %%  "akka-testkit"        % akkaV   % "test",
    "org.specs2"             %%  "specs2-core"         % "2.3.7" % "test",
    "net.sourceforge.owlapi" %   "owlapi-distribution" % "3.5.0",
    "org.semanticweb.elk"    %   "elk-owlapi"          % "0.4.1",
    "org.phenoscape"         %   "owlet"               % "1.1.5",
    "commons-io"             %   "commons-io"          % "2.4",
    "org.apache.jena"        %   "apache-jena-libs"    % "2.11.2"
  )
}

Revolver.settings
