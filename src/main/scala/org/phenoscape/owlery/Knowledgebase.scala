package org.phenoscape.owlery

import org.semanticweb.owlapi.reasoner.OWLReasoner
import com.hp.hpl.jena.query.Query
import com.hp.hpl.jena.query.QueryExecutionFactory
import com.hp.hpl.jena.rdf.model.ModelFactory
import org.phenoscape.owlet.QueryExpander

case class Knowledgebase(name: String, reasoner: OWLReasoner) {

  def performSPARQLQuery(query: Query): String = {
    val owlet = new QueryExpander(this.reasoner)
    //TODO
    ???
  }

}