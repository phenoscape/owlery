package org.phenoscape.owlery

import org.phenoscape.owlet.Owlet
import org.semanticweb.owlapi.reasoner.OWLReasoner
import com.hp.hpl.jena.query.Query
import com.hp.hpl.jena.query.ResultSet

case class Knowledgebase(name: String, reasoner: OWLReasoner) {

  def performSPARQLQuery(query: Query): ResultSet = {
    val owlet = new Owlet(this.reasoner)
    owlet.performSPARQLQuery(query)
  }

}