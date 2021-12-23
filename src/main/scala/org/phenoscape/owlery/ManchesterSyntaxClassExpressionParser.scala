package org.phenoscape.owlery

import org.phenoscape.owlery.PrefixEntityChecker.KnownEntities
import org.semanticweb.owlapi.apibinding.OWLManager
import org.semanticweb.owlapi.manchestersyntax.parser.ManchesterOWLSyntaxClassExpressionParser
import org.semanticweb.owlapi.model._

import scala.collection.Map
import scala.util.Try

object ManchesterSyntaxClassExpressionParser {

  def parse(expression: String, prefixes: Map[String, String], knownEntities: Option[KnownEntities]): Either[String, OWLClassExpression] = {
    val checker = new PrefixEntityChecker(prefixes, knownEntities)
    val parser = new ManchesterOWLSyntaxClassExpressionParser(OWLManager.getOWLDataFactory, checker)
    Try(parser.parse(expression)).toEither.left.map(_.getMessage)
  }

  def parseIRI(input: String, prefixes: Map[String, String] = Map.empty): Either[String, IRI] =
    PrefixEntityChecker.nameToIRI(input, prefixes) match {
      case Some(iri) => Right(iri)
      case None      => Left(s"Invalid IRI: $input")
    }

}
