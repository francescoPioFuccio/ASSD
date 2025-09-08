#!/bin/bash
# deploy.sh - Script per il deployment su Kubernetes

set -e

# Colori per output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Funzione per stampare messaggi colorati
print_status() {
    echo -e "${GREEN}[INFO]${NC} $1"
}

print_warning() {
    echo -e "${YELLOW}[WARNING]${NC} $1"
}

print_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

# Verifica prerequisiti
check_prerequisites() {
    print_status "Verifica prerequisiti..."
    
    if ! command -v kubectl &> /dev/null; then
        print_error "kubectl non trovato. Installa kubectl prima di continuare."
        exit 1
    fi
    
    if ! command -v docker &> /dev/null; then
        print_error "docker non trovato. Installa Docker prima di continuare."
        exit 1
    fi
    
    print_status "Prerequisiti verificati con successo."
}

# Configurazione variabili
setup_variables() {
    print_status "Configurazione variabili..."
    
    if [ -z "$GITHUB_USERNAME" ]; then
        read -p "Inserisci il tuo username GitHub: " GITHUB_USERNAME
    fi
    
    if [ -z "$GITHUB_REPO" ]; then
        read -p "Inserisci il nome del repository GitHub: " GITHUB_REPO
    fi
    
    if [ -z "$GITHUB_PAT" ]; then
        read -s -p "Inserisci il tuo Personal Access Token GitHub: " GITHUB_PAT
        echo
    fi
    
    export GITHUB_USERNAME
    export GITHUB_REPO
    export GITHUB_PAT
    
    print_status "Variabili configurate."
}

# Login al registry GitHub
login_registry() {
    print_status "Login al GitHub Container Registry..."
    echo $GITHUB_PAT | docker login ghcr.io -u $GITHUB_USERNAME --password-stdin
    print_status "Login completato."
}

# Build e push delle immagini
build_and_push() {
    print_status "Build e push delle immagini Docker..."
    
    TAG=${1:-latest}
    
    # UserDataModule2
    print_status "Building UserDataModule2..."
    docker build -t ghcr.io/${GITHUB_USERNAME}/${GITHUB_REPO}/userdatamodule2:${TAG} ./UserDataModule2
    docker push ghcr.io/${GITHUB_USERNAME}/${GITHUB_REPO}/userdatamodule2:${TAG}
    
    # Gateway
    print_status "Building Gateway..."
    docker build -t ghcr.io/${GITHUB_USERNAME}/${GITHUB_REPO}/gateway:${TAG} ./Gateway
    docker push ghcr.io/${GITHUB_USERNAME}/${GITHUB_REPO}/gateway:${TAG}
    
    print_status "Immagini pubblicate su GHCR."
}

# Crea secret per pull delle immagini
create_image_pull_secret() {
    print_status "Creazione secret per pull delle immagini..."
    
    kubectl create secret docker-registry ghcr-secret \
        --docker-server=ghcr.io \
        --docker-username=$GITHUB_USERNAME \
        --docker-password=$GITHUB_PAT \
        --docker-email=$GITHUB_USERNAME@users.noreply.github.com \
        -n museoapp --dry-run=client -o yaml | kubectl apply -f -
    
    print_status "Secret creato."
}

# Aggiorna manifesti con i nomi corretti
update_manifests() {
    print_status "Aggiornamento manifesti con le immagini corrette..."
    
    # Crea directory k8s se non esiste
    mkdir -p k8s
    
    # Copia i manifesti e sostituisci i placeholder
    sed "s/YOUR_GITHUB_USERNAME/$GITHUB_USERNAME/g; s/YOUR_REPO_NAME/$GITHUB_REPO/g" kubernetes-manifests.yaml > k8s/manifests.yaml
    
    print_status "Manifesti aggiornati in k8s/manifests.yaml"
}

# Deploy dell'applicazione
deploy_app() {
    print_status "Deploy dell'applicazione su Kubernetes..."
    
    # Applica i manifesti
    kubectl apply -f k8s/manifests.yaml
    
    print_status "Deploy completato. Verifica dello stato dei pod..."
    
    # Attendi che i pod siano pronti
    kubectl wait --for=condition=ready pod -l app=mysql -n museoapp --timeout=300s
    kubectl wait --for=condition=ready pod -l app=zookeeper -n museoapp --timeout=300s
    kubectl wait --for=condition=ready pod -l app=kafka -n museoapp --timeout=300s
    kubectl wait --for=condition=ready pod -l app=userdatamodule2 -n museoapp --timeout=300s
    kubectl wait --for=condition=ready pod -l app=gateway -n museoapp --timeout=300s
    
    print_status "Tutti i pod sono pronti!"
}

# Mostra informazioni sui servizi
show_services() {
    print_status "Informazioni sui servizi:"
    kubectl get services -n museoapp
    
    print_status "Per accedere all'applicazione:"
    
    # Se è un LoadBalancer, mostra l'IP esterno
    EXTERNAL_IP=$(kubectl get service gateway-service -n museoapp -o jsonpath='{.status.loadBalancer.ingress[0].ip}' 2>/dev/null || echo "")
    
    if [ -n "$EXTERNAL_IP" ]; then
        print_status "L'applicazione è accessibile all'indirizzo: http://$EXTERNAL_IP:8085"
    else
        print_status "Usa port-forward per accedere localmente:"
        print_status "kubectl port-forward service/gateway-service 8085:8085 -n museoapp"
        print_status "Poi visita: http://localhost:8085"
    fi
}

# Funzioni di utilità
check_status() {
    print_status "Stato dei pod:"
    kubectl get pods -n museoapp
    
    print_status "Stato dei servizi:"
    kubectl get services -n museoapp
}

cleanup() {
    print_warning "Rimozione di tutte le risorse..."
    kubectl delete namespace museoapp
    print_status "Cleanup completato."
}

logs() {
    local service=$1
    if [ -z "$service" ]; then
        print_error "Specifica il nome del servizio (mysql, kafka, zookeeper, userdatamodule2, gateway)"
        return 1
    fi
    
    kubectl logs -f deployment/$service -n museoapp
}

# Menu principale
case "$1" in
    "deploy")
        check_prerequisites
        setup_variables
        login_registry
        build_and_push $2
        update_manifests
        create_image_pull_secret
        deploy_app
        show_services
        ;;
    "build")
        setup_variables
        login_registry
        build_and_push $2
        ;;
    "status")
        check_status
        ;;
    "cleanup")
        cleanup
        ;;
    "logs")
        logs $2
        ;;
    "port-forward")
        print_status "Avvio port-forward per il gateway..."
        kubectl port-forward service/gateway-service 8085:8085 -n museoapp
        ;;
    *)
        echo "Uso: $0 {deploy|build|status|cleanup|logs|port-forward} [tag|service]"
        echo ""
        echo "Comandi:"
        echo "  deploy [tag]    - Deploy completo dell'applicazione"
        echo "  build [tag]     - Solo build e push delle immagini"
        echo "  status          - Mostra lo stato dei pod e servizi"
        echo "  cleanup         - Rimuove tutte le risorse"
        echo "  logs [service]  - Mostra i log di un servizio"
        echo "  port-forward    - Crea port-forward per accedere al gateway"
        echo ""
        echo "Esempi:"
        echo "  $0 deploy"
        echo "  $0 deploy v1.0.0"
        echo "  $0 logs gateway"
        exit 1
        ;;
esac