package org.phenoscape.owlery

import org.semanticweb.owlapi.apibinding.OWLManager
import java.io.File
import org.semanticweb.owlapi.model.UnloadableImportException
import org.semanticweb.owlapi.model.OWLOntologyLoaderConfiguration
import org.semanticweb.owlapi.model.MissingImportHandlingStrategy
import org.semanticweb.owlapi.io.FileDocumentSource
import org.semanticweb.owlapi.model.IRI
import org.semanticweb.owlapi.model.OWLOntologyIRIMapper
import org.semanticweb.owlapi.model.OWLOntologyManager
import scala.collection.JavaConversions._
import org.semanticweb.owlapi.model.OWLOntology
import org.semanticweb.owlapi.model.AddImport
import org.semanticweb.owlapi.reasoner.structural.StructuralReasonerFactory
import org.apache.commons.io.FileUtils

object Owlery {

  val kbs = demoKBs()
  private[this] val factory = OWLManager.getOWLDataFactory
  private[this] val loaderConfig = new OWLOntologyLoaderConfiguration().setMissingImportHandlingStrategy(MissingImportHandlingStrategy.SILENT)

  def kb(name: String): Option[Knowledgebase] = kbs.get(name)

  private[this] def demoKBs(): Map[String, Knowledgebase] = {
    val uberon = OWLManager.createOWLOntologyManager().loadOntology(IRI.create("http://purl.obolibrary.org/obo/uberon/ext.owl"))
    val uberonReasoner = new StructuralReasonerFactory().createReasoner(uberon)
    val pato = OWLManager.createOWLOntologyManager().loadOntology(IRI.create("http://purl.obolibrary.org/obo/pato.owl"))
    val patoReasoner = new StructuralReasonerFactory().createReasoner(pato)
    Map(
      "uberon" -> Knowledgebase("uberon", uberonReasoner),
      "pato" -> Knowledgebase("pato", patoReasoner))
  }

  private[this] def checkForMissingImports(manager: OWLOntologyManager): Set[IRI] = {
    val allImportedOntologies = manager.getOntologies.flatMap(_.getImportsDeclarations).map(_.getIRI).toSet
    val allLoadedOntologies = manager.getOntologies.map(_.getOntologyID.getOntologyIRI).toSet
    allImportedOntologies -- allLoadedOntologies
  }

  private[this] def loadOntology(manager: OWLOntologyManager, file: File): Unit =
    manager.loadOntologyFromOntologyDocument(new FileDocumentSource(file), loaderConfig)

  private[this] def createManager(): OWLOntologyManager = {
    val manager = OWLManager.createOWLOntologyManager
    manager.clearIRIMappers()
    manager.addIRIMapper(NullIRIMapper)
    manager
  }

  private[this] def importAll(manager: OWLOntologyManager): OWLOntology = {
    val onts = manager.getOntologies()
    val newOnt = manager.createOntology
    onts foreach { ont =>
      manager.applyChange(new AddImport(newOnt, factory.getOWLImportsDeclaration(ont.getOntologyID.getOntologyIRI)))
    }
    newOnt
  }

  private[this] def loadKnowledgebases(configs: Set[KnowledgebaseConfig]): Map[String, Knowledgebase] =
    configs.map(loadKnowledgebase).map(kb => kb.name -> kb).toMap

  private[this] def loadKnowledgebase(config: KnowledgebaseConfig): Knowledgebase = {
    val manager = createManager()
    FileUtils.listFiles(new File(config.location), Array[String](), true).foreach(loadOntology(manager, _))
    val ontology = importAll(manager)
    val reasoner = config.reasoner match {
      case "structural" => new StructuralReasonerFactory().createReasoner(ontology)
      case "elk" => ???
      case "hermit" => ???
      case _ => new StructuralReasonerFactory().createReasoner(ontology)
    }
    Knowledgebase(config.name, reasoner)
  }

  private[this] case class KnowledgebaseConfig(name: String, location: String, reasoner: String)

  private[this] object NullIRIMapper extends OWLOntologyIRIMapper {

    override def getDocumentIRI(iri: IRI): IRI = null

  }

}