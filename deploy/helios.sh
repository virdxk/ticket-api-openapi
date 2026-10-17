#!/bin/sh
set -eu
ulimit -c 0

TOMCAT_VERSION=10.1.60
SOA=$HOME/soa
DIST=$SOA/dist
export JAVA_HOME=/usr/local/openjdk21
export CATALINA_HOME=$SOA/apache-tomcat-$TOMCAT_VERSION

if [ ! -d "$CATALINA_HOME" ]; then
    fetch -o "$SOA/tomcat.tar.gz" \
        "https://dlcdn.apache.org/tomcat/tomcat-10/v$TOMCAT_VERSION/bin/apache-tomcat-$TOMCAT_VERSION.tar.gz"
    tar -xzf "$SOA/tomcat.tar.gz" -C "$SOA"
    rm -rf "$SOA/tomcat.tar.gz" "$CATALINA_HOME/webapps"
fi

export DB_URL=jdbc:postgresql://pg:5432/studs
export DB_USER=$(id -un)
export DB_PASSWORD=$(head -1 "$HOME/.pgpass" | cut -d: -f5)
export CATALINA_OPTS="-Xmx128m -Xss512k -XX:+UseSerialGC -XX:TieredStopAtLevel=1 -XX:MaxMetaspaceSize=160m -XX:CompressedClassSpaceSize=48m -XX:ReservedCodeCacheSize=48m -XX:MaxDirectMemorySize=32m"

instance() {
    base=$SOA/tomcat-${1%-service}
    while pkill -f "catalina.base=$base " 2>/dev/null; do sleep 2; done
    rm -rf "$base"
    mkdir -p "$base/conf" "$base/logs" "$base/temp" "$base/webapps" "$base/work"
    cp "$CATALINA_HOME/conf/web.xml" "$CATALINA_HOME/conf/logging.properties" "$DIST/tomcat/keystore.p12" "$base/conf/"
    sed "s|^shared.loader=.*|shared.loader=\"$SOA/lib/*.jar\"|" "$CATALINA_HOME/conf/catalina.properties" \
        > "$base/conf/catalina.properties"
    sed "s|HTTPS_PORT|$2|; s|SHUTDOWN_PORT|$3|" "$DIST/tomcat/server.xml" > "$base/conf/server.xml"
    chmod 600 "$base/conf/keystore.p12" "$base/conf/server.xml"
    cp "$DIST/$1.war" "$base/webapps/ROOT.war"
    CATALINA_BASE=$base "$CATALINA_HOME/bin/startup.sh" >/dev/null
}

instance ticket-service 41390 41395
instance person-service 41391 41396
instance booking-service 41392 41397
