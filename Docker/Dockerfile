FROM openjdk:8-jdk

LABEL maintainer="hilmar.lapp@duke.edu"

# Prevent error messages from debconf about non-interactive frontend
ARG TERM=linux
ARG DEBIAN_FRONTEND=noninteractive

# create designated non-root system user and group
ARG OWLERY_USER=owlery
ARG OWLERY_GROUP=owlery

ADD create-user.sh /usr/local/bin/create-service-user
RUN chmod ug+x /usr/local/bin/create-service-user && \
    create-service-user $OWLERY_USER $OWLERY_GROUP

# Install dependencies of sbt
RUN apt-get update -y && \
    apt-get install -y apt-utils && \
    apt-get install -y \
            apt-transport-https \
            fakeroot

# Install sbt (Scala Build Tool) through its Debian package directory.
RUN echo "deb https://dl.bintray.com/sbt/debian /" > \
         /etc/apt/sources.list.d/sbt.list && \
    apt-key adv --keyserver hkp://keyserver.ubuntu.com:80 \
                --recv 2EE0EA64E40A89B84B2DF73499E82A75642AC823

RUN apt-get update -y && \
    apt-get install -y   \
            sbt

# Download and unpack owlery from the latest release on Github
RUN curl -L -o /tmp/owlery.tar.gz \
         $(curl -s https://api.github.com/repos/phenoscape/owlery/releases/latest | grep tarball_url | cut -d\" -f4) && \
    mkdir /tmp/owlery && \
    tar -z -x -v --strip-components 1 -C /tmp/owlery -f /tmp/owlery.tar.gz

# Build owlery (using sbt)
RUN cd /tmp/owlery && \
    sbt stage

# then install to /usr/share, adding a symlink to /usr/local/bin, and
# end by cleaning up
RUN cd /tmp/owlery && \
    mv target/universal/stage /usr/share/owlery && \
    chmod a+x /usr/share/owlery/bin/owlery && \
    ln -s /usr/share/owlery/bin/owlery /usr/local/bin/owlery && \
    rm -rf /tmp/owlery*

# create the mount point for configuration files
RUN mkdir /srv/conf

# owlery needs a significant amount of memory; you may have to increase this
# (use --env on the docker run command line to override)
ENV JAVA_OPTS="-Xmx8G"

# the port on which owlery will be listening within the container
EXPOSE 8080

# run the service under a designated user rather than root
USER $OWLERY_USER

# by default, the application config file is expected at this location
# (override on the docker run commandline by providing a trailing argument)
CMD ["-Dconfig.file=/srv/conf/application.conf"]

# this will have to run on port 8080 and bind to 0.0.0.0 to work, so we
# override here on the command line whatever the configuration file has
ENTRYPOINT ["/usr/share/owlery/bin/owlery", "-Dowlery.port=8080", "-Dowlery.host=0.0.0.0"]