#!/bin/sh
# Builds everything locally (helios has no access to Maven Central) and deploys to helios.
set -eu
cd "$(dirname "$0")/.."

rm -rf target/lib target/dist
mvn -q package dependency:copy-dependencies -DincludeScope=runtime -DoutputDirectory="$PWD/target/lib" -DskipTests
mkdir -p target/dist
cp ticket-service/target/ticket-service.war person-service/target/person-service.war \
    booking-service/target/booking-service.war client/target/client.war deploy/helios.sh target/dist/
tar -czf - -C target lib dist | ssh -p 2222 s412939@helios.cs.ifmo.ru \
    'mkdir -p soa && rm -rf soa/lib soa/dist && tar -xzf - -C soa && sh soa/dist/helios.sh'
