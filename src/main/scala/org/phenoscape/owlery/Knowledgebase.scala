package org.phenoscape.owlery

import org.phenoscape.owlet.Owlet
import org.semanticweb.owlapi.reasoner.OWLReasoner

import com.hp.hpl.jena.query.Query

case class Knowledgebase(name: String, reasoner: OWLReasoner) {

  def performSPARQLQuery(query: Query): String = {
    val owlet = new Owlet(this.reasoner)
    //TODO
    ???
  }

}