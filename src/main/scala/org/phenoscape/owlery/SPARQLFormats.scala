package org.phenoscape.owlery

import org.apache.jena.query.Query
import org.apache.jena.query.QueryException
import org.apache.jena.query.QueryFactory
import org.apache.jena.query.ResultSet
import org.apache.jena.query.ResultSetFormatter

import akka.http.scaladsl.marshalling.Marshaller
import akka.http.scaladsl.marshalling.ToEntityMarshaller
import akka.http.scaladsl.model._
import akka.http.scaladsl.model.HttpCharsets
import akka.http.scaladsl.model.MediaType
import akka.http.scaladsl.unmarshalling.FromEntityUnmarshaller
import akka.http.scaladsl.unmarshalling.Unmarshaller

object SPARQLFormats {

  val `application/sparql-results+xml` = MediaType.applicationWithFixedCharset("sparql-results+xml", HttpCharsets.`UTF-8`, "xml")
  val `application/sparql-query` = MediaType.applicationWithFixedCharset("sparql-query", HttpCharsets.`UTF-8`, "rq", "sparql")
  val `application/ld+json` = MediaType.applicationWithFixedCharset("ld+json", HttpCharsets.`UTF-8`, "jsonld")

  implicit val SPARQLXMLMarshaller: ToEntityMarshaller[ResultSet] = Marshaller.stringMarshaller(`application/sparql-results+xml`).compose(ResultSetFormatter.asXMLString(_))

  val SPARQLQueryBodyUnmarshaller: FromEntityUnmarshaller[Query] = Unmarshaller.stringUnmarshaller.forContentTypes(`application/sparql-query`).map(QueryFactory.create)

  val SPARQLQueryFormUnmarshaller: FromEntityUnmarshaller[Query] = Unmarshaller.defaultUrlEncodedFormDataUnmarshaller.map { data =>
    data.fields.filter(_._1 == "query").headOption match {
      case Some((param, queryText)) => QueryFactory.create(queryText)
      case None                     => throw new QueryException
    }
  }

  implicit val SPARQLQueryUnmarshaller = Unmarshaller.firstOf(SPARQLQueryBodyUnmarshaller, SPARQLQueryFormUnmarshaller)

  implicit val SPARQLQueryMarshaller: ToEntityMarshaller[Query] = Marshaller.stringMarshaller(`application/sparql-query`).compose(_.toString)

  implicit val SPARQLQueryValue: Unmarshaller[String, Query] = Unmarshaller.strict(QueryFactory.create)

}