<# :
@REM ----------------------------------------------------------------------------
@REM Licensed to the Apache Software Foundation (ASF) under one
@REM or more contributor license agreements.  See the NOTICE file
@REM distributed with this work for additional information
@REM regarding copyright ownership.  The ASF licenses this file
@REM to you under the Apache License, Version 2.0 (the
@REM "License"); you may not use this file except in compliance
@REM with the License.  You may obtain a copy of the License at
@REM
@REM    http://www.apache.org/licenses/LICENSE-2.0
@REM
@REM Unless required by applicable law or agreed to in writing,
@REM software distributed under the License is distributed on an
@REM "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
@REM KIND, either express or implied.  See the License for the
@REM specific language governing permissions and limitations
@REM under the License.
@REM ----------------------------------------------------------------------------
@REM ----------------------------------------------------------------------------
@REM Apache Maven Wrapper startup batch script, version 3.2.0
@REM ----------------------------------------------------------------------------
@REM Begin PowerShell script region
#>

$ErrorActionPreference = "Stop"
$scriptDir = Split-Path -Path $MyInvocation.MyCommand.Definition -Parent

$distributionUrl = ""
$wrapperPropertiesPath = Join-Path $scriptDir ".mvn\wrapper\maven-wrapper.properties"
if (Test-Path $wrapperPropertiesPath) {
    $content = Get-Content $wrapperPropertiesPath
    foreach ($line in $content) {
        if ($line -match 'distributionUrl=(.*)') {
            $distributionUrl = $Matches[1]
            break
        }
    }
}

if (-not $distributionUrl) {
    Write-Error "Cannot find distributionUrl in $wrapperPropertiesPath"
    exit 1
}

$mavenHome = Join-Path $env:USERPROFILE ".m2\wrapper\dists\apache-maven-3.9.6"
$mavenBin = Join-Path $mavenHome "bin\mvn.cmd"

if (-not (Test-Path $mavenBin)) {
    Write-Host "Downloading Maven..."
    New-Item -ItemType Directory -Path $mavenHome -Force | Out-Null
    $zipPath = Join-Path $env:TEMP "apache-maven-3.9.6-bin.zip"
    Invoke-WebRequest -Uri $distributionUrl -OutFile $zipPath
    Expand-Archive -Path $zipPath -DestinationPath (Join-Path $env:USERPROFILE ".m2\wrapper\dists") -Force
    Remove-Item $zipPath
}

& $mavenBin @args
