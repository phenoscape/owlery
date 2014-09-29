package org.phenoscape.owlery

import scala.collection.JavaConversions._
import org.phenoscape.owlet.Owlet
import org.semanticweb.owlapi.model.OWLClassExpression
import org.semanticweb.owlapi.model.OWLEntity
import org.semanticweb.owlapi.model.OWLNamedIndividual
import org.semanticweb.owlapi.model.OWLObject
import org.semanticweb.owlapi.reasoner.OWLReasoner
import com.hp.hpl.jena.query.Query
import com.hp.hpl.jena.query.ResultSet
import com.hp.hpl.jena.vocabulary.OWL2
import com.hp.hpl.jena.vocabulary.RDF
import com.hp.hpl.jena.vocabulary.RDFS
import spray.json.DefaultJsonProtocol._
import spray.json._
import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.blocking
import org.semanticweb.owlapi.apibinding.OWLManager
import org.semanticweb.owlapi.model.IRI
import java.util.UUID
import org.semanticweb.owlapi.model.OWLClass

case class Knowledgebase(name: String, reasoner: OWLReasoner) {

  private lazy val owlet = new Owlet(this.reasoner)
  private lazy val factory = OWLManager.getOWLDataFactory
  private val jsonldContext = Map(
    "@context" -> Map(
      "subClassOf" -> Map(
        "@id" -> RDFS.subClassOf.getURI,
        "@type" -> "@id"),
      "superClassOf" -> Map(
        "@reverse" -> RDFS.subClassOf.getURI,
        "@type" -> "@id"),
      "equivalentClass" -> Map(
        "@id" -> OWL2.equivalentClass.getURI,
        "@type" -> "@id"),
      "hasInstance" -> Map(
        "@reverse" -> RDF.`type`.getURI,
        "@type" -> "@id"),
      "value" -> Map(
        "@id" -> RDF.value.getURI,
        "@type" -> "@id"),
      "isSatisfiable" -> Map(
        "@id" -> Vocabulary.isSatisfiable,
        "@type" -> "http://www.w3.org/2001/XMLSchema#boolean"))).toJson

  def performSPARQLQuery(query: Query): Future[ResultSet] = Future(blocking(owlet.performSPARQLQuery(query)))

  def expandSPARQLQuery(query: Query): Future[Query] = Future(blocking(owlet.expandQuery(query)))

  def querySuperClasses(expression: OWLClassExpression, direct: Boolean): Future[JsObject] = Future {
    blocking {
      val namedQuery = addQueryAsClass(expression)
      val results = Map("subClassOf" -> reasoner.getSuperClasses(namedQuery, direct).getFlattened.map(_.getIRI.toString).toList)
      merge(toQueryObject(expression), results.toJson, jsonldContext)
    }
  }

  def querySubClasses(expression: OWLClassExpression, direct: Boolean): Future[JsObject] = Future {
    blocking {
      val namedQuery = addQueryAsClass(expression)
      val results = Map("superClassOf" -> reasoner.getSubClasses(namedQuery, direct).getFlattened.map(_.getIRI.toString).toList)
      merge(toQueryObject(expression), results.toJson, jsonldContext)
    }
  }

  def queryInstances(expression: OWLClassExpression, direct: Boolean): Future[JsObject] = Future {
    blocking {
      val namedQuery = addQueryAsClass(expression)
      val results = Map("hasInstance" -> reasoner.getInstances(namedQuery, direct).getFlattened.map(_.getIRI.toString).toList)
      merge(toQueryObject(expression), results.toJson, jsonldContext)
    }
  }

  def queryEquivalentClasses(expression: OWLClassExpression): Future[JsObject] = Future {
    blocking {
      val namedQuery = addQueryAsClass(expression)
      val results = Map("equivalentClass" -> reasoner.getEquivalentClasses(namedQuery).getEntities.filterNot(_ == expression).map(_.getIRI.toString).toList)
      merge(toQueryObject(expression), results.toJson, jsonldContext)
    }
  }

  def isSatisfiable(expression: OWLClassExpression): Future[JsObject] = Future {
    blocking {
      val namedQuery = addQueryAsClass(expression)
      val results = Map("isSatisfiable" -> reasoner.isSatisfiable(namedQuery))
      merge(toQueryObject(expression), results.toJson, jsonldContext)
    }
  }

  def queryTypes(individual: OWLNamedIndividual, direct: Boolean): Future[JsObject] = Future {
    blocking {
      val results = Map("@type" -> reasoner.getTypes(individual, direct).getFlattened.map(_.getIRI.toString).toList)
      merge(toQueryObject(individual), results.toJson, jsonldContext)
    }
  }

  def addQueryAsClass(expression: OWLClassExpression): OWLClass = expression match {
    case named: OWLClass => named
    case anonymous => {
      val ontology = reasoner.getRootOntology
      val manager = ontology.getOWLOntologyManager
      val namedQuery = factory.getOWLClass(IRI.create(s"http://example.org/${UUID.randomUUID.toString}"))
      manager.addAxiom(ontology, factory.getOWLEquivalentClassesAxiom(namedQuery, expression))
      reasoner.flush()
      namedQuery
    }

  }

  lazy val summary: Future[JsObject] = Future {
    blocking {
      val summaryObj = Map(
        "label" -> name.toJson,
        "isConsistent" -> reasoner.isConsistent.toJson,
        "logicalAxiomsCount" -> reasoner.getRootOntology.getLogicalAxiomCount.toJson)
      merge(summaryObj.toJson, jsonldContext)
    }
  }

  private def toQueryObject(expression: OWLObject): JsObject = expression match {
    case named: OWLEntity => JsObject("@id" -> named.getIRI.toString.toJson)
    case anonymous => JsObject(
      "@id" -> "_:b0".toJson,
      "value" -> anonymous.toString.toJson) //TODO do a better job of converting the expression to a string
  }

  private def merge(jsonObjects: JsValue*): JsObject = {
    JsObject(jsonObjects.flatMap(_.asInstanceOf[JsObject].fields).toMap) //TODO do this without casting
  }

}