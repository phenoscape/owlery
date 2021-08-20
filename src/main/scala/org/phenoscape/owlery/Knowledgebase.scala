package org.phenoscape.owlery

import org.apache.jena.query.{Query, ResultSet}
import org.phenoscape.owlery.Util.OptionalOption
import org.phenoscape.owlet.Owlet
import org.semanticweb.owlapi.apibinding.OWLManager
import org.semanticweb.owlapi.model
import org.semanticweb.owlapi.model._
import org.semanticweb.owlapi.model.parameters.Imports
import org.semanticweb.owlapi.reasoner.OWLReasoner
import org.semanticweb.owlapi.search.EntitySearcher
import org.semanticweb.owlapi.vocab.{OWL2Datatype, PROVVocabulary}
import spray.json.DefaultJsonProtocol._
import spray.json._
import uk.ac.manchester.cs.owlapi.modularity.{ModuleType, SyntacticLocalityModuleExtractor}

import java.util.{Date, GregorianCalendar, UUID}
import javax.xml.datatype.DatatypeFactory
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.jdk.CollectionConverters._

case class Knowledgebase(name: String, reasoner: OWLReasoner) {

  private val factory = OWLManager.getOWLDataFactory
  private lazy val owlet = new Owlet(this.reasoner)
  private val jsonldContext = Map("@context" -> "https://owlery.phenoscape.org/json/context.jsonld").toJson
  private val ontology = reasoner.getRootOntology
  private val manager = ontology.getOWLOntologyManager

  def performSPARQLQuery(query: Query): Future[ResultSet] = Future {
    owlet.performSPARQLQuery(query)
  }

  def expandSPARQLQuery(query: Query): Future[Query] = Future {
    owlet.expandQuery(query, asValues = true)
  }

  def querySuperClasses(expression: OWLClassExpression, direct: Boolean, includeEquivalent: Boolean, includeThing: Boolean, includeDeprecated: Boolean): Future[JsObject] = Future {
    val superClasses = Map("subClassOf" -> reasoner.getSuperClasses(expression, direct).getFlattened.asScala
      .filterNot(!includeThing && _.isOWLThing)
      .filterNot(!includeDeprecated && isDeprecated(_))
      .map(_.getIRI.toString).toList)
    val json = merge(toQueryObject(expression), superClasses.toJson, jsonldContext)
    if (includeEquivalent) {
      val equivalents = Map("equivalentClass" -> reasoner.getEquivalentClasses(expression).getEntities.asScala
        .filterNot(_ == expression)
        .filterNot(!includeDeprecated && isDeprecated(_))
        .map(_.getIRI.toString).toList)
      merge(json, equivalents.toJson)
    } else json
  }

  def querySubClasses(expression: OWLClassExpression, direct: Boolean, includeEquivalent: Boolean, includeNothing: Boolean, includeDeprecated: Boolean): Future[JsObject] = Future {
    val subClasses = Map("superClassOf" -> reasoner.getSubClasses(expression, direct).getFlattened.asScala
      .filterNot(!includeNothing && _.isOWLNothing)
      .filterNot(!includeDeprecated && isDeprecated(_))
      .map(_.getIRI.toString).toList)
    val json = merge(toQueryObject(expression), subClasses.toJson, jsonldContext)
    if (includeEquivalent) {
      val equivalents = Map("equivalentClass" -> reasoner.getEquivalentClasses(expression).getEntities.asScala
        .filterNot(_ == expression)
        .filterNot(!includeDeprecated && isDeprecated(_))
        .map(_.getIRI.toString).toList)
      merge(json, equivalents.toJson)
    } else json
  }

  def queryInstances(expression: OWLClassExpression, direct: Boolean, includeDeprecated: Boolean): Future[JsObject] = Future {
    val results = Map("hasInstance" -> reasoner.getInstances(expression, direct).getFlattened.asScala
      .filterNot(!includeDeprecated && isDeprecated(_))
      .map(_.getIRI.toString).toList)
    merge(toQueryObject(expression), results.toJson, jsonldContext)
  }

  def queryEquivalentClasses(expression: OWLClassExpression, includeDeprecated: Boolean): Future[JsObject] = Future {
    val results = Map("equivalentClass" -> reasoner.getEquivalentClasses(expression).getEntities.asScala
      .filterNot(_ == expression)
      .filterNot(!includeDeprecated && isDeprecated(_))
      .map(_.getIRI.toString).toList)
    merge(toQueryObject(expression), results.toJson, jsonldContext)
  }

  def isSatisfiable(expression: OWLClassExpression): Future[JsObject] = Future {
    val results = Map("isSatisfiable" -> reasoner.isSatisfiable(expression))
    merge(toQueryObject(expression), results.toJson, jsonldContext)
  }

  def queryTypes(individual: OWLNamedIndividual, direct: Boolean, includeThing: Boolean, includeDeprecated: Boolean): Future[JsObject] = Future {
    val results = Map("type" -> reasoner.getTypes(individual, direct).getFlattened.asScala
      .filterNot(!includeThing && _.isOWLThing)
      .filterNot(!includeDeprecated && isDeprecated(_))
      .map(_.getIRI.toString).toList)
    merge(toQueryObject(individual), results.toJson, jsonldContext)
  }

  lazy val summary: Future[JsObject] = Future {
    val mainOnt = ontologyIDToJSONMap(reasoner.getRootOntology.getOntologyID, 0)
    val imports = reasoner.getRootOntology.getImports.asScala.to(Set).zipWithIndex.map { case (imp, index) => ontologyIDToJSONMap(imp.getOntologyID, index + 1).toJson }.toSeq
    val summaryObj = mainOnt ++ Map(
      "label" -> name.toJson,
      "imports" -> imports.toJson,
      //"reasoner" -> reasoner.getReasonerName.toJson, //FIXME currently HermiT returns null
      "isConsistent" -> reasoner.isConsistent.toJson,
      "logicalAxiomsCount" -> reasoner.getRootOntology.getLogicalAxiomCount(Imports.INCLUDED).toJson)
    merge(summaryObj.toJson, jsonldContext)
  }

  def extractModuleForOntology(ont: OWLOntology, moduleType: ModuleType, fromOntologies: Set[IRI]): Future[OWLOntology] = Future {
    ont.getSignature(Imports.INCLUDED).asScala.toSet
  }.flatMap(extractModuleForEntities(_, moduleType, fromOntologies))

  def extractModuleForIRIs(iris: Set[IRI], moduleType: ModuleType, fromOntologies: Set[IRI]): Future[OWLOntology] = Future {
    iris.flatMap(ontology.getEntitiesInSignature(_, Imports.INCLUDED).asScala)
  }.flatMap(extractModuleForEntities(_, moduleType, fromOntologies))

  def extractModuleForEntities(entities: Set[OWLEntity], moduleType: ModuleType, fromOntologies: Set[IRI]): Future[OWLOntology] = Future {
    if (fromOntologies.forall(manager.contains)) {
      val (extractFromOnt, fresh) = fromOntologies.size match {
        case 0 => (ontology, false)
        case 1 => (manager.getOntology(fromOntologies.head), false)
        case _ => {
          val freshOnt = manager.createOntology()
          val addImports = fromOntologies.map(o => new AddImport(freshOnt, factory.getOWLImportsDeclaration(o)))
          manager.applyChanges(addImports.toList.asJava)
          (freshOnt, true)
        }
      }
      val extractor = new SyntacticLocalityModuleExtractor(manager, extractFromOnt, moduleType)
      val module = extractor.extract(entities.asJava)
      val localManager = OWLManager.createOWLOntologyManager()
      val moduleOntologyIRI = IRI.create(s"urn:uuid:${UUID.randomUUID().toString}")
      val moduleOntology = localManager.createOntology(module, moduleOntologyIRI)
      localManager.applyChanges(createdDerivedFromAnnotations(extractFromOnt).map(ann => new AddOntologyAnnotation(moduleOntology, ann)).toList.asJava)
      localManager.applyChange(new model.AddOntologyAnnotation(moduleOntology, factory.getOWLAnnotation(Created, currentDateTime())))
      if (fresh) manager.removeOntology(extractFromOnt)
      moduleOntology
    } else {
      val bad = fromOntologies.filterNot(manager.contains)
      throw new IllegalArgumentException(s"Ontologies not found: ${bad.mkString(" ")}")
    }
  }

  private def currentDateTime(): OWLLiteral = {
    val calendar = new GregorianCalendar()
    calendar.setTime(new Date())
    val dateTime = DatatypeFactory.newInstance().newXMLGregorianCalendar(calendar)
    factory.getOWLLiteral(dateTime.toString, OWL2Datatype.XSD_DATE_TIME)
  }

  private val WasDerivedFrom = factory.getOWLAnnotationProperty(PROVVocabulary.WAS_DERIVED_FROM.getIRI)
  private val Used = factory.getOWLAnnotationProperty(PROVVocabulary.USED.getIRI)
  private val Created = factory.getOWLAnnotationProperty(IRI.create("http://purl.org/dc/terms/created"))

  private def createdDerivedFromAnnotations(sourceOntology: OWLOntology): Set[OWLAnnotation] = {
    println(sourceOntology.getImportsClosure.asScala.size)
    (for {
      ont <- sourceOntology.getImportsClosure.asScala
      ontID = ont.getOntologyID
      ontIRI <- ontID.getOntologyIRI.asScala
      annotations = ontID.getVersionIRI.asSet.asScala.map(v => factory.getOWLAnnotation(Used, v)).asJava
    } yield factory.getOWLAnnotation(WasDerivedFrom, ontIRI, annotations)).toSet
  }

  private def ontologyIDToJSONMap(id: OWLOntologyID, anonIndex: Int): Map[String, JsValue] = {
    val versionIRI = id.getVersionIRI.asScala.map(v => "version" -> v.toString.toJson).toSeq
    Map("@id" -> id.getOntologyIRI.asScala.map(_.toString).getOrElse(s"_:anonymousOnt$anonIndex").toJson) ++ versionIRI
  }

  private def toQueryObject(expression: OWLObject): JsObject = expression match {
    case named: OWLEntity => JsObject("@id" -> named.getIRI.toString.toJson)
    case anonymous        => JsObject(
      "@id" -> "_:b0".toJson,
      "value" -> anonymous.toString.toJson) //TODO do a better job of converting the expression to a string
  }

  private def merge(jsonObjects: JsValue*): JsObject = {
    JsObject(jsonObjects.flatMap(_.asInstanceOf[JsObject].fields).toMap) //TODO do this without casting
  }

  private def isDeprecated(entity: OWLEntity): Boolean =
    reasoner.getRootOntology.getImportsClosure.asScala.exists { o =>
      EntitySearcher.getAnnotations(entity, o, factory.getOWLDeprecated).asScala.exists { v =>
        Option(v.getValue.asLiteral.orNull).exists(l => l.isBoolean && l.parseBoolean)
      }
    }

}
