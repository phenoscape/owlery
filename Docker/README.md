# Docker container for Owlery

[![Docker Hub Repository](https://img.shields.io/docker/automated/phenoscape/owlery.svg)](https://hub.docker.com/r/phenoscape/owlery/) [![Docker Hub Pulls](https://img.shields.io/docker/pulls/phenoscape/owlery.svg)](https://hub.docker.com/r/phenoscape/owlery/)

This Docker container will automatically run the owlery service, exposed on
port 8080 (map to a port suitable for you on the host).

## Runtime customization

### application.conf
By defaut, the service will expect the `application.conf` file at `/srv/conf/application.conf`. Map the directory where you have your conf file to this path, or alternatively override the location on the `docker run` command line (append `-Dconfig.file=</path/to/application.conf>` after the image name; this must be a path _within_ the container).

You can find an example `application.conf` file in [`src/main/resources/application.conf.example`](../src/main/resources/application.conf.example) (relative to the project root).

Note that the `owlery.port` and `owlery.host` settings in your `application.conf` are irrelevant, because they are overridden in the entrypoint definition for the container.

### Ontologies to be loaded
The ontologies to be loaded are defined in `application.conf`, section `owlery.kbs` (see above for example), and can be configured to be loaded from a URL, or from a file. If you have an ontology in a file, map the file into the container at `/srv`.

### Java memory and other options
Owlery will typically benefit from a fair amount of memory. The reasoner will load into and hold in memory all ontologies you configure. By default, Java is allowed up to 8GB of memory in the container.

You can change the memory available to Java, and other options, by setting the `JAVA_OPTS` environment on the `docker run` command line, by using the `--env` option. Note that this will override the default setting, and hence if you set the environemnt, you must include the increased memory as well.

## Build configuration

Currently the following build arguments (`--build-arg` command line option to `docker build`) are supported:

* `OWLERY_USER` and `OWLERY_GROUP`: designated user and group for running
  the owlery process within the container (default: `owlery`)
* `TARGET`: the target to build, as the branch, tag, or release. By default,
  the _latest release_ is built (this may not be the latest _tag_). To build
  a specific branch or tag, set `TARGET` to the corresponding value (e.g.,
  `--build-arg TARGET=master` to build from the master branch).

