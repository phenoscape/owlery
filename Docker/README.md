# Docker container for Owlery

This Docker container will automatically run the owlery service, exposed on
port 8080 (map to a port suitable for you on the host).

## Runtime customization

### application.conf
By defaut, the service will expect the `application.conf` file at `/srv/conf/application.conf`. Map the directory where you have your conf file to this path, or alternatively override the location on the `docker run` command line (append `-Dconfig.file=</path/to/application.conf>` after the image name).

Note that the `owlery.port` and `owlery.host` settings in your `application.conf` are irrelevant, because they are overridden in the entrypoint definition for the container.

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

