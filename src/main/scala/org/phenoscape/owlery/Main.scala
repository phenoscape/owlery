package org.phenoscape.owlery

import scala.collection.immutable.Map

import org.phenoscape.owlery.Owlery.OwleryMarshaller
import org.phenoscape.owlery.SPARQLFormats._
import org.phenoscape.owlet.ManchesterSyntaxClassExpressionParser
import org.semanticweb.owlapi.apibinding.OWLManager
import org.semanticweb.owlapi.model.IRI
import org.semanticweb.owlapi.model.OWLClassExpression
import org.semanticweb.owlapi.reasoner.InferenceType

import com.typesafe.config.ConfigFactory

import akka.actor.ActorSystem
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport.sprayJsonMarshaller
import akka.http.scaladsl.server.HttpApp
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.unmarshalling.Unmarshaller
import ch.megard.akka.http.cors.scaladsl.CorsDirectives._
import spray.json._
import spray.json.DefaultJsonProtocol._
import org.apache.jena.query.Query

object Main extends HttpApp with App {

  val factory = OWLManager.getOWLDataFactory

  implicit val IRIUnmarshaller: Unmarshaller[String, IRI] = Unmarshaller.strict(IRI.create)

  implicit val SimpleMapFromJSONString: Unmarshaller[String, Map[String, String]] = Unmarshaller.strict { text =>
    text.parseJson match {
      case o: JsObject => o.fields.map { case (key, value) => key -> value.toString }
      case _           => throw new IllegalArgumentException(s"Not a valid JSON map: $text")
    }
  }

  val NullQuery = new Query()

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

  val NoPrefixes = Map.empty[String, String]

  def objectAndPrefixParametersToClass(subroute: OWLClassExpression => (Route)): Route =
    parameters('object, 'prefixes.as[Map[String, String]].?(NoPrefixes)).as(PrefixedManchesterClassExpression) { ce =>
      subroute(ce.expression)
    }

  def initializeReasoners() = Owlery.kbs.values.foreach(_.reasoner.precomputeInferences(InferenceType.CLASS_HIERARCHY))

  initializeReasoners()

  val conf = ConfigFactory.load()
  val port = conf.getInt("owlery.port")
  val host = conf.getString("owlery.host")

  def route: Route = cors() {
    pathPrefix("kbs") {
      pathPrefix(Segment) { kbName =>
        Owlery.kb(kbName) match {
          case None => reject
          case Some(kb) => {
            path("subclasses") {
              objectAndPrefixParametersToClass { expression =>
                parameters('direct.?(false)) { direct =>
                  complete {
                    kb.querySubClasses(expression, direct)
                  }
                }
              }
            } ~
              path("superclasses") {
                objectAndPrefixParametersToClass { expression =>
                  parameters('direct.?(false)) { direct =>
                    complete {
                      kb.querySuperClasses(expression, direct)
                    }
                  }
                }
              } ~
              path("instances") {
                objectAndPrefixParametersToClass { expression =>
                  parameters('direct.?(false)) { direct =>
                    complete {
                      kb.queryInstances(expression, direct)
                    }
                  }
                }
              } ~
              path("equivalent") {
                objectAndPrefixParametersToClass { expression =>
                  complete {
                    kb.queryEquivalentClasses(expression)
                  }
                }
              } ~
              path("satisfiable") {
                objectAndPrefixParametersToClass { expression =>
                  complete {
                    kb.isSatisfiable(expression)
                  }
                }
              } ~
              path("types") {
                parameters('object, 'prefixes.as[Map[String, String]].?(NoPrefixes)).as(PrefixedIndividualIRI) { preIRI =>
                  parameters('direct.?(true)) { direct =>
                    complete {
                      kb.queryTypes(factory.getOWLNamedIndividual(preIRI.iri), direct)
                    }
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
                    parameter('query.as[Query].?(NullQuery)) { query =>
                      query match {
                        case NullQuery => handleWith(kb.performSPARQLQuery)
                        case _ => complete {
                          kb.performSPARQLQuery(query)
                        }
                      }
                    }
                  }
              } ~
              path("expand") {
                get {
                  parameter('query.as[Query]) { query =>
                    complete {
                      kb.expandSPARQLQuery(query)
                    }
                  }
                } ~
                  post {
                    handleWith(kb.expandSPARQLQuery)
                  }
              } ~
              pathEnd {
                complete {
                  kb.summary
                }
              }
          }
        }
      } ~
        pathEnd {
          complete {
            Owlery
          }
        }
    }
  }

  // Starting the server
  Main.startServer(host, port)

}