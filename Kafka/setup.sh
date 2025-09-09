#!/bin/bash

# Script di setup per WildFly con configurazione MySQL e deploy automatico
set -e

echo "=== Avvio configurazione WildFly ==="

# Avvia WildFly in background
echo "Avvio WildFly..."
$JBOSS_HOME/bin/standalone.sh -b 0.0.0.0 -bmanagement 0.0.0.0 &
WILDFLY_PID=$!

# Attendi che WildFly sia completamente avviato
echo "Attendo l'avvio completo di WildFly..."
until $JBOSS_HOME/bin/jboss-cli.sh --connect --command=":read-attribute(name=server-state)" 2>/dev/null | grep -q "success"; do
    sleep 2
    echo "WildFly non ancora pronto, attendo..."
done

echo "WildFly avviato con successo!"

# Aggiungi il driver MySQL
echo "Configurazione driver MySQL..."
$JBOSS_HOME/bin/jboss-cli.sh --connect --command="module add --name=com.mysql --resources=/opt/jboss/container-setup/mysql-connector-j-8.0.32.jar --dependencies=javax.api,javax.transaction.api"

# Registra il driver
$JBOSS_HOME/bin/jboss-cli.sh --connect --command="/subsystem=datasources/jdbc-driver=mysql:add(driver-name=\"mysql\",driver-module-name=\"com.mysql\",driver-class-name=com.mysql.cj.jdbc.Driver)"

# Aspetta che le variabili d'ambiente siano disponibili
echo "Configurazione DataSource con parametri:"
echo "DB_HOST: ${DB_HOST:-mysql_db}"
echo "DB_PORT: ${DB_PORT:-3306}"
echo "DB_NAME: ${DB_NAME:-assdproject}"
echo "DB_USER: ${DB_USER:-Mattia}"

# Configura il DataSource
$JBOSS_HOME/bin/jboss-cli.sh --connect --command="data-source add --name=MySqlDS1 --jndi-name=java:/MySqlDS1 --driver-name=mysql --connection-url=jdbc:mysql://${DB_HOST:-mysql_db}:${DB_PORT:-3306}/${DB_NAME:-assdproject}?useSSL=false&allowPublicKeyRetrieval=true --user-name=${DB_USER:-Mattia} --password=${DB_PASSWORD:-mattia} --validate-on-match=true --background-validation=false --valid-connection-checker-class-name=org.jboss.jca.adapters.jdbc.extensions.mysql.MySQLValidConnectionChecker --exception-sorter-class-name=org.jboss.jca.adapters.jdbc.extensions.mysql.MySQLExceptionSorter"

# Abilita il DataSource
$JBOSS_HOME/bin/jboss-cli.sh --connect --command="data-source enable --name=MySqlDS1"

# Deploy dell'applicazione se esiste un WAR
if ls /opt/jboss/container-setup/*.war 1> /dev/null 2>&1; then
    echo "Deploy dell'applicazione..."
    for war_file in /opt/jboss/container-setup/*.war; do
        echo "Deploy di: $war_file"
        $JBOSS_HOME/bin/jboss-cli.sh --connect --command="deploy $war_file"
    done
    echo "Deploy completato!"
else
    echo "Nessun file WAR trovato per il deploy"
fi

echo "=== Configurazione completata ==="
echo "WildFly è pronto e in ascolto su porta 8085"
echo "Management interface disponibile su porta 9990"

# Mantieni WildFly in foreground
wait $WILDFLY_PID