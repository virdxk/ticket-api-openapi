#!/bin/sh
set -e
cd "$(dirname "$0")/.."
H=s412939@helios.cs.ifmo.ru

mvn clean package dependency:copy-dependencies -DincludeScope=runtime -DoutputDirectory="$PWD/target/lib" -DskipTests
(cd client && npm install && npm run build)

rsync -az --delete -e 'ssh -p 2222' target/lib/ $H:soa/lib/
rsync -az -e 'ssh -p 2222' */target/*-service.war deploy/helios.sh deploy/tomcat $H:soa/dist/
rsync -az --delete -e 'ssh -p 2222' client/dist/ $H:public_html/lab2/
for s in ticket-service person-service booking-service; do
    rsync -az -e 'ssh -p 2222' deploy/proxy/ $H:public_html/$s/
done
ssh -p 2222 $H sh soa/dist/helios.sh
