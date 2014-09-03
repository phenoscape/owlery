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

  def performSPARQLQuery(query: Query): ResultSet = owlet.performSPARQLQuery(query)

  def expandSPARQLQuery(query: Query): Query = owlet.expandQuery(query)

  def querySuperClasses(expression: OWLClassExpression, direct: Boolean): JsObject = {
    val results = Map("subClassOf" -> reasoner.getSuperClasses(expression, direct).getFlattened.map(_.getIRI.toString).toList)
    merge(toQueryObject(expression), results.toJson, jsonldContext)
  }

  def querySubClasses(expression: OWLClassExpression, direct: Boolean): JsObject = {
    val results = Map("superClassOf" -> reasoner.getSubClasses(expression, direct).getFlattened.map(_.getIRI.toString).toList)
    merge(toQueryObject(expression), results.toJson, jsonldContext)
  }

  def queryInstances(expression: OWLClassExpression, direct: Boolean): JsObject = {
    val results = Map("hasInstance" -> reasoner.getInstances(expression, direct).getFlattened.map(_.getIRI.toString).toList)
    merge(toQueryObject(expression), results.toJson, jsonldContext)
  }

  def queryEquivalentClasses(expression: OWLClassExpression): JsObject = {
    val results = Map("equivalentClass" -> reasoner.getEquivalentClasses(expression).getEntities.filterNot(_ == expression).map(_.getIRI.toString).toList)
    merge(toQueryObject(expression), results.toJson, jsonldContext)
  }

  def isSatisfiable(expression: OWLClassExpression): JsObject = {
    val results = Map("isSatisfiable" -> reasoner.isSatisfiable(expression))
    merge(toQueryObject(expression), results.toJson, jsonldContext)
  }

  def queryTypes(individual: OWLNamedIndividual, direct: Boolean): JsObject = {
    val results = Map("@type" -> reasoner.getTypes(individual, direct).getFlattened.map(_.getIRI.toString).toList)
    merge(toQueryObject(individual), results.toJson, jsonldContext)
  }

  lazy val summary: JsObject = {
    val summaryObj = Map(
      "label" -> name.toJson,
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