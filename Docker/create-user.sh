#!/bin/sh

GROUP=${2:-owlery}
USER=${1:-owlery}

# Adding desired system group
if ! getent group $GROUP > /dev/null 2>&1 ; then
    echo "Creating system group: $GROUP"
    addgroup --system $GROUP
fi

# Adding desired system user
if ! id -u $USER > /dev/null 2>&1; then
    echo "Creating user $USER in group $GROUP"
    useradd --system --no-create-home --gid $GROUP --shell /bin/false $USER
fi
