package org.phenoscape.owlery

import org.semanticweb.owlapi.reasoner.OWLReasoner

case class Knowledgebase(name: String, reasoner: OWLReasoner)
