#!/bin/sh
# Runs on helios: installs Tomcat, creates one HTTPS-only instance per service,
# deploys the WARs from ~/soa/dist and (re)starts everything.
# The WARs are thin: their libraries are loaded once from ~/soa/lib via shared.loader.
set -eu
ulimit -c 0

TOMCAT_VERSION=10.1.60
SOA=$HOME/soa
DIST=$SOA/dist
export JAVA_HOME=/usr/local/openjdk21
export CATALINA_HOME=$SOA/apache-tomcat-$TOMCAT_VERSION

TICKET_PORT=41390
PERSON_PORT=41391
BOOKING_PORT=41392
CLIENT_PORT=41393

if [ ! -d "$CATALINA_HOME" ]; then
    fetch -o "$SOA/tomcat.tar.gz" \
        "https://dlcdn.apache.org/tomcat/tomcat-10/v$TOMCAT_VERSION/bin/apache-tomcat-$TOMCAT_VERSION.tar.gz"
    tar -xzf "$SOA/tomcat.tar.gz" -C "$SOA"
    rm -rf "$SOA/tomcat.tar.gz" "$CATALINA_HOME/webapps"
fi

if [ ! -f "$SOA/keystore.p12" ]; then
    umask 077
    openssl rand -hex 16 > "$SOA/keystore.pass"
    "$JAVA_HOME/bin/keytool" -J-Xmx64m -genkeypair -alias soa -keyalg RSA -keysize 2048 -validity 365 \
        -dname "CN=helios.cs.ifmo.ru" -ext "SAN=dns:localhost,dns:helios.cs.ifmo.ru,ip:127.0.0.1" \
        -keystore "$SOA/keystore.p12" -storetype PKCS12 -storepass "$(cat "$SOA/keystore.pass")"
    "$JAVA_HOME/bin/keytool" -J-Xmx64m -exportcert -alias soa -keystore "$SOA/keystore.p12" \
        -storepass "$(cat "$SOA/keystore.pass")" -rfc -file "$SOA/server.crt"
    "$JAVA_HOME/bin/keytool" -J-Xmx64m -importcert -noprompt -alias soa -file "$SOA/server.crt" \
        -keystore "$SOA/truststore.p12" -storetype PKCS12 -storepass changeit
    umask 022
fi

export KEYSTORE=$SOA/keystore.p12
export KEYSTORE_PASSWORD=$(cat "$SOA/keystore.pass")
export DB_URL=jdbc:postgresql://pg:5432/studs
export DB_USER=$(id -un)
export DB_PASSWORD=$(head -1 "$HOME/.pgpass" | cut -d: -f5)
export TICKET_SERVICE_URL=https://localhost:$TICKET_PORT
export PERSON_SERVICE_URL=https://localhost:$PERSON_PORT
export BOOKING_SERVICE_URL=https://localhost:$BOOKING_PORT
JVM_OPTS="-Xmx128m -Xss512k -XX:+UseSerialGC -XX:TieredStopAtLevel=1 -XX:MaxMetaspaceSize=160m -XX:CompressedClassSpaceSize=48m -XX:ReservedCodeCacheSize=48m -XX:MaxDirectMemorySize=32m -Djavax.net.ssl.trustStore=$SOA/truststore.p12 -Djavax.net.ssl.trustStorePassword=changeit"

instance() { # name https-port shutdown-port
    base=$SOA/tomcat-${1%-service}
    if [ -f "$base/conf/server.xml" ]; then
        CATALINA_BASE=$base "$CATALINA_HOME/bin/shutdown.sh" >/dev/null 2>&1 || true
        sleep 3
    fi
    rm -rf "$base"
    mkdir -p "$base/conf" "$base/logs" "$base/temp" "$base/webapps" "$base/work"
    cp "$CATALINA_HOME/conf/web.xml" "$CATALINA_HOME/conf/logging.properties" "$base/conf/"
    sed "s|^shared.loader=.*|shared.loader=\"$SOA/lib/*.jar\"|" "$CATALINA_HOME/conf/catalina.properties" \
        > "$base/conf/catalina.properties"
    # Only an HTTPS connector exists, so plain HTTP is not served at all.
    cat > "$base/conf/server.xml" <<XML
<Server port="$3" shutdown="SHUTDOWN">
  <Service name="Catalina">
    <Connector port="$2" protocol="org.apache.coyote.http11.Http11NioProtocol"
               SSLEnabled="true" scheme="https" secure="true" maxThreads="50">
      <SSLHostConfig>
        <Certificate certificateKeystoreFile="\${KEYSTORE}" certificateKeystorePassword="\${KEYSTORE_PASSWORD}"
                     certificateKeystoreType="PKCS12"/>
      </SSLHostConfig>
    </Connector>
    <Engine name="Catalina" defaultHost="localhost">
      <Host name="localhost" appBase="webapps" unpackWARs="false" autoDeploy="false"/>
    </Engine>
  </Service>
</Server>
XML
    cp "$DIST/$1.war" "$base/webapps/ROOT.war"
    CATALINA_BASE=$base CATALINA_OPTS="$JVM_OPTS -Dorg.apache.tomcat.util.digester.PROPERTY_SOURCE=org.apache.tomcat.util.digester.EnvironmentPropertySource" \
        "$CATALINA_HOME/bin/startup.sh" >/dev/null
}

instance ticket-service $TICKET_PORT 41395
instance person-service $PERSON_PORT 41396
instance booking-service $BOOKING_PORT 41397
instance client $CLIENT_PORT 41398
