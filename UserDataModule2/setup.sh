#!/bin/bash

# --- PASSO 1: GENERAZIONE DELLO SCRIPT CLI PER IL DATASOURCE ---
# Questo crea un file .cli temporaneo con i dettagli del tuo database.
# Le variabili d'ambiente (es. $DB_HOST) sono passate dal docker-compose.yml.
# I valori dopo :- (es. :-localhost) sono fallback se le variabili non sono impostate.
echo "Generating dynamic datasource configuration script..."
cat > /opt/jboss/container-setup/configure-datasource-dynamic.cli <<EOT
# Aggiungi il modulo per il driver MySQL
# 'com.mysql.cj' è il nome del modulo, mysql-connector-j-8.0.32.jar è il file del driver.
# Assicurati che il nome del file .jar sia corretto per la tua versione (es. mysql-connector-j-8.0.32.jar).
module add --name=com.mysql.cj --resources=/opt/jboss/container-setup/mysql-connector-j-8.0.32.jar --dependencies=javax.api,javax.transaction.api

# Aggiungi il driver JDBC MySQL a WildFly
# 'mysql' è il nome del driver logico, com.mysql.cj.jdbc.Driver è la classe Java del driver.
/subsystem=datasources/jdbc-driver=mysql:add(driver-name=mysql,driver-module-name=com.mysql.cj,driver-class-name=com.mysql.cj.jdbc.Driver)

# Aggiungi il datasource JTA per la tua applicazione
# jndi-name="java:/MySqlDS1" DEVE CORRISPONDERE A QUANTO HAI NEL persistence.xml
# Ho rimosso 'pool-name' che non è supportato in WildFly 25 per questa configurazione.
/subsystem=datasources/data-source=MySqlDS1:add(connection-url="jdbc:mysql://${DB_HOST:-localhost}:${DB_PORT:-3306}/${DB_NAME:-your_database}?useSSL=false&allowPublicKeyRetrieval=true",driver-name="mysql",jndi-name="java:/MySqlDS1",user-name="${DB_USER:-root}",password="${DB_PASSWORD:-password}",valid-connection-checker-class-name="org.jboss.jca.adapters.jdbc.extensions.mysql.MySQLValidConnectionChecker",exception-sorter-class-name="org.jboss.jca.adapters.jdbc.extensions.mysql.MySQLExceptionSorter",background-validation=true,background-validation-millis=10000)

# Abilita il datasource appena creato
/subsystem=datasources/data-source=MySqlDS1:enable
EOT
echo "Dynamic datasource configuration script generated."

# --- PASSO 2: AVVIO DI WILDFLY IN BACKGROUND ---
# Avvia WildFly in modalità standalone e lo mette in background (&).
# -b 0.0.0.0 fa sì che WildFly ascolti su tutte le interfacce di rete, rendendolo accessibile dall'esterno del container.
echo "Starting Wildfly in background..."
/opt/jboss/wildfly/bin/standalone.sh -b 0.0.0.0 &

# --- PASSO 3: ATTESA DELL'AVVIO DI WILDFLY (più robusto) ---
echo "Waiting for Wildfly to fully start and be accessible via CLI..."
until /opt/jboss/wildfly/bin/jboss-cli.sh --connect --timeout=5 --commands="ls /deployment" > /dev/null 2>&1; do
  echo "Wildfly non ancora avviato... in attesa..."
  sleep 5
done
echo "Wildfly avviato e CLI accessibile."

# --- PASSO 4: ESECUZIONE DELLA CONFIGURAZIONE CLI ---
# Una volta che WildFly è su, si connette al CLI e esegue lo script generato.
echo "Running Wildfly CLI database configuration..."
/opt/jboss/wildfly/bin/jboss-cli.sh --connect --file=/opt/jboss/container-setup/configure-datasource-dynamic.cli

# Controlla il codice di uscita del comando CLI. Se diverso da 0, c'è stato un errore.
if [ $? -ne 0 ]; then
    echo "ERROR: Wildfly CLI database configuration failed! Check logs above for details."
    exit 1 # Esci con errore se la configurazione del DB fallisce
else
    echo "Wildfly CLI database configuration completed successfully."
fi

# --- NUOVO PASSO: DEPLOYMENT DELL'APPLICAZIONE WAR ---
echo "Deploying usermodule3.war..."
/opt/jboss/wildfly/bin/jboss-cli.sh --connect --commands="deploy /opt/jboss/container-setup/usermodule3.war"

# Controlla il codice di uscita del comando di deploy.
if [ $? -ne 0 ]; then
    echo "ERROR: Deployment of usermodule3.war failed! Check logs above for details."
    exit 1 # Esci con errore se il deployment fallisce
else
    echo "usermodule3.war deployed successfully."
fi

# --- PASSO 5: MANTENERE IL CONTAINER IN ESECUZIONE ---
# Il 'wait $!' attende il processo di WildFly che è stato avviato in background.
# Senza questo, il container si chiuderebbe non appena lo script setup.sh finisce.
echo "Wildfly setup finished. Keeping Wildfly running."
wait $!