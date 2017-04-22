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
import org.semanticweb.owlapi.reasoner.InferenceType
import scala.concurrent.ExecutionContext.Implicits.global

object Main extends App with SimpleRoutingApp with CORSDirectives {

  implicit val system = ActorSystem("owlery-system")
  val factory = OWLManager.getOWLDataFactory

  implicit object IRIValue extends Deserializer[String, IRI] {

    def apply(text: String): Deserialized[IRI] = Right(IRI.create(text))

  }

  implicit object SimpleMapFromJSONString extends Deserializer[String, Map[String, String]] {

    def apply(text: String): Deserialized[Map[String, String]] = text.parseJson match {
      case o: JsObject => Right(o.fields.map { case (key, value) => key -> value.toString })
      case _           => deserializationError("JSON object expected")
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

  val NoPrefixes = Map[String, String]()

  def objectAndPrefixParametersToClass(subroute: OWLClassExpression => (RequestContext => Unit)): RequestContext => Unit =
    parameters('object, 'prefixes.as[Map[String, String]].?(NoPrefixes)).as(PrefixedManchesterClassExpression) { ce =>
      subroute(ce.expression)
    }

  def initializeReasoners() = Owlery.kbs.values.foreach(_.reasoner.precomputeInferences(InferenceType.CLASS_HIERARCHY))

  initializeReasoners()

  val conf = ConfigFactory.load()
  val serverPort = conf.getInt("owlery.port")
  val serverHost = conf.getString("owlery.host")

  if(serverHost == null || serverHost.isEmpty()) {
    serverHost = "localhost"
  }

  startServer(interface = serverHost, port = serverPort) {

    corsFilter(List("*")) {
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
  }

}
