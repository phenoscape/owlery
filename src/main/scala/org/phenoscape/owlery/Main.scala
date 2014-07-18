package org.phenoscape.owlery

import scala.collection.JavaConversions._
import scala.util.Right
import org.semanticweb.owlapi.apibinding.OWLManager
import org.semanticweb.owlapi.model.IRI
import com.hp.hpl.jena.query.Query
import com.hp.hpl.jena.query.QueryException
import com.hp.hpl.jena.query.QueryFactory
import akka.actor.ActorSystem
import spray.http._
import spray.httpx.unmarshalling._
import spray.routing.Directive.pimpApply
import spray.routing.SimpleRoutingApp
import spray.routing.directives.ParamDefMagnet.apply
import java.io.InputStreamReader
import java.io.ByteArrayInputStream

object Main extends App with SimpleRoutingApp {

  implicit val system = ActorSystem("owlery-system")
  val factory = OWLManager.getOWLDataFactory

  implicit object IRIValue extends Deserializer[String, IRI] {

    def apply(text: String): Deserialized[IRI] = Right(IRI.create(text))

  }
  val `application/sparql-query` = MediaTypes.register(MediaType.custom("application/sparql-query"))
  implicit object SPARQLQueryUnmarshaller extends Deserializer[HttpEntity, Query] {

    def apply(entity: HttpEntity): Deserialized[Query] = entity match {
      case HttpEntity.NonEmpty(`application/sparql-query`, data) => SPARQLQueryValue(data.asString(HttpCharsets.`UTF-8`))
      case HttpEntity.NonEmpty(otherContentType, data) => Left(MalformedContent("Unsupported content type"))
      case HttpEntity.Empty => Left(MalformedContent("Empty query"))
    }

  }

  implicit object SPARQLQueryValue extends Deserializer[String, Query] {

    def apply(text: String): Deserialized[Query] = try {
      Right(QueryFactory.create(text))
    } catch {
      case e: QueryException => Left(MalformedContent(e.getMessage, e))
    }

  }

  def initializeReasoners() = Owlery.kbs.values.foreach(_.reasoner.isConsistent)

  initializeReasoners()

  startServer(interface = "localhost", port = 8080) {

    pathPrefix("kb" / Segment) { kbName =>
      Owlery.kb(kbName) match {
        case None => reject
        case Some(kb) => {
          path("subclasses") {
            parameters('object.as[IRI], 'prefixes.?, 'direct ? true) { (owlObject, prefixes, direct) =>
              complete {
                val subclasses = kb.reasoner.getSubClasses(factory.getOWLClass(owlObject), direct).getFlattened
                subclasses.map(_.getIRI.toString).mkString("\n")
              }
            }
          } ~
            path("superclasses") {
              parameters('object.as[IRI], 'prefixes.?, 'direct ? true) { (owlObject, prefixes, direct) =>
                complete {
                  val superclasses = kb.reasoner.getSuperClasses(factory.getOWLClass(owlObject), direct).getFlattened
                  superclasses.map(_.getIRI.toString).mkString("\n")
                }
              }
            } ~
            path("equivalent") {
              parameters('object.as[IRI], 'prefixes.?) { (owlObject, prefixes) =>
                complete {
                  val equivalents = kb.reasoner.getEquivalentClasses(factory.getOWLClass(owlObject)).getEntities
                  equivalents.map(_.getIRI.toString).mkString("\n")
                }
              }
            } ~
            path("satisfiable") {
              parameters('object.as[IRI], 'prefixes.?) { (owlObject, prefixes) =>
                complete {
                  kb.reasoner.isSatisfiable(factory.getOWLClass(owlObject)).toString
                }
              }
            } ~
            path("types") {
              parameters('object.as[IRI], 'prefixes.?, 'direct ? true) { (owlObject, prefixes, direct) =>
                complete {
                  val types = kb.reasoner.getTypes(factory.getOWLNamedIndividual(owlObject), direct).getFlattened
                  types.map(_.getIRI.toString).mkString("\n")
                }
              }
            } ~
            path("sparql") {
              get {
                parameter('query.as[Query]) { query =>
                  complete {
                    kb.performSPARQLQuery(query)
                  }
                }
              } ~
                post {
                  handleWith(kb.performSPARQLQuery)
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