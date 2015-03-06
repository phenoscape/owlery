package org.phenoscape.owlery

import spray.http._
import spray.httpx.marshalling._
import spray.json.DefaultJsonProtocol._
import spray.json._
import spray.httpx.SprayJsonSupport._

trait MarshallableOwlery {

  def kbs: Map[String, Knowledgebase]

  implicit val OwleryMarshaller = Marshaller.delegate[MarshallableOwlery, JsArray](MediaTypes.`application/json`)(owlery => JsArray(owlery.kbs.keys.map(_.toJson).toVector))

}