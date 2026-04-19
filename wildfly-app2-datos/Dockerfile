FROM quay.io/wildfly/wildfly:31.0.0.Final-jdk17

USER root

# Instalar módulo PostgreSQL
RUN mkdir -p /opt/jboss/wildfly/modules/org/postgresql/main
RUN curl -fL https://jdbc.postgresql.org/download/postgresql-42.7.3.jar \
    -o /opt/jboss/wildfly/modules/org/postgresql/main/postgresql.jar
COPY module.xml /opt/jboss/wildfly/modules/org/postgresql/main/module.xml

# Aplicar configuración via CLI embebido
COPY configure.cli /opt/configure.cli
RUN /opt/jboss/wildfly/bin/jboss-cli.sh --file=/opt/configure.cli

# Copiar WAR
COPY target/exam-datos.war /opt/jboss/wildfly/standalone/deployments/

# Script de inicio
COPY src/main/resources/start.sh /opt/start.sh
RUN chmod +x /opt/start.sh

# Corregir permisos
RUN chown -R jboss:root /opt/jboss/wildfly/standalone && \
    chmod -R 775 /opt/jboss/wildfly/standalone && \
    chown -R jboss:root /opt/jboss/wildfly/modules/org/postgresql && \
    chmod -R 755 /opt/jboss/wildfly/modules/org/postgresql

USER jboss
EXPOSE 8080 9990
CMD ["/opt/start.sh"]
