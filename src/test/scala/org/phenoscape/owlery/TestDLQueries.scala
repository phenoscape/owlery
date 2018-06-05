package org.phenoscape.owlery

import utest._
import org.semanticweb.owlapi.apibinding.OWLManager
import org.semanticweb.owlapi.model.IRI
import java.io.File
import org.semanticweb.elk.owlapi.ElkReasonerFactory
import scala.concurrent.ExecutionContext.Implicits.global
import spray.json.JsArray
import org.apache.jena.system.JenaSystem
import spray.json.JsString

object TestDLQueries extends TestSuite {

  JenaSystem.init()

  val factory = OWLManager.getOWLDataFactory
  val MuscleOrgan = factory.getOWLClass(IRI.create("http://purl.obolibrary.org/obo/UBERON_0001630"))
  val SkeletalMuscleOrgan = factory.getOWLClass(IRI.create("http://purl.obolibrary.org/obo/UBERON_0014892"))
  val RespiratorySystemMuscle = factory.getOWLClass(IRI.create("http://purl.obolibrary.org/obo/UBERON_0003831"))

  val tests = Tests {
    val ontology = OWLManager.createOWLOntologyManager().loadOntologyFromOntologyDocument(this.getClass.getResourceAsStream("skeleton.ofn"))
    val reasoner = new ElkReasonerFactory().createReasoner(ontology)
    val kb = Knowledgebase("test_ont", reasoner)

    "Test filters" - {

      "Include deprecated terms" - {
        kb.querySubClasses(MuscleOrgan, true, false, false, true).map { res =>
          res.fields("superClassOf") match {
            case JsArray(values) => {
              val strings = values.map { case JsString(v) => v }
              assert(strings.contains(SkeletalMuscleOrgan.getIRI.toString))
              assert(strings.contains(RespiratorySystemMuscle.getIRI.toString))
            }
          }
        }

        "Exclude deprecated terms" - {
          kb.querySubClasses(MuscleOrgan, true, false, false, false).map { res =>
            res.fields("superClassOf") match {
              case JsArray(values) => {
                val strings = values.map { case JsString(v) => v }
                assert(!strings.contains(SkeletalMuscleOrgan.getIRI.toString))
                assert(strings.contains(RespiratorySystemMuscle.getIRI.toString))
              }
            }
          }
        }
      }

      "Include owlNothing" - {
        kb.querySubClasses(MuscleOrgan, false, false, true, true).map { res =>
          res.fields("superClassOf") match {
            case JsArray(values) => {
              val strings = values.map { case JsString(v) => v }
              assert(strings.contains(SkeletalMuscleOrgan.getIRI.toString))
              assert(strings.contains(RespiratorySystemMuscle.getIRI.toString))
              assert(strings.contains(factory.getOWLNothing.getIRI.toString))
            }
          }
        }

        "not when conflicts with 'direct'" - {
          kb.querySubClasses(MuscleOrgan, true, false, true, true).map { res =>
            res.fields("superClassOf") match {
              case JsArray(values) => {
                val strings = values.map { case JsString(v) => v }
                assert(strings.contains(SkeletalMuscleOrgan.getIRI.toString))
                assert(strings.contains(RespiratorySystemMuscle.getIRI.toString))
                assert(!strings.contains(factory.getOWLNothing.getIRI.toString))
              }
            }
          }
        }

      }

      "Exclude owlNothing" - {
        kb.querySubClasses(MuscleOrgan, false, false, false, true).map { res =>
          res.fields("superClassOf") match {
            case JsArray(values) => {
              val strings = values.map { case JsString(v) => v }
              assert(strings.contains(SkeletalMuscleOrgan.getIRI.toString))
              assert(strings.contains(RespiratorySystemMuscle.getIRI.toString))
              assert(!strings.contains(factory.getOWLNothing.getIRI.toString))
            }
          }
        }
      }

    }

  }

}