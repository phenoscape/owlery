package org.phenoscape.owlery

import scala.Left
import scala.Right

import org.phenoscape.owlet.ManchesterSyntaxClassExpressionParser
import org.semanticweb.owlapi.model.OWLClassExpression

import scalaz._
import spray.httpx.unmarshalling.Deserialized
import spray.httpx.unmarshalling.Deserializer
import spray.httpx.unmarshalling.MalformedContent

object OWLFormats {

  implicit object ManchesterSyntaxClassExpression extends Deserializer[String, OWLClassExpression] {

    def apply(text: String): Deserialized[OWLClassExpression] = ManchesterSyntaxClassExpressionParser.parse(text) match {
      case Success(expression) => Right(expression)
      case Failure(message) => Left(MalformedContent(message))
    }

  }

}