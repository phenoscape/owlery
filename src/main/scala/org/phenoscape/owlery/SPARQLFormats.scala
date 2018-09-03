package org.phenoscape.owlery

import akka.http.scaladsl.marshalling.{Marshaller, ToEntityMarshaller}
import akka.http.scaladsl.model.{HttpCharsets, HttpEntity, MediaType}
import akka.http.scaladsl.unmarshalling.{FromEntityUnmarshaller, Unmarshaller}
import org.apache.jena.query._

object SPARQLFormats {

  val `application/sparql-results+xml`: MediaType.WithFixedCharset = MediaType.applicationWithFixedCharset("sparql-results+xml", HttpCharsets.`UTF-8`, "xml")
  val `application/sparql-query`: MediaType.WithFixedCharset = MediaType.applicationWithFixedCharset("sparql-query", HttpCharsets.`UTF-8`, "rq", "sparql")
  val `application/ld+json`: MediaType.WithFixedCharset = MediaType.applicationWithFixedCharset("ld+json", HttpCharsets.`UTF-8`, "jsonld")

  implicit val SPARQLXMLMarshaller: ToEntityMarshaller[ResultSet] = Marshaller.stringMarshaller(`application/sparql-results+xml`).compose(ResultSetFormatter.asXMLString)

  val SPARQLQueryBodyUnmarshaller: FromEntityUnmarshaller[Query] = Unmarshaller.stringUnmarshaller.forContentTypes(`application/sparql-query`).map(QueryFactory.create)

  val SPARQLQueryFormUnmarshaller: FromEntityUnmarshaller[Query] = Unmarshaller.defaultUrlEncodedFormDataUnmarshaller.map { data =>
    data.fields.find(_._1 == "query") match {
      case Some((_, queryText)) => QueryFactory.create(queryText)
      case None                     => throw new QueryException
    }
  }

  implicit val SPARQLQueryUnmarshaller: Unmarshaller[HttpEntity, Query] = Unmarshaller.firstOf(SPARQLQueryBodyUnmarshaller, SPARQLQueryFormUnmarshaller)

  implicit val SPARQLQueryMarshaller: ToEntityMarshaller[Query] = Marshaller.stringMarshaller(`application/sparql-query`).compose(_.toString)

  implicit val SPARQLQueryValue: Unmarshaller[String, Query] = Unmarshaller.strict(QueryFactory.create)

}