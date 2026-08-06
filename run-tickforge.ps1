$ErrorActionPreference = "Stop"

& .\gradlew.bat :client:shadowJar

if ($LASTEXITCODE -ne 0)
{
    throw "Tickforge build failed."
}

$jar = Get-ChildItem .\runelite-client\build\libs\*-shaded.jar |
    Sort-Object LastWriteTime -Descending |
    Select-Object -First 1

if ($null -eq $jar)
{
    throw "Could not find the shaded client jar."
}

& java -ea -jar $jar.FullName `
    --developer-mode `
    --debug `
    --disable-telemetry