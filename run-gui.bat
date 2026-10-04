@echo off
setlocal
echo =========================================================================
echo  Launching Blockchain Network Simulation JavaFX Dashboard...
echo =========================================================================

set MVN_CMD="C:\Program Files\JetBrains\IntelliJ IDEA 2026.1\plugins\maven\lib\maven3\bin\mvn.cmd"
where mvn >nul 2>nul
if %errorlevel% equ 0 (
    set MVN_CMD=mvn
)

%MVN_CMD% javafx:run

if %errorlevel% neq 0 (
    echo [!] Fallback: launching DashboardApp directly via exec:java...
    %MVN_CMD% exec:java "-Dexec.mainClass=sim.dashboard.DashboardApp"
)

pause
