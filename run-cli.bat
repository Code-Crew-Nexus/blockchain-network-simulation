@echo off
setlocal
echo =========================================================================
echo  Launching Blockchain Network Simulation CLI Runner...
echo =========================================================================

set MVN_CMD="C:\Program Files\JetBrains\IntelliJ IDEA 2026.1\plugins\maven\lib\maven3\bin\mvn.cmd"
where mvn >nul 2>nul
if %errorlevel% equ 0 (
    set MVN_CMD=mvn
)

%MVN_CMD% compile exec:java "-Dexec.mainClass=sim.Main" "-Dexec.args=%*"

pause
