#!/bin/bash
# Esperar a que RabbitMQ y Postgres estén listos
echo "Esperando servicios externos..."
sleep 15

# Iniciar WildFly con configuración standalone
exec /opt/jboss/wildfly/bin/standalone.sh \
  -b 0.0.0.0 \
  -bmanagement 0.0.0.0 \
  -c standalone.xml
