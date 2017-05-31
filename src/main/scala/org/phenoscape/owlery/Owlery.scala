package org.phenoscape.owlery

import java.io.File

import scala.collection.JavaConversions._

import org.apache.commons.io.FileUtils
import org.semanticweb.elk.owlapi.ElkReasonerFactory
import org.semanticweb.owlapi.apibinding.OWLManager
import org.semanticweb.owlapi.io.FileDocumentSource
import org.semanticweb.owlapi.model.AddImport
import org.semanticweb.owlapi.model.IRI
import org.semanticweb.owlapi.model.MissingImportHandlingStrategy
import org.semanticweb.owlapi.model.OWLOntology
import org.semanticweb.owlapi.model.OWLOntologyIRIMapper
import org.semanticweb.owlapi.model.OWLOntologyLoaderConfiguration
import org.semanticweb.owlapi.model.OWLOntologyManager
import org.semanticweb.owlapi.reasoner.structural.StructuralReasonerFactory

import com.typesafe.config.Config
import com.typesafe.config.ConfigFactory

object Owlery extends MarshallableOwlery {

  private[this] val factory = OWLManager.getOWLDataFactory
  private[this] val loaderConfig = new OWLOntologyLoaderConfiguration().setMissingImportHandlingStrategy(MissingImportHandlingStrategy.SILENT)
  val kbs = loadKnowledgebases(ConfigFactory.load().getConfigList("owlery.kbs").map(configToKBConfig).toSet)

  def kb(name: String): Option[Knowledgebase] = kbs.get(name)

  private[this] def checkForMissingImports(manager: OWLOntologyManager): Set[IRI] = {
    val allImportedOntologies = manager.getOntologies.flatMap(_.getImportsDeclarations).map(_.getIRI).toSet
    val allLoadedOntologies = manager.getOntologies.map(_.getOntologyID.getOntologyIRI).toSet
    allImportedOntologies -- allLoadedOntologies
  }

  private[this] def loadOntologyFromLocalFile(manager: OWLOntologyManager, file: File): Unit = manager.loadOntologyFromOntologyDocument(new FileDocumentSource(file), loaderConfig)

  private[this] def createOntologyFolderManager(): OWLOntologyManager = {
    val manager = OWLManager.createOWLOntologyManager
    manager.clearIRIMappers()
    manager.addIRIMapper(NullIRIMapper)
    manager
  }

  private[this] def importAll(manager: OWLOntologyManager): OWLOntology = {
    val onts = manager.getOntologies
    val newOnt = manager.createOntology
    for (ont <- onts)
      manager.applyChange(new AddImport(newOnt, factory.getOWLImportsDeclaration(ont.getOntologyID.getOntologyIRI)))
    newOnt
  }

  private[this] def configToKBConfig(config: Config) = KnowledgebaseConfig(config.getString("name"), config.getString("location"), config.getString("reasoner"))

  private[this] def loadKnowledgebases(configs: Set[KnowledgebaseConfig]): Map[String, Knowledgebase] =
    configs.map(loadKnowledgebase).map(kb => kb.name -> kb).toMap

  private[this] def loadKnowledgebase(config: KnowledgebaseConfig): Knowledgebase = {
    val ontology = if (config.location.startsWith("http")) loadOntologyFromWeb(config.location)
    else loadOntologyFromFolder(config.location)
    val reasoner = config.reasoner match {
      case "structural" => new StructuralReasonerFactory().createReasoner(ontology)
      case "elk"        => new ElkReasonerFactory().createReasoner(ontology)
      case "hermit"     => ???
      case _            => new StructuralReasonerFactory().createReasoner(ontology)
    }
    Knowledgebase(config.name, reasoner)
  }

  private[this] def loadOntologyFromWeb(location: String): OWLOntology = {
    val manager = OWLManager.createOWLOntologyManager
    manager.loadOntology(IRI.create(location))
  }

  private[this] def loadOntologyFromFolder(location: String): OWLOntology = {
    val manager = createOntologyFolderManager()
    FileUtils.listFiles(new File(location), null, true).foreach(loadOntologyFromLocalFile(manager, _))
    val onts = manager.getOntologies
    if (onts.size == 1) onts.head
    else importAll(manager)
  }

  private[this] case class KnowledgebaseConfig(name: String, location: String, reasoner: String)

  private[this] object NullIRIMapper extends OWLOntologyIRIMapper {

    override def getDocumentIRI(iri: IRI): IRI = null

  }

}