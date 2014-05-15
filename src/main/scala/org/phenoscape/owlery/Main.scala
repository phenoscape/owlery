package org.phenoscape.owlery

import spray.routing.SimpleRoutingApp
import akka.actor.ActorSystem

object Main extends App with SimpleRoutingApp {

  implicit val system = ActorSystem("my-system")

  startServer(interface = "localhost", port = 8080) {
    path("hello") {
      get {
        complete {
          <h1>Say hello to spray</h1>
        }
      }
    }
  }

}