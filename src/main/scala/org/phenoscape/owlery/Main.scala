package org.phenoscape.owlery

import spray.routing.SimpleRoutingApp
import akka.actor.ActorSystem

object Main extends App with SimpleRoutingApp {

  implicit val system = ActorSystem("owlery-system")

  startServer(interface = "localhost", port = 8080) {
    pathPrefix("kb" / Segment) { kbName =>
      parameters('object, 'prefixes.?, 'direct ? true) { (owlObject, prefixes, direct) =>
        path("subclasses") {
          complete {
            kbName + s": $direct subclasses of " + owlObject
          }
        } ~
          path("superclasses") {
            complete {
              kbName + s": $direct superclasses of " + owlObject
            }
          } ~
          path("equivalent") {
            complete {
              kbName + ": equivalent to " + owlObject
            }
          } ~
          path("satisfiable") {
            complete {
              kbName + ": " + owlObject + " satisfiable?"
            }
          } ~
          path("types") {
            complete {
              kbName + s": $direct types for " + owlObject
            }
          }
      } ~
        pathEnd {
          complete {
            kbName
          }
        }
    }
  }

}