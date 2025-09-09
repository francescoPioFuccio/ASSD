# Cleanup Script
param(
    [switch]$Force
)

$namespace = "museoapp2"

Write-Host "=== CLEANUP MUSEOAPP2 ===" -ForegroundColor Red

if (-not $Force) {
    Write-Host "Questa operazione rimuoverà:" -ForegroundColor Yellow
    Write-Host "- Namespace: $namespace" -ForegroundColor Yellow
    Write-Host "- Tutti i pod, servizi, deployment" -ForegroundColor Yellow
    Write-Host "- Tutti i volumi persistenti e i dati" -ForegroundColor Yellow

    $confirm = Read-Host "Sei sicuro di voler continuare? (y/N)"
    if ($confirm -ne "y" -and $confirm -ne "Y") {
        Write-Host "Operazione annullata" -ForegroundColor Yellow
        exit 0
    }
}

Write-Host "Rimuovendo namespace $namespace..." -ForegroundColor Red
kubectl delete namespace $namespace --ignore-not-found=true

Write-Host "✅ Cleanup completato!" -ForegroundColor Green