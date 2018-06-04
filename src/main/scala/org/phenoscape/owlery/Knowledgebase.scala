package org.phenoscape.owlery

import scala.collection.JavaConverters._
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.concurrent.blocking

import org.apache.jena.query.Query
import org.apache.jena.query.ResultSet
import org.apache.jena.vocabulary.OWL2
import org.apache.jena.vocabulary.RDF
import org.apache.jena.vocabulary.RDFS
import org.phenoscape.owlet.Owlet
import org.semanticweb.owlapi.model.OWLClassExpression
import org.semanticweb.owlapi.model.OWLEntity
import org.semanticweb.owlapi.model.OWLNamedIndividual
import org.semanticweb.owlapi.model.OWLObject
import org.semanticweb.owlapi.reasoner.OWLReasoner

import spray.json._
import spray.json.DefaultJsonProtocol._

case class Knowledgebase(name: String, reasoner: OWLReasoner) {

  private lazy val owlet = new Owlet(this.reasoner)
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

  def expandSPARQLQuery(query: Query): Future[Query] = Future { owlet.expandQuery(query) }

  def querySuperClasses(expression: OWLClassExpression, direct: Boolean, includeEquivalent: Boolean, includeThing: Boolean): Future[JsObject] = Future {
    val superClasses = Map("subClassOf" -> reasoner.getSuperClasses(expression, direct).getFlattened.asScala.filterNot(_.isOWLThing).map(_.getIRI.toString).toList)
    val json = merge(toQueryObject(expression), superClasses.toJson, jsonldContext)
    if (includeEquivalent) {
      val equivalents = Map("equivalentClass" -> reasoner.getEquivalentClasses(expression).getEntities.asScala.filterNot(_ == expression).map(_.getIRI.toString).toList)
      merge(json, equivalents.toJson)
    } else json
  }

  def querySubClasses(expression: OWLClassExpression, direct: Boolean, includeEquivalent: Boolean, includeNothing: Boolean): Future[JsObject] = Future {
    val subClasses = Map("superClassOf" -> reasoner.getSubClasses(expression, direct).getFlattened.asScala.filterNot(_.isOWLNothing).map(_.getIRI.toString).toList)
    val json = merge(toQueryObject(expression), subClasses.toJson, jsonldContext)
    if (includeEquivalent) {
      val equivalents = Map("equivalentClass" -> reasoner.getEquivalentClasses(expression).getEntities.asScala.filterNot(_ == expression).map(_.getIRI.toString).toList)
      merge(json, equivalents.toJson)
    } else json
  }

  def queryInstances(expression: OWLClassExpression, direct: Boolean): Future[JsObject] = Future {
    val results = Map("hasInstance" -> reasoner.getInstances(expression, direct).getFlattened.asScala.map(_.getIRI.toString).toList)
    merge(toQueryObject(expression), results.toJson, jsonldContext)
  }

  def queryEquivalentClasses(expression: OWLClassExpression): Future[JsObject] = Future {
    val results = Map("equivalentClass" -> reasoner.getEquivalentClasses(expression).getEntities.asScala.filterNot(_ == expression).map(_.getIRI.toString).toList)
    merge(toQueryObject(expression), results.toJson, jsonldContext)
  }

  def isSatisfiable(expression: OWLClassExpression): Future[JsObject] = Future {
    val results = Map("isSatisfiable" -> reasoner.isSatisfiable(expression))
    merge(toQueryObject(expression), results.toJson, jsonldContext)
  }

  def queryTypes(individual: OWLNamedIndividual, direct: Boolean, includeThing: Boolean): Future[JsObject] = Future {
    val results = Map("@type" -> reasoner.getTypes(individual, direct).getFlattened.asScala.filterNot(_.isOWLThing).map(_.getIRI.toString).toList)
    merge(toQueryObject(individual), results.toJson, jsonldContext)
  }

  lazy val summary: Future[JsObject] = Future {
    val summaryObj = Map(
      "label" -> name.toJson,
      //"reasoner" -> reasoner.getReasonerName.toJson, //FIXME currently HermiT returns null
      "isConsistent" -> reasoner.isConsistent.toJson,
      "logicalAxiomsCount" -> reasoner.getRootOntology.getLogicalAxiomCount.toJson)
    merge(summaryObj.toJson, jsonldContext)
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