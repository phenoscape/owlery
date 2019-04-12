package org.phenoscape.owlery

import java.io.File
import java.util.UUID

import com.typesafe.config.{Config, ConfigFactory}
import org.apache.commons.io.FileUtils
import org.obolibrary.robot.CatalogXmlIRIMapper
import org.semanticweb.HermiT.ReasonerFactory
import org.semanticweb.elk.owlapi.ElkReasonerFactory
import org.semanticweb.owlapi.apibinding.OWLManager
import org.semanticweb.owlapi.model._
import org.semanticweb.owlapi.reasoner.structural.StructuralReasonerFactory
import uk.ac.manchester.cs.jfact.JFactFactory

import scala.collection.JavaConverters._

object Owlery extends MarshallableOwlery {

  private val factory = OWLManager.getOWLDataFactory

  val kbs: Map[String, Knowledgebase] = loadKnowledgebases(ConfigFactory.load().getConfigList("owlery.kbs").asScala.map(configToKBConfig).toSet)

  def kb(name: String): Option[Knowledgebase] = kbs.get(name)

  private[this] def importAll(manager: OWLOntologyManager, loadedOntologies: Set[OWLOntology]): OWLOntology = {
    val newOnt = manager.createOntology
    for (ont <- loadedOntologies) {
      val ontID = ont.getOntologyID
      if (ontID.getOntologyIRI.isPresent) manager.applyChange(new AddImport(newOnt, factory.getOWLImportsDeclaration(ontID.getOntologyIRI.get)))
      else {
        val newOntIRI = IRI.create(s"urn:uuid:${UUID.randomUUID().toString}")
        manager.applyChange(new SetOntologyID(ont, newOntIRI))
        manager.applyChange(new AddImport(newOnt, factory.getOWLImportsDeclaration(newOntIRI)))
      }
    }
    newOnt
  }

  private[this] def configToKBConfig(config: Config) = KnowledgebaseConfig(
    config.getString("name"),
    config.getString("location"),
    config.getString("reasoner"),
    if (config.hasPath("catalog")) Some(config.getString("catalog")) else None)

  private[this] def loadKnowledgebases(configs: Set[KnowledgebaseConfig]): Map[String, Knowledgebase] =
    configs.map(loadKnowledgebase).map(kb => kb.name -> kb).toMap

  private[this] def loadKnowledgebase(config: KnowledgebaseConfig): Knowledgebase = {
    println(config.catalogLocation)
    val manager = OWLManager.createConcurrentOWLOntologyManager()
    config.catalogLocation.foreach(c => manager.addIRIMapper(new CatalogXmlIRIMapper(new File(c))))
    val ontology = if (config.location.startsWith("http")) manager.loadOntology(IRI.create(config.location))
    else loadOntologyFromFolder(config.location, manager)
    val reasoner = config.reasoner.toLowerCase match {
      case "structural" => new StructuralReasonerFactory().createReasoner(ontology)
      case "elk"        => new ElkReasonerFactory().createReasoner(ontology)
      case "hermit"     => new ReasonerFactory().createReasoner(ontology)
      case "jfact"      => new JFactFactory().createReasoner(ontology)
      case other        => throw new IllegalArgumentException(s"Invalid reasoner specified: $other")
    }
    Knowledgebase(config.name, reasoner)
  }

  private[this] def loadOntologyFromFolder(location: String, manager: OWLOntologyManager): OWLOntology = {
    val fileOrDir = new File(location)
    val files = if (fileOrDir.isDirectory) FileUtils.listFiles(fileOrDir, null, true).asScala else List(fileOrDir)
    val loadedOnts = files.map(f => manager.loadOntology(IRI.create(f))).toSet
    if (loadedOnts.size == 1) loadedOnts.head
    else importAll(manager, loadedOnts)
  }

  private[this] case class KnowledgebaseConfig(name: String, location: String, reasoner: String, catalogLocation: Option[String])

}