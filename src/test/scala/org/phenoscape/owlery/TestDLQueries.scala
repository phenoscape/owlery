package org.phenoscape.owlery

import org.apache.jena.sys.JenaSystem
import org.semanticweb.elk.owlapi.ElkReasonerFactory
import org.semanticweb.owlapi.apibinding.OWLManager
import org.semanticweb.owlapi.model.IRI
import spray.json.{JsArray, JsString}
import utest._

import scala.concurrent.ExecutionContext.Implicits.global

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
            case JsArray(values) =>
              val strings = (values).map {
                case JsString(v) => v
                case _           => ???
              }
              assert(strings.contains(SkeletalMuscleOrgan.getIRI.toString))
              assert(strings.contains(RespiratorySystemMuscle.getIRI.toString))
            case _               => ???
          }
        }

        "Exclude deprecated terms" - {
          kb.querySubClasses(MuscleOrgan, true, false, false, false).map { res =>
            res.fields("superClassOf") match {
              case JsArray(values) =>
                val strings = values.map {
                  case JsString(v) => v
                  case _           => ???
                }
                assert(!strings.contains(SkeletalMuscleOrgan.getIRI.toString))
                assert(strings.contains(RespiratorySystemMuscle.getIRI.toString))
              case _               => ???
            }
          }
        }
      }

      "Include owlNothing" - {
        kb.querySubClasses(MuscleOrgan, false, false, true, true).map { res =>
          res.fields("superClassOf") match {
            case JsArray(values) =>
              val strings = values.map {
                case JsString(v) => v
                case _           => ???
              }
              assert(strings.contains(SkeletalMuscleOrgan.getIRI.toString))
              assert(strings.contains(RespiratorySystemMuscle.getIRI.toString))
              assert(strings.contains(factory.getOWLNothing.getIRI.toString))
            case _               => ???
          }
        }

        "not when conflicts with 'direct'" - {
          kb.querySubClasses(MuscleOrgan, true, false, true, true).map { res =>
            res.fields("superClassOf") match {
              case JsArray(values) =>
                val strings = values.map {
                  case JsString(v) => v
                  case _           => ???
                }
                assert(strings.contains(SkeletalMuscleOrgan.getIRI.toString))
                assert(strings.contains(RespiratorySystemMuscle.getIRI.toString))
                assert(!strings.contains(factory.getOWLNothing.getIRI.toString))
              case _               => ???
            }
          }
        }

      }

      "Exclude owlNothing" - {
        kb.querySubClasses(MuscleOrgan, false, false, false, true).map { res =>
          res.fields("superClassOf") match {
            case JsArray(values) =>
              val strings = values.map {
                case JsString(v) => v
                case _           => ???
              }
              assert(strings.contains(SkeletalMuscleOrgan.getIRI.toString))
              assert(strings.contains(RespiratorySystemMuscle.getIRI.toString))
              assert(!strings.contains(factory.getOWLNothing.getIRI.toString))
            case _               => ???
          }
        }
      }

    }

  }

}