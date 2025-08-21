# Usa l'immagine base di Wildfly
FROM jboss/wildfly:latest
# ALTERNATIVA (se mantieni Hibernate 6.x): FROM jboss/wildfly:27.0.0.Final

# Crea una directory temporanea per il driver e lo script di setup
RUN mkdir -p /opt/jboss/container-setup

# Copia il driver JDBC MySQL nel container
COPY mysql-connector-j-8.0.32.jar /opt/jboss/container-setup/

# Copia lo script di configurazione e rendilo eseguibile
COPY --chmod=0755 setup.sh /opt/jboss/

# Copia il file WAR specifico di questo modulo nel container
COPY build/libs/gestionequest.war /opt/jboss/container-setup/

# Espone la porta per il traffico HTTP (8085 è la porta interna di WildFly)
EXPOSE 8085

# Comando per avviare Wildfly tramite il tuo script di setup
CMD ["/opt/jboss/setup.sh"]