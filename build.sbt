enablePlugins(JavaServerAppPackaging)

organization  := "org.phenoscape"

name          := "owlery"

version       := "0.12"

packageDescription in Debian := "Owlery"

maintainer in Debian := "James Balhoff"

maintainer in Linux := "James Balhoff <balhoff@renci.org>"

packageSummary in Linux := "Owlery OWL server"

packageDescription := "A web api for OWL API reasoners"

daemonUser in Linux := normalizedName.value // user which will execute the application

daemonGroup in Linux := normalizedName.value    // group which will execute the application

scalaVersion  := "2.12.5"

scalacOptions := Seq("-unchecked", "-deprecation", "-encoding", "utf8")

javaOptions += "-Xmx10G"

libraryDependencies ++= {
  Seq(
    "com.typesafe.akka"      %% "akka-http"              % "10.0.6",
    "com.typesafe.akka"      %% "akka-http-spray-json"   % "10.0.4",
    "ch.megard"              %% "akka-http-cors"         % "0.2.1",
    "io.spray"               %% "spray-json"             % "1.3.3",
    "net.sourceforge.owlapi" %  "owlapi-distribution"    % "4.5.2",
    "org.semanticweb.elk"    %  "elk-owlapi"             % "0.4.3",
    "net.sourceforge.owlapi" %  "org.semanticweb.hermit" % "1.4.0.432",
    "net.sourceforge.owlapi" %  "jfact"                  % "4.0.4",
    "org.phenoscape"         %% "owlet"                  % "1.6",
    "commons-io"             %  "commons-io"             % "2.4",
    "org.apache.jena"        %  "apache-jena-libs"       % "3.2.0"
  )
}

Revolver.settings
