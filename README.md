[![Build Status](https://secure.travis-ci.org/phenoscape/owlery.png)](http://travis-ci.org/phenoscape/owlery)

# Owlery

[Owlery](https://owlery.phenoscape.org/api/) is a set of REST web services, built on the [akka-http](https://akka.io) toolkit, which allow querying of an OWL reasoner containing a configured set of ontologies, via HTTP.

It also provides a web service front-end for [Owlet](https://github.com/phenoscape/owlet), which can be used as a remote SERVICE in a [SPARQL federated query](http://www.w3.org/TR/sparql11-federated-query/).

## Examples

 * [phenoscape owlery api](https://owlery.phenoscape.org/api/)
     * [swagger.json](https://owlery.phenoscape.org/json/swagger.json)

## Running Owlery

* A docker image is [available on DockerHub](https://hub.docker.com/r/phenoscape/owlery).
* You can run directly as a Java application: download a `.tgz` [for the latest release](https://github.com/phenoscape/owlery/releases/latest).

### Configuration

* Configuration file template: https://github.com/phenoscape/owlery/blob/master/src/main/resources/application.conf.example
* Your own configuration file can be specified with a JVM argument, e.g.: `-Dconfig.file=/etc/default/owlery.conf`

### Experimenting with the API

* The API description can be found on your server.  If your server runs locally on port 8080, you can get the API description at `http://localhost:8080/docs/swagger.json`.
* An interactive version of the API can be found here: `http://localhost:8080/docs/index.html`
