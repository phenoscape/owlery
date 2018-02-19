package org.phenoscape.owlery

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport.sprayJsonMarshaller
import akka.http.scaladsl.marshalling.Marshaller
import akka.http.scaladsl.marshalling.ToEntityMarshaller
import spray.json._
import spray.json.DefaultJsonProtocol._

trait MarshallableOwlery {

  def kbs: Map[String, Knowledgebase]

  implicit val OwleryMarshaller: ToEntityMarshaller[MarshallableOwlery] = Marshaller.combined(owlery => JsArray(owlery.kbs.keys.map(_.toJson).toVector))

}