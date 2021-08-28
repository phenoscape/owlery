[![Build Status](https://secure.travis-ci.org/phenoscape/owlery.png)](http://travis-ci.org/phenoscape/owlery)

# Owlery

[Owlery](https://owlery.phenoscape.org/api/) is a set of REST web services, built on the [akka-http](https://akka.io) toolkit, which allow querying of an OWL reasoner containing a configured set of ontologies, via HTTP.

It also provides a web service front-end for [Owlet](https://github.com/phenoscape/owlet), which can be used as a remote SERVICE in a [SPARQL federated query](http://www.w3.org/TR/sparql11-federated-query/).

## Examples

 * [phenoscape owlery api](https://owlery.phenoscape.org/api/) _This link needs to be updated to reflect the current Owlery API location_
     * [swagger.json](https://owlery.phenoscape.org/json/swagger.json)

## Running Owlery

* A docker image is [available on DockerHub](https://hub.docker.com/r/phenoscape/owlery).
* You can run directly as a Java application: download a `.tgz` [for the latest release](https://github.com/phenoscape/owlery/releases/latest).

### Configuration

* Configuration file template: https://github.com/phenoscape/owlery/blob/master/src/main/resources/application.conf.example
* Your own configuration file can be specified with a JVM argument, e.g.: `-Dconfig.file=/etc/default/owlery.conf`

#### Specifying ontology file(s) to load

The `location` property for each knowledgebase specifies which ontologies to load into Owlery. This property can be a path or an IRI. If the path is a folder, all files in the folder will be imported into one ontology. If the path is a file, that file will be directly loaded as an ontology. If the value is an IRI, the ontology will be retrieved from that IRI.

##### Locating ontologies via IRI

By default, an ontology specified by IRI will be downloaded from the web using that IRI. This applies to a `location` configuration value as well as to any ontologies imported via `owl:imports` within any loaded ontology. You can use the `catalog` property for a kb to map ontology IRIs to other URLs or filesystem paths by providing a catalog XML file, as is done in Protégé. For example, `application.conf` could be:

```
owlery {
  port = 8080
  host = localhost
  kbs = [
          {
            name = myproject
            location = "http://example.org/my-importer-ont.owl"
            reasoner = hermit
            catalog = "/data/project/catalog.xml"
          }
        ]
}
```

Say the ontology `http://example.org/my-importer-ont.owl` contains an `owl:imports` statement pointing to `http://example.org/my-terminology.owl`. `/data/project.catalog.xml` might look like this:

```xml
<?xml version="1.0" encoding="UTF-8" standalone="no"?>
<catalog prefer="public" xmlns="urn:oasis:names:tc:entity:xmlns:xml:catalog">
    <uri name="http://example.org/my-importer-ont.owl" uri="/data/ontologies/my-importer-ont.owl"/>
    <uri name="http://example.org/my-terminology.owl" uri="/data/ontologies/my-terminology.owl"/>
</catalog>
```

### Experimenting with the API

* The API description can be found on your server.  If your server runs locally on port 8080, you can get the API description at `http://localhost:8080/docs/swagger.json`.
* An interactive version of the API can be found here: `http://localhost:8080/docs/index.html`
