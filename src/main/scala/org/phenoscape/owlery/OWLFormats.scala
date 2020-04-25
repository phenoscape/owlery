package org.phenoscape.owlery

import akka.http.scaladsl.unmarshalling.Unmarshaller
import org.phenoscape.owlet.ManchesterSyntaxClassExpressionParser
import org.semanticweb.owlapi.model.OWLClassExpression
import scalaz._

object OWLFormats {

  implicit val ManchesterSyntaxClassExpression: Unmarshaller[String, OWLClassExpression] = Unmarshaller.strict { text =>
    ManchesterSyntaxClassExpressionParser.parse(text) match {
      case Success(expression) => expression
      case Failure(message)    => throw new IllegalArgumentException(message)
    }

  }

}