package org.phenoscape.owlery

import spray.http._
import spray.httpx.marshalling._
import spray.httpx.unmarshalling._
import com.hp.hpl.jena.query.Query
import com.hp.hpl.jena.query.ResultSet
import com.hp.hpl.jena.query.ResultSetFormatter
import com.hp.hpl.jena.query.QueryFactory
import com.hp.hpl.jena.query.QueryException

object SPARQLFormats {

  val `application/sparql-results+xml` = MediaTypes.register(MediaType.custom("application/sparql-results+xml"))
  val `application/sparql-query` = MediaTypes.register(MediaType.custom("application/sparql-query"))

  implicit val SPARQLXMLMarshaller = Marshaller.delegate[ResultSet, String](`application/sparql-results+xml`, MediaTypes.`application/xml`, MediaTypes.`text/xml`)(ResultSetFormatter.asXMLString(_))

  implicit val SPARQLQueryUnmarshaller = Unmarshaller.delegate[String, Query](`application/sparql-query`)(QueryFactory.create(_))

  implicit object SPARQLQueryValue extends Deserializer[String, Query] {

    def apply(text: String): Deserialized[Query] = try {
      Right(QueryFactory.create(text))
    } catch {
      case e: QueryException => Left(MalformedContent(e.getMessage, e))
    }

  }

}