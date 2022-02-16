package org.phenoscape.owlery

import akka.event.Logging
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport.sprayJsonMarshaller
import akka.http.scaladsl.model.{StatusCodes, Uri}
import akka.http.scaladsl.server.{ExceptionHandler, HttpApp, Route, ValidationRejection}
import akka.http.scaladsl.unmarshalling.Unmarshaller
import ch.megard.akka.http.cors.scaladsl.CorsDirectives._
import com.typesafe.config.ConfigFactory
import org.apache.commons.io.IOUtils
import org.apache.jena.query.Query
import org.apache.jena.sys.JenaSystem
import org.phenoscape.owlery.OWLFormats.{ModuleTypeUnmarshaller, OWLFunctionalSyntaxMarshaller, OWLTextUnmarshaller}
import org.phenoscape.owlery.Owlery.OwleryMarshaller
import org.phenoscape.owlery.PrefixEntityChecker.KnownEntities
import org.phenoscape.owlery.SPARQLFormats._
import org.semanticweb.owlapi.apibinding.OWLManager
import org.semanticweb.owlapi.model.{IRI, OWLClassExpression}
import org.semanticweb.owlapi.reasoner.InferenceType
import spray.json.DefaultJsonProtocol._
import spray.json._
import uk.ac.manchester.cs.owlapi.modularity.ModuleType

object Main extends HttpApp with App {

  JenaSystem.init()

  val factory = OWLManager.getOWLDataFactory

  implicit val IRIUnmarshaller: Unmarshaller[String, IRI] = Unmarshaller.strict(IRI.create)

  implicit val SimpleMapFromJSONString: Unmarshaller[String, Map[String, String]] = Unmarshaller.strict { text =>
    text.parseJson match {
      case o: JsObject => o.fields.map {
        case (key, JsString(value)) => key -> value
        case _                      => throw new IllegalArgumentException(s"Only string values are supported in JSON map: $text")
      }
      case _           => throw new IllegalArgumentException(s"Not a valid JSON map: $text")
    }
  }

  implicit val IRISeqFromJSONString: Unmarshaller[String, Seq[IRI]] = Unmarshaller.strict { text =>
    text.parseJson match {
      case a: JsArray => a.elements.map(_.convertTo[String]).map(IRI.create)
      case _          => throw new IllegalArgumentException(s"Not a valid JSON array: $text")
    }
  }

  val NullQuery = new Query()

  case class PrefixedManchesterClassExpression(text: String, prefixes: Map[String, String], knownEntities: KnownEntities) {

    private val parseResult = ManchesterSyntaxClassExpressionParser.parse(text, prefixes, Some(knownEntities))
    require(parseResult.isRight, parseResult.swap.getOrElse("Error parsing class expression"))
    val expression: OWLClassExpression = parseResult.toOption.get

  }

  case class PrefixedIndividualIRI(text: String, prefixes: Map[String, String]) {

    private val parseResult = ManchesterSyntaxClassExpressionParser.parseIRI(text, prefixes)
    require(parseResult.isRight, parseResult.swap.getOrElse("Error parsing individual IRI"))
    val iri: IRI = parseResult.toOption.get

  }

  def constructPrefixedClassExpresion(knownEntities: KnownEntities): (String, Map[String, String]) => PrefixedManchesterClassExpression =
    (text: String, prefixes: Map[String, String]) => PrefixedManchesterClassExpression(text, prefixes, knownEntities)

  val NoPrefixes = Map.empty[String, String]

  def objectAndPrefixParametersToClass(knownEntities: KnownEntities) = (subroute: OWLClassExpression => Route) =>
    parameters("object", "prefixes".as[Map[String, String]].?(NoPrefixes)).as(constructPrefixedClassExpresion(knownEntities)) { ce =>
      subroute(ce.expression)
    }

  implicit def exceptionHandler: ExceptionHandler = ExceptionHandler {
    case e: IllegalArgumentException =>
      reject(ValidationRejection(e.getMessage, Some(e)))
  }

  def initializeReasoners(): Unit = Owlery.kbs.values.foreach(_.reasoner.precomputeInferences(InferenceType.CLASS_HIERARCHY))

  initializeReasoners()

  val conf = ConfigFactory.load()
  val port = conf.getInt("owlery.port")
  val host = conf.getString("owlery.host")
  val basePath = if (conf.hasPath("owlery.base-path")) conf.getString("owlery.base-path") else "/"

  private val swaggerText = IOUtils
    .toString(this.getClass.getResourceAsStream("/docs/swagger.yaml"), "UTF-8")
    .replace("{{basePathToReplace}}", basePath)

  def routes: Route = Route.seal {
    cors() {
      logRequestResult("", Logging.InfoLevel) {
        pathSingleSlash {
          redirect(Uri("docs/"), StatusCodes.SeeOther)
        } ~ pathPrefix("docs") {
          pathEnd {
            redirect(Uri("docs/"), StatusCodes.MovedPermanently)
          } ~
            pathSingleSlash {
              getFromResource("docs/index.html")
            } ~
            path("swagger.yaml") {
              complete {
                swaggerText
              }
            } ~
            getFromResourceDirectory("docs")
        } ~
          pathPrefix("kbs") {
            pathPrefix(Segment) { kbName =>
              Owlery.kb(kbName) match {
                case None     => reject
                case Some(kb) =>
                  path("subclasses") {
                    objectAndPrefixParametersToClass(kb.knownEntities) { expression =>
                      parameters("direct".?(false), "includeEquivalent".?(false), "includeNothing".?(false), "includeDeprecated".?(true)) { (direct, includeEquivalent, includeNothing, includeDeprecated) =>
                        complete {
                          kb.querySubClasses(expression, direct, includeEquivalent, includeNothing, includeDeprecated)
                        }
                      }
                    }
                  } ~
                    path("superclasses") {
                      objectAndPrefixParametersToClass(kb.knownEntities) { expression =>
                        parameters("direct".?(false), "includeEquivalent".?(false), "includeThing".?(false), "includeDeprecated".?(true)) { (direct, includeEquivalent, includeThing, includeDeprecated) =>
                          complete {
                            kb.querySuperClasses(expression, direct, includeEquivalent, includeThing, includeDeprecated)
                          }
                        }
                      }
                    } ~
                    path("instances") {
                      objectAndPrefixParametersToClass(kb.knownEntities) { expression =>
                        parameters("direct".?(false), "includeDeprecated".?(true)) { (direct, includeDeprecated) =>
                          complete {
                            kb.queryInstances(expression, direct, includeDeprecated)
                          }
                        }
                      }
                    } ~
                    path("equivalent") {
                      objectAndPrefixParametersToClass(kb.knownEntities) { expression =>
                        parameters("includeDeprecated".?(true)) { includeDeprecated =>
                          complete {
                            kb.queryEquivalentClasses(expression, includeDeprecated)
                          }
                        }
                      }
                    } ~
                    path("satisfiable") {
                      objectAndPrefixParametersToClass(kb.knownEntities) { expression =>
                        complete {
                          kb.isSatisfiable(expression)
                        }
                      }
                    } ~
                    path("types") {
                      parameters("object", "prefixes".as[Map[String, String]].?(NoPrefixes)).as(PrefixedIndividualIRI) { preIRI =>
                        parameters("direct".?(true), "includeThing".?(false), "includeDeprecated".?(true)) { (direct, includeThing, includeDeprecated) =>
                          complete {
                            kb.queryTypes(factory.getOWLNamedIndividual(preIRI.iri), direct, includeThing, includeDeprecated)
                          }
                        }
                      }
                    } ~
                    path("extract") {
                      post {
                        parameters("type".as[ModuleType].?(ModuleType.STAR), "ontologies".as[Seq[IRI]].?(Seq.empty[IRI])) { (moduleType, fromIRIs) =>
                          handleWith(kb.extractModuleForOntology(_, moduleType, fromIRIs.toSet))
                        }
                      }
                    } ~
                    path("sparql") {
                      get {
                        parameter("query".as[Query]) { query =>
                          complete {
                            kb.performSPARQLQuery(query)
                          }
                        }
                      } ~
                        post {
                          parameter("query".as[Query].?(NullQuery)) {
                            case NullQuery => handleWith(kb.performSPARQLQuery)
                            case query     => complete {
                              kb.performSPARQLQuery(query)
                            }
                          }
                        }
                    } ~
                    path("expand") {
                      get {
                        parameter("query".as[Query]) { query =>
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

  // Starting the server
  Main.startServer(host, port)

}
