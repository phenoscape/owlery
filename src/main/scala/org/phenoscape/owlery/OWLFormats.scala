package org.phenoscape.owlery

import akka.http.scaladsl.marshalling.{Marshaller, ToEntityMarshaller}
import akka.http.scaladsl.model.{HttpCharsets, MediaType, MediaTypes}
import akka.http.scaladsl.unmarshalling.{FromEntityUnmarshaller, Unmarshaller}
import org.phenoscape.owlet.ManchesterSyntaxClassExpressionParser
import org.semanticweb.owlapi.apibinding.OWLManager
import org.semanticweb.owlapi.formats.FunctionalSyntaxDocumentFormat
import org.semanticweb.owlapi.io.{StringDocumentSource, StringDocumentTarget}
import org.semanticweb.owlapi.model.{OWLClassExpression, OWLOntology}
import scalaz._
import uk.ac.manchester.cs.owlapi.modularity.ModuleType

object OWLFormats {

  val `text/owl-functional`: MediaType.WithFixedCharset = MediaType.textWithFixedCharset("owl-functional", HttpCharsets.`UTF-8`, "ofn")
  val `text/turtle`: MediaType.WithFixedCharset = MediaType.textWithFixedCharset("turtle", HttpCharsets.`UTF-8`, "ttl")
  val `application/rdf+xml`: MediaType.WithFixedCharset = MediaType.applicationWithFixedCharset("rdf+xml", HttpCharsets.`UTF-8`, "rdf", "owl")


  implicit val ManchesterSyntaxClassExpression: Unmarshaller[String, OWLClassExpression] = Unmarshaller.strict { text =>
    ManchesterSyntaxClassExpressionParser.parse(text) match {
      case Success(expression) => expression
      case Failure(message)    => throw new IllegalArgumentException(message)
    }
  }

  implicit val OWLFunctionalSyntaxMarshaller: ToEntityMarshaller[OWLOntology] = Marshaller.stringMarshaller(`text/owl-functional`).compose { ont =>
    val target = new StringDocumentTarget()
    ont.saveOntology(new FunctionalSyntaxDocumentFormat(), target)
    target.toString
  }

  implicit val OWLTextUnmarshaller: FromEntityUnmarshaller[OWLOntology] = Unmarshaller.stringUnmarshaller.forContentTypes(`application/rdf+xml`, `text/turtle`, `text/owl-functional`, MediaTypes.`text/plain`).map { text =>
    OWLManager.createOWLOntologyManager().loadOntologyFromOntologyDocument(new StringDocumentSource(text))
  }

  implicit val ModuleTypeUnmarshaller: Unmarshaller[String, ModuleType] = Unmarshaller.strict { text =>
    text.toUpperCase match {
      case "STAR" => ModuleType.STAR
      case "TOP"  => ModuleType.TOP
      case "BOT"  => ModuleType.BOT
    }
  }

}