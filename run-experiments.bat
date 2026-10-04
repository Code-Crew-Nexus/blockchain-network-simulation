@echo off
setlocal
echo =========================================================================
echo  Executing Full Benchmark Academic Experiments...
echo =========================================================================

set MVN_CMD="C:\Program Files\JetBrains\IntelliJ IDEA 2026.1\plugins\maven\lib\maven3\bin\mvn.cmd"
where mvn >nul 2>nul
if %errorlevel% equ 0 (
    set MVN_CMD=mvn
)

%MVN_CMD% compile exec:java "-Dexec.mainClass=sim.Main" "-Dexec.args=--all"

echo.
echo Opening output directory with generated PNG charts and JSON metrics...
explorer output

pause
