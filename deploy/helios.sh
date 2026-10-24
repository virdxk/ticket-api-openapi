#!/bin/sh

export JAVA_HOME=/usr/local/openjdk21

#if [ ! -d "$HOME/soa/apache-tomcat-10.1.60" ]; then
#    fetch -o "$HOME/soa/tomcat.tar.gz" \
#        "https://dlcdn.apache.org/tomcat/tomcat-10/v10.1.60/bin/apache-tomcat-10.1.60.tar.gz"
#    tar -xzf "$HOME/soa/tomcat.tar.gz" -C "$HOME/soa"
#    rm -rf "$HOME/soa/tomcat.tar.gz" "$HOME/soa/apache-tomcat-10.1.60/webapps"
#fi

export DB_URL=jdbc:postgresql://pg:5432/studs
export DB_USER=$(whoami)
export DB_PASSWORD=$(head -1 "$HOME/.pgpass" | cut -d: -f5)
export CATALINA_OPTS="-Xmx128m -Xss512k -XX:+UseSerialGC -XX:TieredStopAtLevel=1 -XX:MaxMetaspaceSize=160m -XX:CompressedClassSpaceSize=48m -XX:ReservedCodeCacheSize=48m -XX:MaxDirectMemorySize=32m"

TLS=$HOME/soa/tls
if [ ! -f "$TLS/keystore.p12" ]; then
    mkdir -p "$TLS"
    "$JAVA_HOME/bin/keytool" -J-Xmx64m -genkeypair -alias soa -keyalg RSA -keysize 2048 -validity 365 \
        -dname "CN=helios.cs.ifmo.ru" -ext "SAN=dns:localhost,dns:helios.cs.ifmo.ru,ip:127.0.0.1" \
        -keystore "$TLS/keystore.p12" -storetype PKCS12 -storepass soa-lab2-tls
    "$JAVA_HOME/bin/keytool" -J-Xmx64m -exportcert -alias soa -rfc -file "$TLS/server.crt" \
        -keystore "$TLS/keystore.p12" -storepass soa-lab2-tls
    "$JAVA_HOME/bin/keytool" -J-Xmx64m -importcert -noprompt -alias soa -file "$TLS/server.crt" \
        -keystore "$TLS/truststore.p12" -storetype PKCS12 -storepass soa-lab2-tls
    chmod 600 "$TLS/keystore.p12" "$TLS/truststore.p12"
fi
for s in ticket-service person-service booking-service; do
    cp "$TLS/server.crt" "$HOME/public_html/$s/"
done

instance() {
    base=$HOME/soa/tomcat-${1%-service}
    while pkill -f "catalina.base=$base " 2>/dev/null; do sleep 2; done
    rm -rf "$base"
    mkdir -p "$base/conf" "$base/logs" "$base/temp" "$base/webapps" "$base/work"
    cp "$HOME/soa/apache-tomcat-10.1.60/conf/web.xml" "$HOME/soa/apache-tomcat-10.1.60/conf/logging.properties" "$HOME/soa/tls/keystore.p12" "$base/conf/"
    sed "s|^shared.loader=.*|shared.loader=\"$HOME/soa/lib/*.jar\"|" "$HOME/soa/apache-tomcat-10.1.60/conf/catalina.properties" \
        > "$base/conf/catalina.properties"
    sed "s|HTTPS_PORT|$2|; s|SHUTDOWN_PORT|$3|" "$HOME/soa/dist/tomcat/server.xml" > "$base/conf/server.xml"
    chmod 600 "$base/conf/keystore.p12" "$base/conf/server.xml"
    cp "$HOME/soa/dist/$1.war" "$base/webapps/ROOT.war"
    CATALINA_BASE=$base "$HOME/soa/apache-tomcat-10.1.60/bin/startup.sh" >/dev/null
}

instance ticket-service 41390 41395
instance person-service 41391 41396
instance booking-service 41392 41397