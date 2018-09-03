package org.phenoscape.owlery

import java.io.File

import com.typesafe.config.{Config, ConfigFactory}
import org.apache.commons.io.FileUtils
import org.semanticweb.HermiT.ReasonerFactory
import org.semanticweb.elk.owlapi.ElkReasonerFactory
import org.semanticweb.owlapi.apibinding.OWLManager
import org.semanticweb.owlapi.model.{IRI, OWLOntology, OWLOntologyManager}
import org.semanticweb.owlapi.reasoner.structural.StructuralReasonerFactory
import uk.ac.manchester.cs.jfact.JFactFactory

import scala.collection.JavaConverters._

object Owlery extends MarshallableOwlery {

  val kbs: Map[String, Knowledgebase] = loadKnowledgebases(ConfigFactory.load().getConfigList("owlery.kbs").asScala.map(configToKBConfig).toSet)

  def kb(name: String): Option[Knowledgebase] = kbs.get(name)

  private[this] def importAll(manager: OWLOntologyManager): OWLOntology = {
    val axioms = for {
      ont <- manager.getOntologies().asScala
      axiom <- ont.getAxioms().asScala
    } yield axiom
    val newOnt = manager.createOntology
    manager.addAxioms(newOnt, axioms.asJava)
    newOnt
  }

  private[this] def configToKBConfig(config: Config) = KnowledgebaseConfig(config.getString("name"), config.getString("location"), config.getString("reasoner"))

  private[this] def loadKnowledgebases(configs: Set[KnowledgebaseConfig]): Map[String, Knowledgebase] =
    configs.map(loadKnowledgebase).map(kb => kb.name -> kb).toMap

  private[this] def loadKnowledgebase(config: KnowledgebaseConfig): Knowledgebase = {
    val ontology = if (config.location.startsWith("http")) loadOntologyFromWeb(config.location)
    else loadOntologyFromFolder(config.location)
    val reasoner = config.reasoner.toLowerCase match {
      case "structural" => new StructuralReasonerFactory().createReasoner(ontology)
      case "elk"        => new ElkReasonerFactory().createReasoner(ontology)
      case "hermit"     => new ReasonerFactory().createReasoner(ontology)
      case "jfact"      => new JFactFactory().createReasoner(ontology)
      case other        => throw new IllegalArgumentException(s"Invalid reasoner specified: $other")
    }
    Knowledgebase(config.name, reasoner)
  }

  private[this] def loadOntologyFromWeb(location: String): OWLOntology = {
    val manager = OWLManager.createOWLOntologyManager
    manager.loadOntology(IRI.create(location))
  }

  private[this] def loadOntologyFromFolder(location: String): OWLOntology = {
    val manager = OWLManager.createOWLOntologyManager()
    FileUtils.listFiles(new File(location), null, true).asScala.foreach(f => manager.loadOntology(IRI.create(f)))
    val onts = manager.getOntologies().asScala
    if (onts.size == 1) onts.head
    else importAll(manager)
  }

  private[this] case class KnowledgebaseConfig(name: String, location: String, reasoner: String)

}