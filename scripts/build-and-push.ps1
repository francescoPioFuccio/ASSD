# Build and Push Script per PowerShell
param(
    [Parameter(Mandatory=$false)]
    [string]$Tag = "latest"
)

# Carica variabili d'ambiente dal file .env
if (Test-Path ".env") {
    Get-Content ".env" | ForEach-Object {
        if ($_ -match "^([^#=]+)=(.*)$") {
            [Environment]::SetEnvironmentVariable($matches[1], $matches[2], "Process")
        }
    }
} else {
    Write-Error "File .env non trovato!"
    exit 1
}

$GITHUB_USERNAME = $env:GITHUB_USERNAME
$GITHUB_REPO = $env:GITHUB_REPO
$GITHUB_PAT = $env:GITHUB_PAT

if (-not $GITHUB_USERNAME -or -not $GITHUB_REPO -or -not $GITHUB_PAT) {
    Write-Error "Variabili GitHub non configurate correttamente nel file .env"
    exit 1
}

Write-Host "=== BUILD E PUSH IMMAGINI DOCKER ===" -ForegroundColor Green
Write-Host "Repository: $GITHUB_USERNAME/$GITHUB_REPO" -ForegroundColor Yellow
Write-Host "Tag: $Tag" -ForegroundColor Yellow

# Login al registry GitHub
Write-Host "`nLogin al GitHub Container Registry..." -ForegroundColor Blue
echo $GITHUB_PAT | docker login ghcr.io -u $GITHUB_USERNAME --password-stdin

if ($LASTEXITCODE -ne 0) {
    Write-Error "Login fallito!"
    exit 1
}

# Lista dei moduli da buildare
$modules = @(
    @{ name = "userdatamodule2"; path = "UserDataModule2" },
    @{ name = "gestionemusei"; path = "GestioneMusei" },
    @{ name = "gestioneopere"; path = "GestioneOpere" },
    @{ name = "gestionequest"; path = "GestioneQuest" },
    @{ name = "gateway"; path = "Gateway" }
)

foreach ($module in $modules) {
    $moduleName = $module.name
    $modulePath = $module.path
    $imageName = "ghcr.io/$($GITHUB_USERNAME.ToLower())/$($GITHUB_REPO.ToLower())/$($moduleName):$Tag"

    Write-Host "`n=== BUILDING $moduleName ===" -ForegroundColor Cyan

    # Verifica che la directory esista
    if (-not (Test-Path $modulePath)) {
        Write-Error "Directory $modulePath non trovata!"
        continue
    }

    # Verifica che il Dockerfile esista
    if (-not (Test-Path "$modulePath/Dockerfile")) {
        Write-Error "Dockerfile non trovato in $modulePath!"
        continue
    }

    Write-Host "Building $imageName..." -ForegroundColor Yellow
    docker build -t $imageName $modulePath

    if ($LASTEXITCODE -ne 0) {
        Write-Error "Build fallita per $moduleName"
        exit 1
    }

    Write-Host "Pushing $imageName..." -ForegroundColor Yellow
    docker push $imageName

    if ($LASTEXITCODE -ne 0) {
        Write-Error "Push fallito per $moduleName"
        exit 1
    }

    Write-Host "✅ $moduleName completato!" -ForegroundColor Green
}

Write-Host "`n🎉 Tutte le immagini sono state create e pubblicate con successo!" -ForegroundColor Green