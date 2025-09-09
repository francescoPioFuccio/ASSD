# Dockerfile per il modulo Kafka
FROM jboss/wildfly:latest

# Crea directory di setup
RUN mkdir -p /opt/jboss/container-setup

# Copia il driver JDBC MySQL
COPY mysql-connector-j-8.0.32.jar /opt/jboss/container-setup/

# Copia lo script di configurazione e rendilo eseguibile
COPY --chmod=0755 setup.sh /opt/jboss/

# Copia il file WAR del modulo Kafka
COPY build/libs/Kafka.war /opt/jboss/container-setup/

# Espone la porta HTTP di WildFly
EXPOSE 8085

# Avvia WildFly tramite script di setup
CMD ["/opt/jboss/setup.sh"]