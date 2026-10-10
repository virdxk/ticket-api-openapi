#!/bin/sh
# Builds the services locally (helios has no access to Maven Central), deploys them to helios
# and publishes the client and the per-service proxies in public_html (https://se.ifmo.ru/~s412939/).
set -eu
cd "$(dirname "$0")/.."

rm -rf target/lib target/dist
mvn -q package dependency:copy-dependencies -DincludeScope=runtime -DoutputDirectory="$PWD/target/lib" -DskipTests
mkdir -p target/dist
cp ticket-service/target/ticket-service.war person-service/target/person-service.war \
    booking-service/target/booking-service.war deploy/helios.sh target/dist/
tar -czf - -C target lib dist | ssh -p 2222 s412939@helios.cs.ifmo.ru \
    'mkdir -p soa && rm -rf soa/lib soa/dist && tar -xzf - -C soa && sh soa/dist/helios.sh'
tar -czf - -C client . | ssh -p 2222 s412939@helios.cs.ifmo.ru \
    'rm -rf public_html/lab2 && mkdir -p public_html/lab2 && tar -xzf - -C public_html/lab2 && chmod -R a+rX public_html/lab2'
for s in ticket-service person-service booking-service; do
    tar -czf - -C deploy/proxy . | ssh -p 2222 s412939@helios.cs.ifmo.ru \
        "rm -rf public_html/$s && mkdir -p public_html/$s && tar -xzf - -C public_html/$s \
         && cp soa/server.crt public_html/$s/ && chmod -R a+rX public_html/$s"
done
