Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"

function Invoke-Step {
    param(
        [string]$Name,
        [scriptblock]$Action
    )

    Write-Host "==> $Name"
    & $Action
}

$repoRoot = Split-Path -Parent $PSScriptRoot

Invoke-Step "Backend tests" {
    Push-Location (Join-Path $repoRoot "backend")
    try {
        .\mvnw.cmd test
    } finally {
        Pop-Location
    }
}

Invoke-Step "Frontend typecheck/build/tests" {
    Push-Location (Join-Path $repoRoot "frontend")
    try {
        npm run build
        npx vue-tsc --noEmit
        npm run test:run
    } finally {
        Pop-Location
    }
}

Invoke-Step "Book import tool tests" {
    Push-Location (Join-Path $repoRoot "book-import-tool")
    try {
        python -m pytest
    } finally {
        Pop-Location
    }
}

Invoke-Step "DDoS defense shell syntax" {
    Push-Location (Join-Path $repoRoot "ddos-defense")
    try {
        bash -n scripts/*.sh
    } finally {
        Pop-Location
    }
}
