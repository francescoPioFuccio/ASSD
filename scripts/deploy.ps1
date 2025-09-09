# Deploy Script per PowerShell
param(
    [Parameter(Mandatory=$false)]
    [ValidateSet("deploy", "status", "cleanup", "logs")]
    [string]$Action = "deploy",

    [Parameter(Mandatory=$false)]
    [string]$Service = "",

    [Parameter(Mandatory=$false)]
    [string]$Tag = "latest"
)

$namespace = "museoapp2"

function Write-Status {
    param([string]$Message, [string]$Color = "White")
    Write-Host $Message -ForegroundColor $Color
}

function Wait-ForDeployment {
    param([string]$DeploymentName)

    Write-Status "Aspettando che $DeploymentName sia pronto..." "Yellow"

    $timeout = 300 # 5 minuti
    $elapsed = 0
    $interval = 10

    while ($elapsed -lt $timeout) {
        $ready = kubectl get deployment $DeploymentName -n $namespace -o jsonpath='{.status.readyReplicas}' 2>$null
        $desired = kubectl get deployment $DeploymentName -n $namespace -o jsonpath='{.spec.replicas}' 2>$null

        if ($ready -eq $desired -and $ready -gt 0) {
            Write-Status "✅ $DeploymentName è pronto!" "Green"
            return $true
        }

        Write-Status "⏳ $DeploymentName non ancora pronto ($ready/$desired)..." "Yellow"
        Start-Sleep $interval
        $elapsed += $interval
    }

    Write-Status "❌ Timeout per $DeploymentName" "Red"
    return $false
}

switch ($Action) {
    "deploy" {
        Write-Status "=== DEPLOY MUSEOAPP2 SU KUBERNETES ===" "Green"

        # Verifica che kubectl sia configurato
        try {
            kubectl version --client --short | Out-Null
        } catch {
            Write-Error "kubectl non è configurato correttamente!"
            exit 1
        }

        Write-Status "Applicando i manifesti Kubernetes..." "Blue"

        # Apply in ordine specifico
        $manifests = @(
            "namespace.yaml",
            "configmap.yaml",
            "secrets.yaml",
            "mysql.yaml",
            "kafka.yaml",
            "userdatamodule2.yaml",
            "gestionemusei.yaml",
            "gestioneopere.yaml",
            "gestionequest.yaml",
            "gateway.yaml"
        )

        foreach ($manifest in $manifests) {
            $manifestPath = "k8s/$manifest"
            if (Test-Path $manifestPath) {
                Write-Status "Applicando $manifest..." "Yellow"
                kubectl apply -f $manifestPath

                if ($LASTEXITCODE -ne 0) {
                    Write-Error "Errore nell'applicare $manifest"
                    exit 1
                }
            } else {
                Write-Status "⚠️  File $manifest non trovato, saltando..." "Yellow"
            }
        }

        Write-Status "`n=== VERIFICA DEPLOY ===" "Blue"

        # Aspetta che i deployment siano pronti
        $deployments = @("mysql", "zookeeper", "kafka", "userdatamodule2", "gestionemusei", "gestioneopere", "gestionequest", "gateway")

        foreach ($deployment in $deployments) {
            if (-not (Wait-ForDeployment $deployment)) {
                Write-Status "❌ Deploy fallito per $deployment" "Red"
                kubectl describe deployment $deployment -n $namespace
                exit 1
            }
        }

        Write-Status "`n🎉 Deploy completato con successo!" "Green"
        Write-Status "`nPer accedere all'applicazione:" "Cyan"
        Write-Status "kubectl port-forward service/gateway-service 8080:8080 -n $namespace" "White"
        Write-Status "Poi vai su: http://localhost:8080" "White"
    }

    "status" {
        Write-Status "=== STATUS MUSEOAPP2 ===" "Green"

        Write-Status "`n--- NAMESPACE ---" "Blue"
        kubectl get namespace $namespace

        Write-Status "`n--- PODS ---" "Blue"
        kubectl get pods -n $namespace -o wide

        Write-Status "`n--- SERVICES ---" "Blue"
        kubectl get services -n $namespace

        Write-Status "`n--- DEPLOYMENTS ---" "Blue"
        kubectl get deployments -n $namespace

        Write-Status "`n--- PERSISTENT VOLUMES ---" "Blue"
        kubectl get pvc -n $namespace

        Write-Status "`n--- INGRESS ---" "Blue"
        kubectl get ingress -n $namespace
    }

    "cleanup" {
        Write-Status "=== CLEANUP MUSEOAPP2 ===" "Red"
        Write-Status "Questa operazione rimuoverà TUTTO il namespace $namespace" "Yellow"

        $confirm = Read-Host "Sei sicuro? (y/N)"
        if ($confirm -eq "y" -or $confirm -eq "Y") {
            Write-Status "Rimuovendo namespace $namespace..." "Red"
            kubectl delete namespace $namespace
            Write-Status "✅ Cleanup completato!" "Green"
        } else {
            Write-Status "Operazione annullata" "Yellow"
        }
    }

    "logs" {
        if (-not $Service) {
            Write-Status "Servizi disponibili per i logs:" "Blue"
            kubectl get pods -n $namespace --no-headers | ForEach-Object { ($_ -split '\s+')[0] }
            $Service = Read-Host "Inserisci il nome del pod"
        }

        Write-Status "=== LOGS per $Service ===" "Green"
        kubectl logs -f $Service -n $namespace
    }
}