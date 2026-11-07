#!/bin/sh
set -eu
set -o pipefail

export JAVA_HOME=/usr/local/openjdk21


export DB_URL=jdbc:postgresql://pg:5432/studs
export DB_USER=$(whoami)

rest=${DB_URL#jdbc:postgresql://}
hostport=${rest%%/*}
db=${rest#*/}
db=${db%%\?*}
host=${hostport%%:*}
case $hostport in
    *:*) port=${hostport##*:} ;;
    *) port=5432 ;;
esac

DB_PASSWORD=$(awk -v host="$host" -v port="$port" -v db="$db" -v user="$DB_USER" '
    /^[ 	]*(#|$)/ { next }
    {
        n = 1; esc = 0
        delete f
        f[1] = ""
        for (i = 1; i <= length($0); i++) {
            c = substr($0, i, 1)
            if (esc) { f[n] = f[n] c; esc = 0; continue }
            if (c == "\\") { esc = 1; continue }
            if (c == ":") { n++; f[n] = ""; continue }
            f[n] = f[n] c
        }
        if (n < 5) { next }
        if ((f[1] == "*" || f[1] == host) && (f[2] == "*" || f[2] == port) &&
            (f[3] == "*" || f[3] == db) && (f[4] == "*" || f[4] == user)) {
            print f[5]
            exit
        }
    }
' "$HOME/.pgpass")
if [ -z "$DB_PASSWORD" ]; then
    echo "helios.sh: no $HOME/.pgpass line matches $host:$port:$db for user $DB_USER" >&2
    exit 1
fi
export DB_PASSWORD
export CATALINA_OPTS="-Xmx128m -Xss512k -XX:+UseSerialGC -XX:TieredStopAtLevel=1 -XX:MaxMetaspaceSize=160m -XX:CompressedClassSpaceSize=48m -XX:ReservedCodeCacheSize=48m -XX:MaxDirectMemorySize=32m"

TLS=$HOME/soa/tls
STOREPASS=soa-lab2-tls

keystore_ok() {
    [ -f "$TLS/keystore.p12" ] || return 1
    openssl pkcs12 -in "$TLS/keystore.p12" -nokeys -passin "pass:$STOREPASS" 2>/dev/null |
        openssl x509 -noout -checkend 604800 >/dev/null 2>&1
}

cert_ok() {
    [ -f "$TLS/server.crt" ] || return 1
    openssl x509 -in "$TLS/server.crt" -noout -checkend 604800 >/dev/null 2>&1
}

truststore_ok() {
    [ -f "$TLS/truststore.p12" ] || return 1
    openssl pkcs12 -in "$TLS/truststore.p12" -nokeys -passin "pass:$STOREPASS" >/dev/null 2>&1
}

mkdir -p "$TLS"
if ! keystore_ok; then
    echo "helios.sh: keystore missing or certificate expires within 7 days; generating a new key pair"
    rm -f "$TLS/keystore.p12" "$TLS/server.crt" "$TLS/truststore.p12"
    "$JAVA_HOME/bin/keytool" -J-Xmx64m -genkeypair -alias soa -keyalg RSA -keysize 2048 -validity 365 \
        -dname "CN=helios.cs.ifmo.ru" -ext "SAN=dns:localhost,dns:helios.cs.ifmo.ru,ip:127.0.0.1" \
        -keystore "$TLS/keystore.p12" -storetype PKCS12 -storepass "$STOREPASS"
fi
if ! cert_ok; then
    echo "helios.sh: exporting server certificate"
    "$JAVA_HOME/bin/keytool" -J-Xmx64m -exportcert -alias soa -rfc -file "$TLS/server.crt" \
        -keystore "$TLS/keystore.p12" -storepass "$STOREPASS"
fi
if ! truststore_ok; then
    echo "helios.sh: rebuilding truststore"
    rm -f "$TLS/truststore.p12"
    "$JAVA_HOME/bin/keytool" -J-Xmx64m -importcert -noprompt -alias soa -file "$TLS/server.crt" \
        -keystore "$TLS/truststore.p12" -storetype PKCS12 -storepass "$STOREPASS"
fi
chmod 600 "$TLS/keystore.p12" "$TLS/truststore.p12"
for s in ticket-service person-service booking-service; do
    cp "$TLS/server.crt" "$HOME/public_html/$s/"
done

wait_ready() {
    name=$1
    port=$2
    case $name in
        ticket-service) path=/tickets ;;
        person-service) path=/persons ;;
        booking-service) path=/booking/bookings ;;
        *) return 1 ;;
    esac
    i=0
    while [ "$i" -lt 30 ]; do
        if curl -sfk "https://localhost:$port$path?page=1&size=1" >/dev/null 2>&1; then
            return 0
        fi
        i=$((i + 1))
        sleep 2
    done
    return 1
}

stop_instance() {
    base=$1
    i=0
    while pkill -f "catalina.base=$base " 2>/dev/null; do
        i=$((i + 1))
        if [ "$i" -ge 15 ]; then
            echo "helios.sh: $base did not stop within 30s; sending SIGKILL" >&2
            pkill -9 -f "catalina.base=$base " 2>/dev/null || true
            sleep 2
            if pkill -f "catalina.base=$base " 2>/dev/null; then
                echo "helios.sh: instance $base is still running after SIGKILL; giving up" >&2
                exit 1
            fi
            return 0
        fi
        sleep 2
    done
}

instance() {
    name=$1
    base=$HOME/soa/tomcat-${name%-service}
    new=$base.new.$$
    backup=

    rm -rf "$new"
    mkdir -p "$new/conf" "$new/logs" "$new/temp" "$new/webapps" "$new/work"
    cp "$HOME/soa/apache-tomcat-10.1.60/conf/web.xml" "$HOME/soa/apache-tomcat-10.1.60/conf/logging.properties" "$TLS/keystore.p12" "$new/conf/"
    sed "s|^shared.loader=.*|shared.loader=\"$HOME/soa/lib/*.jar\"|" "$HOME/soa/apache-tomcat-10.1.60/conf/catalina.properties" \
        > "$new/conf/catalina.properties"
    sed "s|HTTPS_PORT|$2|; s|SHUTDOWN_PORT|$3|" "$HOME/soa/dist/tomcat/server.xml" > "$new/conf/server.xml"
    chmod 600 "$new/conf/keystore.p12" "$new/conf/server.xml"
    cp "$HOME/soa/dist/$name.war" "$new/webapps/ROOT.war"

    stop_instance "$base"

    if [ -d "$base" ]; then
        backup="$base.bak.$(date +%Y%m%d%H%M%S)"
        mv "$base" "$backup"
        cp -a "$backup/logs/." "$new/logs/" 2>/dev/null || true
    fi
    mv "$new" "$base"

    CATALINA_BASE=$base "$HOME/soa/apache-tomcat-10.1.60/bin/startup.sh" >/dev/null

    if ! wait_ready "$name" "$2"; then
        echo "helios.sh: $name did not answer on port $2 within 60s" >&2
        if [ -f "$base/logs/catalina.out" ]; then
            tail -n 30 "$base/logs/catalina.out" >&2
        fi
        if [ -n "$backup" ]; then
            echo "helios.sh: previous instance kept at $backup" >&2
        fi
        exit 1
    fi
    echo "helios.sh: $name is up on port $2${backup:+; previous instance: $backup}"
}

instance ticket-service 41390 41395
instance person-service 41391 41396
instance booking-service 41392 41397
