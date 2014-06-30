package org.phenoscape.owlery

import org.semanticweb.owlapi.model.IRI
import akka.actor.ActorSystem
import spray.httpx.marshalling.ToResponseMarshallable.isMarshallable
import spray.httpx.unmarshalling.Deserializer
import spray.httpx.unmarshalling.Deserialized
import spray.routing.Directive.pimpApply
import spray.routing.SimpleRoutingApp
import spray.routing.directives.ParamDefMagnet.apply
import scala.util.Right
import spray.routing.ValidationRejection
import spray.httpx.unmarshalling.MalformedContent
import org.semanticweb.owlapi.apibinding.OWLManager
import scala.collection.JavaConversions._

object Main extends App with SimpleRoutingApp {

  implicit val system = ActorSystem("owlery-system")
  val factory = OWLManager.getOWLDataFactory

  implicit object IRIValue extends Deserializer[String, IRI] {

    def apply(text: String): Deserialized[IRI] = Right(IRI.create(text))

  }

  // this doesn't seem useful so far
  implicit object KBValue extends Deserializer[String, Knowledgebase] {

    def apply(text: String): Deserialized[Knowledgebase] = Owlery.kb(text) match {
      case None => Left(MalformedContent(s"No such knowledgebase: $text"))
      case Some(kb) => Right(kb)
    }

  }

  startServer(interface = "localhost", port = 8080) {

    pathPrefix("kb" / Segment) { kbName =>
      Owlery.kb(kbName) match {
        case None => reject {
          ValidationRejection(s"No such knowledgebase: $kbName")
        }
        case Some(kb) => {
          parameters('object.as[IRI], 'prefixes.?, 'direct ? true) { (owlObject, prefixes, direct) =>
            path("subclasses") {
              complete {
                val subclasses = kb.reasoner.getSubClasses(factory.getOWLClass(owlObject), direct).getFlattened
                subclasses.map(_.getIRI.toString).mkString("\n")
              }
            } ~
              path("superclasses") {
                complete {
                  val superclasses = kb.reasoner.getSuperClasses(factory.getOWLClass(owlObject), direct).getFlattened
                  superclasses.map(_.getIRI.toString).mkString("\n")
                }
              } ~
              path("equivalent") {
                complete {
                  val equivalents = kb.reasoner.getEquivalentClasses(factory.getOWLClass(owlObject)).getEntities
                  equivalents.map(_.getIRI.toString).mkString("\n")
                }
              } ~
              path("satisfiable") {
                complete {
                  kb.reasoner.isSatisfiable(factory.getOWLClass(owlObject)).toString
                }
              } ~
              path("types") {
                complete {
                  val types = kb.reasoner.getTypes(factory.getOWLNamedIndividual(owlObject), direct).getFlattened
                  types.map(_.getIRI.toString).mkString("\n")
                }
              }
          } ~
            path("sparql") {
              complete {
                kbName + " owlet SPARQL endpoint"
              }
            } ~
            pathEnd {
              complete {
                val consistent = if (kb.reasoner.isConsistent) "consistent" else "inconsistent"
                s"Knowledgebase $kbName is $consistent"
              }
            }
        }
      }
    }
  }

}