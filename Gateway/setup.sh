#!/bin/bash

set -e
set -x

WILDFLY_HOME="/opt/jboss/wildfly"
WAR_FILE="/opt/jboss/container-setup/gateway.war"  # CAMBIA QUESTO NOME PER OGNI MODULO
MYSQL_DRIVER="/opt/jboss/container-setup/mysql-connector-j-8.0.32.jar"

echo "=== WILDFLY SETUP DEBUG INFO ==="
echo "WILDFLY_HOME: $WILDFLY_HOME"
echo "WAR_FILE: $WAR_FILE"
echo "DB_HOST: ${DB_HOST:-NOT_SET}"
echo "DB_PORT: ${DB_PORT:-NOT_SET}"
echo "DB_NAME: ${DB_NAME:-NOT_SET}"
echo "DB_USER: ${DB_USER:-NOT_SET}"
echo "KAFKA_BOOTSTRAP_SERVERS: ${KAFKA_BOOTSTRAP_SERVERS:-NOT_SET}"
echo "================================"

# Verifica i file necessari
if [ ! -f "$WAR_FILE" ]; then
    echo "FATAL: WAR file not found: $WAR_FILE"
    ls -la /opt/jboss/container-setup/
    exit 1
fi

if [ ! -f "$MYSQL_DRIVER" ]; then
    echo "FATAL: MySQL driver not found: $MYSQL_DRIVER"
    ls -la /opt/jboss/container-setup/
    exit 1
fi

# Test connessione database
echo "Testing database connection..."
timeout 30 bash -c 'until echo > /dev/tcp/${DB_HOST}/${DB_PORT}; do sleep 1; done' || {
    echo "WARNING: Cannot connect to database ${DB_HOST}:${DB_PORT}"
    echo "Continuing anyway..."
}

# Avvia WildFly in background
echo "Starting WildFly..."
$WILDFLY_HOME/bin/standalone.sh \
    -b 0.0.0.0 \
    -bmanagement 0.0.0.0 \
    -Djboss.http.port=8085 \
    -Djava.security.egd=file:/dev/./urandom &
WILDFLY_PID=$!

echo "Waiting for WildFly to start..."
sleep 30  # Aspetta un tempo fisso per l'avvio

# Configura datasource e deploy
echo "Configuring datasource and deploying application..."

$WILDFLY_HOME/bin/jboss-cli.sh --connect --timeout=60000 --command-timeout=60000 <<EOF
# Aggiungi il modulo MySQL
module add --name=com.mysql.cj --resources=$MYSQL_DRIVER --dependencies=javax.api,javax.transaction.api

# Aggiungi il driver JDBC
/subsystem=datasources/jdbc-driver=mysql:add(driver-name=mysql,driver-module-name=com.mysql.cj,driver-class-name=com.mysql.cj.jdbc.Driver)

# Crea la datasource
/subsystem=datasources/data-source=MySqlDS1:add(connection-url="jdbc:mysql://${DB_HOST}:${DB_PORT}/${DB_NAME}?useSSL=false&allowPublicKeyRetrieval=true",jndi-name="java:/MySqlDS1",user-name="${DB_USER}",password="${DB_PASSWORD}",driver-name=mysql,enabled=true,use-ccm=true,min-pool-size=5,max-pool-size=20)

# Testa la connessione
/subsystem=datasources/data-source=MySqlDS1:test-connection-in-pool()

# Deploy dell'applicazione
deploy $WAR_FILE --force

# Ricarica la configurazione
:reload
EOF

if [ $? -ne 0 ]; then
    echo "FATAL: Configuration failed"
    tail -50 $WILDFLY_HOME/standalone/log/server.log || echo "No server.log"
    kill $WILDFLY_PID 2>/dev/null || true
    exit 1
fi

echo "Configuration completed successfully!"

# Aspetta che WildFly finisca (modalità foreground)
wait $WILDFLY_PID