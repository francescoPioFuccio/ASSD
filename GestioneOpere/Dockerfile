# Usa un'immagine WildFly moderna con JDK 17
FROM quay.io/wildfly/wildfly:32.0.1.Final-jdk17

# Aumenta la memoria della JVM per garantire un avvio stabile
USER root
RUN sed -i 's/-Xms64m/-Xms128m/' /opt/jboss/wildfly/bin/standalone.conf && \
    sed -i 's/-Xmx512m/-Xmx384m/' /opt/jboss/wildfly/bin/standalone.conf && \
    sed -i 's/MaxMetaspaceSize=256m/MaxMetaspaceSize=128m/' /opt/jboss/wildfly/bin/standalone.conf
USER jboss

# Crea la directory di setup e imposta i permessi corretti
USER root
RUN mkdir -p /opt/jboss/container-setup && \
    chown -R jboss:jboss /opt/jboss/container-setup
USER jboss

# Copia gli artefatti necessari
COPY mysql-connector-j-8.0.32.jar /opt/jboss/container-setup/
COPY --chmod=755 setup.sh /opt/jboss/

# --- MODIFICA QUESTA RIGA PER OGNI MODULO ---
COPY build/libs/gestioneopere.war /opt/jboss/container-setup/

# Espone la porta interna
EXPOSE 8085

# Avvia tramite lo script
CMD ["/opt/jboss/setup.sh"]