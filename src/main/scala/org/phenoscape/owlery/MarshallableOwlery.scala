package org.phenoscape.owlery

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport.sprayJsonMarshaller
import akka.http.scaladsl.marshalling.{Marshaller, ToEntityMarshaller}
import spray.json.DefaultJsonProtocol._
import spray.json._

trait MarshallableOwlery {

  def kbs: Map[String, Knowledgebase]

  implicit val OwleryMarshaller: ToEntityMarshaller[MarshallableOwlery] = Marshaller.combined(owlery => JsArray(owlery.kbs.keys.map(_.toJson).toVector))

}