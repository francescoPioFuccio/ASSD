# ... altre righe invariate ...
# Crea una directory temporanea per il driver e lo script di setup
RUN mkdir -p /opt/jboss/container-setup

# Copia il driver JDBC MySQL nel container
COPY mysql-connector-j-8.0.32.jar /opt/jboss/container-setup/

# Copia lo script di configurazione e rendilo eseguibile
COPY --chmod=0755 setup.sh /opt/jboss/

# MODIFICA QUI: Assicurati che il nome del WAR corrisponda a quello impostato in UserDataModule2/build.gradle.kts
COPY build/libs/usermodule3.war /opt/jboss/container-setup/

# Espone la porta per il traffico HTTP (8085 è la porta interna di WildFly)
EXPOSE 8085

# Comando per avviare Wildfly tramite il tuo script di setup
CMD ["/opt/jboss/setup.sh"]