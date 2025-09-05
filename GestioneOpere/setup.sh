#!/bin/bash

# =============================================================================
#  SCRIPT DI SETUP ROBUSTO PER WILDFLY
#  Questo script esegue la configurazione e il deploy in modo atomico
#  per evitare race condition e garantire un avvio pulito.
# =============================================================================

WILDFLY_HOME="/opt/jboss/wildfly"
WAR_FILE=$(find /opt/jboss/container-setup -name "*.war" | head -n 1)

if [ -z "$WAR_FILE" ]; then
    echo "FATAL: No .war file found in /opt/jboss/container-setup/. Aborting."
    exit 1
fi
echo "Found WAR file to deploy: $WAR_FILE"

# 1. Avvia WildFly in modalità admin-only.
#    In questa modalità, solo la console di amministrazione è attiva,
#    il che permette una configurazione pulita senza che le applicazioni partano.
echo "--> Starting WildFly in admin-only mode..."
$WILDFLY_HOME/bin/standalone.sh -b 0.0.0.0 --admin-only &
WILDFLY_PID=$!

# 2. Attendi che il server sia pronto per accettare comandi.
#    Questo ciclo controlla attivamente lo stato del server invece di usare un 'sleep' fisso.
echo "--> Waiting for WildFly to be ready for configuration..."
until $WILDFLY_HOME/bin/jboss-cli.sh -c "ls /subsystem=datasources" &> /dev/null; do
  echo "    WildFly not ready yet, waiting 5 seconds..."
  sleep 5
done
echo "--> WildFly is ready for configuration."

# 3. Esegui TUTTA la configurazione e il deploy in un unico blocco (batch).
#    Questo è atomico: o tutti i comandi hanno successo, o l'intera operazione viene annullata.
#    Questo risolve il problema della datasource mancante.
echo "--> Configuring datasource and deploying application..."
$WILDFLY_HOME/bin/jboss-cli.sh --connect <<EOF
batch
# Configura il modulo del driver MySQL
module add --name=com.mysql.cj --resources=/opt/jboss/container-setup/mysql-connector-j-8.0.32.jar --dependencies=javax.api,javax.transaction.api
# Configura il driver JDBC
/subsystem=datasources/jdbc-driver=mysql:add(driver-name=mysql,driver-module-name=com.mysql.cj)
# Configura e abilita la datasource
/subsystem=datasources/data-source=MySqlDS1:add(connection-url="jdbc:mysql://${DB_HOST}:${DB_PORT}/${DB_NAME}",jndi-name="java:/MySqlDS1",user-name="${DB_USER}",password="${DB_PASSWORD}",driver-name=mysql,enabled=true,use-ccm=true)
# Deploy dell'applicazione WAR
deploy $WAR_FILE --force-deploy
# Esegui tutti i comandi del batch
run-batch
EOF

# 4. Controlla se il batch di configurazione e deploy ha avuto successo.
if [ $? -ne 0 ]; then
    echo "FATAL: Configuration or deployment failed. See logs above. Shutting down."
    # Se fallisce, spegni il server. Questo farà fallire il container,
    # rendendo l'errore immediatamente visibile.
    kill $WILDFLY_PID
    exit 1
fi
echo "--> Configuration and deployment successful."

# 5. Ferma il server in modalità admin-only per poterlo riavviare normalmente.
echo "--> Shutting down admin-only server..."
$WILDFLY_HOME/bin/jboss-cli.sh -c ":shutdown"

# Attendi che il processo di WildFly termini completamente.
wait $WILDFLY_PID

# 6. Avvia WildFly in modalità normale.
#    Il server ora partirà con la configurazione corretta e l'applicazione già deployata.
$WILDFLY_HOME/bin/standalone.sh -b 0.0.0.0 -bmanagement 0.0.0.0 -Djboss.http.port=8085 -Djava.security.egd=file:/dev/./urandom