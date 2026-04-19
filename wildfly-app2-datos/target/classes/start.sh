#!/bin/bash
echo "Iniciando WildFly 2 - Capa de Datos..."
sleep 20
exec /opt/jboss/wildfly/bin/standalone.sh \
  -b 0.0.0.0 \
  -bmanagement 0.0.0.0 \
  -c standalone.xml
