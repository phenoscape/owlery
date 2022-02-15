enablePlugins(JavaServerAppPackaging)

organization  := "org.phenoscape"

name          := "owlery"

version       := "0.16"

packageDescription in Debian := "Owlery"

maintainer in Debian := "James Balhoff"

maintainer in Linux := "James Balhoff <balhoff@renci.org>"

packageSummary in Linux := "Owlery OWL server"

packageDescription := "A web api for OWL API reasoners"

daemonUser in Linux := normalizedName.value // user which will execute the application

daemonGroup in Linux := normalizedName.value    // group which will execute the application

scalaVersion  := "2.13.6"

scalacOptions := Seq("-unchecked", "-deprecation", "-encoding", "utf8")

javaOptions += "-Xmx10G"

testFrameworks += new TestFramework("utest.runner.Framework")

libraryDependencies ++= {
  Seq(
    "com.typesafe.akka"      %% "akka-stream"            % "2.6.12",
    "com.typesafe.akka"      %% "akka-actor"             % "2.6.12",
    "com.typesafe.akka"      %% "akka-slf4j"             % "2.6.12",
    "com.typesafe.akka"      %% "akka-http"              % "10.2.6",
    "com.typesafe.akka"      %% "akka-http-spray-json"   % "10.2.6",
    "ch.megard"              %% "akka-http-cors"         % "1.1.2",
    "io.spray"               %% "spray-json"             % "1.3.5",
    "net.sourceforge.owlapi" %  "owlapi-distribution"    % "4.5.20" exclude("org.slf4j", "slf4j-log4j12"),
    "org.semanticweb.elk"    %  "elk-owlapi"             % "0.4.3" exclude("log4j", "log4j"),
    "net.sourceforge.owlapi" %  "org.semanticweb.hermit" % "1.4.3.456",
    "net.sourceforge.owlapi" %  "jfact"                  % "4.0.4",
    "org.geneontology"       %% "whelk-owlapi"           % "1.1.1",
    "org.phenoscape"         %% "owlet"                  % "1.9" exclude("org.slf4j", "slf4j-log4j12"),
    "commons-io"             %  "commons-io"             % "2.11.0",
    "org.apache.jena"        %  "apache-jena-libs"       % "4.1.0" exclude("org.slf4j", "slf4j-log4j12"),
    "org.obolibrary.robot"   %  "robot-core"             % "1.8.3" exclude("org.slf4j", "slf4j-log4j12"),
    "ch.qos.logback"         %  "logback-classic"        % "1.2.3",
    "com.lihaoyi"            %% "utest"                  % "0.7.10" % Test
  )
}

Revolver.settings
