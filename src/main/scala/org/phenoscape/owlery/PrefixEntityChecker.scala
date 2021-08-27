package org.phenoscape.owlery

import org.phenoscape.owlery.PrefixEntityChecker.KnownEntities
import org.semanticweb.owlapi.apibinding.OWLManager
import org.semanticweb.owlapi.expression.OWLEntityChecker
import org.semanticweb.owlapi.model._
import org.semanticweb.owlapi.model.parameters.Imports
import org.semanticweb.owlapi.vocab.{OWL2Datatype, OWLRDFVocabulary, XSDVocabulary}

import scala.jdk.CollectionConverters._

object PrefixEntityChecker {
  private val CURIE = "^([^:]*):(.*)$".r
  private val FullIRI = "^<(.+)>$".r
  private val factory = OWLManager.getOWLDataFactory()

  def nameToIRI(name: String, prefixes: PartialFunction[String, String]): Option[IRI] = name match {
    case FullIRI(iri)         => Option(IRI.create(iri))
    case CURIE(prefix, local) => prefixes.lift(prefix).map(uri => IRI.create(s"$uri$local"))
    case _                    => None
  }

  def findKnownEntities(ontology: OWLOntology): KnownEntities = {
    val builtInDatatypes = XSDVocabulary.values.map(_.getIRI).toSet ++ OWL2Datatype.values.map(_.getIRI) + OWLRDFVocabulary.RDFS_LITERAL.getIRI + OWLRDFVocabulary.RDF_XML_LITERAL.getIRI
    KnownEntities(
      ontology.getClassesInSignature(Imports.INCLUDED).asScala.toSet + factory.getOWLThing + factory.getOWLNothing,
      ontology.getIndividualsInSignature(Imports.INCLUDED).asScala.toSet,
      ontology.getObjectPropertiesInSignature(Imports.INCLUDED).asScala.toSet + factory.getOWLTopObjectProperty + factory.getOWLBottomObjectProperty,
      ontology.getDataPropertiesInSignature(Imports.INCLUDED).asScala.toSet + factory.getOWLTopDataProperty,
      ontology.getAnnotationPropertiesInSignature(Imports.INCLUDED).asScala.toSet,
      ontology.getDatatypesInSignature(Imports.INCLUDED).asScala.map(_.getIRI).toSet ++ builtInDatatypes
    )
  }

  final case class KnownEntities(classes: Set[OWLClass],
                                 individuals: Set[OWLNamedIndividual],
                                 objectProperties: Set[OWLObjectProperty],
                                 dataProperties: Set[OWLDataProperty],
                                 annotationProperties: Set[OWLAnnotationProperty],
                                 datatypes: Set[IRI])

}

class PrefixEntityChecker(prefixes: PartialFunction[String, String], knownEntities: Option[KnownEntities]) extends OWLEntityChecker {

  import PrefixEntityChecker._

  private val factory = OWLManager.getOWLDataFactory

  override def getOWLClass(name: String): OWLClass =
    nameToIRI(name, prefixes).map { iri =>
      val cls = factory.getOWLClass(iri)
      knownEntities.map { known =>
        if (known.classes(cls)) cls else null
      }.getOrElse(cls)
    }.orNull

  override def getOWLObjectProperty(name: String): OWLObjectProperty =
    nameToIRI(name, prefixes).map { iri =>
      val prop = factory.getOWLObjectProperty(iri)
      knownEntities.map { known =>
        if (known.objectProperties(prop)) prop else null
      }.getOrElse(prop)
    }.orNull

  override def getOWLDataProperty(name: String): OWLDataProperty =
    nameToIRI(name, prefixes).map { iri =>
      val prop = factory.getOWLDataProperty(iri)
      knownEntities.map { known =>
        if (known.dataProperties(prop)) prop else null
      }.orNull // Can't parse data properties without ontology
    }.orNull

  override def getOWLIndividual(name: String): OWLNamedIndividual =
    nameToIRI(name, prefixes).map { iri =>
      val ind = factory.getOWLNamedIndividual(iri)
      knownEntities.map { known =>
        if (known.individuals(ind)) ind else null
      }.getOrElse(ind)
    }.orNull

  override def getOWLDatatype(name: String): OWLDatatype =
    nameToIRI(name, prefixes).map { iri =>
      val dt = factory.getOWLDatatype(iri)
      knownEntities.map { known =>
        if (known.datatypes(iri)) dt else null
      }.getOrElse(dt)
    }.orNull

  override def getOWLAnnotationProperty(name: String): OWLAnnotationProperty =
    nameToIRI(name, prefixes).map { iri =>
      val prop = factory.getOWLAnnotationProperty(iri)
      knownEntities.map { known =>
        if (known.annotationProperties(prop)) prop else null
      }.getOrElse(prop)
    }.orNull


}

