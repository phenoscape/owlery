package org.phenoscape.owlery

import scala.collection.JavaConversions._
import scala.util.Right
import org.semanticweb.owlapi.apibinding.OWLManager
import org.semanticweb.owlapi.model.IRI
import org.semanticweb.owlapi.model.OWLClassExpression
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
import org.phenoscape.owlery.SPARQLFormats._
import org.phenoscape.owlery.OWLFormats._
import com.typesafe.config.ConfigFactory
import spray.httpx.SprayJsonSupport._
import spray.json._
import spray.json.DefaultJsonProtocol._
import scala.collection.immutable.Map
import org.phenoscape.owlet.ManchesterSyntaxClassExpressionParser
import spray.routing.Directive
import spray.routing.RequestContext

object Main extends App with SimpleRoutingApp {

  implicit val system = ActorSystem("owlery-system")
  val factory = OWLManager.getOWLDataFactory

  implicit object IRIValue extends Deserializer[String, IRI] {

    def apply(text: String): Deserialized[IRI] = Right(IRI.create(text))

  }

  implicit object SimpleMapFromJSONString extends Deserializer[String, Map[String, String]] {

    def apply(text: String): Deserialized[Map[String, String]] = text.parseJson match {
      case o: JsObject => Right(o.fields.map { case (key, value) => key -> value.toString })
      case _ => deserializationError("JSON object expected")
    }

  }

  case class PrefixedManchesterClassExpression(text: String, prefixes: Map[String, String]) {

    val parseResult = ManchesterSyntaxClassExpressionParser.parse(text, prefixes)
    require(parseResult.isSuccess, parseResult.swap.getOrElse("Error parsing class expression"))
    val expression = parseResult.toOption.get

  }

  case class PrefixedIndividualIRI(text: String, prefixes: Map[String, String]) {

    val parseResult = ManchesterSyntaxClassExpressionParser.parseIRI(text, prefixes)
    require(parseResult.isSuccess, parseResult.swap.getOrElse("Error parsing individual IRI"))
    val iri = parseResult.toOption.get

  }

  val NoPrefixes = Map[String, String]()

  def objectAndPrefixParametersToClass(subroute: OWLClassExpression => (RequestContext => Unit)): RequestContext => Unit =
    parameters('object, 'prefixes.as[Map[String, String]].?(NoPrefixes)).as(PrefixedManchesterClassExpression) { ce =>
      subroute(ce.expression)
    }

  def initializeReasoners() = Owlery.kbs.values.foreach(_.reasoner.isConsistent)

  initializeReasoners()

  val conf = ConfigFactory.load()
  val serverPort = conf.getInt("owlery.port")

  startServer(interface = "localhost", port = serverPort) {

    pathPrefix("kbs" / Segment) { kbName =>
      Owlery.kb(kbName) match {
        case None => reject
        case Some(kb) => {
          path("subclasses") {
            objectAndPrefixParametersToClass { expression =>
              parameters('direct.?(false)) { direct =>
                detach() {
                  complete {
                    kb.querySubClasses(expression, direct)
                  }
                }
              }
            }
          } ~
            path("superclasses") {
              objectAndPrefixParametersToClass { expression =>
                parameters('direct.?(false)) { direct =>
                  detach() {
                    complete {
                      kb.querySuperClasses(expression, direct)
                    }
                  }
                }
              }
            } ~
            path("instances") {
              objectAndPrefixParametersToClass { expression =>
                parameters('direct.?(false)) { direct =>
                  detach() {
                    complete {
                      kb.queryInstances(expression, direct)
                    }
                  }
                }
              }
            } ~
            path("equivalent") {
              objectAndPrefixParametersToClass { expression =>
                detach() {
                  complete {
                    kb.queryEquivalentClasses(expression)
                  }
                }
              }
            } ~
            path("satisfiable") {
              objectAndPrefixParametersToClass { expression =>
                detach() {
                  complete {
                    kb.isSatisfiable(expression)
                  }
                }
              }
            } ~
            path("types") {
              parameters('object, 'prefixes.as[Map[String, String]].?(NoPrefixes)).as(PrefixedIndividualIRI) { preIRI =>
                parameters('direct.?(true)) { direct =>
                  detach() {
                    complete {
                      kb.queryTypes(factory.getOWLNamedIndividual(preIRI.iri), direct)
                    }
                  }
                }
              }
            } ~
            path("sparql") {
              get {
                parameter('query.as[Query]) { query =>
                  detach() {
                    complete {
                      kb.performSPARQLQuery(query)
                    }
                  }
                }
              } ~
                post {
                  detach() {
                    handleWith(kb.performSPARQLQuery)
                  }
                }
            } ~
            path("expand") {
              get {
                parameter('query.as[Query]) { query =>
                  detach() {
                    complete {
                      kb.expandSPARQLQuery(query)
                    }
                  }
                }
              } ~
                post {
                  detach() {
                    handleWith(kb.expandSPARQLQuery)
                  }
                }
            } ~
            pathEnd {
              detach() {
                complete {
                  kb.summary
                }
              }
            }
        }
      }
    }
  }

}