package org.phenoscape.owlery

import org.phenoscape.owlet.Owlet
import org.semanticweb.owlapi.reasoner.OWLReasoner
import com.hp.hpl.jena.query.Query
import com.hp.hpl.jena.query.ResultSet

case class Knowledgebase(name: String, reasoner: OWLReasoner) {

  lazy val owlet = new Owlet(this.reasoner)

  def performSPARQLQuery(query: Query): ResultSet = owlet.performSPARQLQuery(query)

  def expandSPARQLQuery(query: Query): Query = owlet.expandQuery(query)

}