FROM quay.io/wildfly/wildfly:32.0.1.Final-jdk17

# Crea directory come utente root, poi cambia proprietario
USER root
RUN mkdir -p /opt/jboss/container-setup && \
    chown jboss:jboss /opt/jboss/container-setup

# Torna all'utente jboss
USER jboss

# Copia il driver JDBC MySQL nel container
COPY mysql-connector-j-8.0.32.jar /opt/jboss/container-setup/

# Copia lo script di configurazione e rendilo eseguibile
COPY --chmod=0755 setup.sh /opt/jboss/

# Copia il file WAR specifico di questo modulo nel container
COPY build/libs/gestoremusei.war /opt/jboss/container-setup/

# Espone la porta per il traffico HTTP (8085 è la porta interna di WildFly)
EXPOSE 8085

# Comando per avviare Wildfly tramite il tuo script di setup
CMD ["/opt/jboss/setup.sh"]