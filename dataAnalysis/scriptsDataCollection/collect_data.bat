@echo off
setlocal enabledelayedexpansion

set N_OPTIONS= 40 50 60 70 80 90 100 110 120 
set F_OPTIONS= 19 24 29 34 39 44 49 54 59 
set ALPHA_OPTIONS=0.0 0.1 1.0
set TLE_OPTIONS=5
set RUN_TIMES=1 2 3 4 5

for %%n in (%N_OPTIONS%) do (
    set /a half_n=%%n/2+1
    for %%f in (%F_OPTIONS%) do (
        if %%f lss !half_n! (
            for %%a in (%ALPHA_OPTIONS%) do (
                for %%t in (%TLE_OPTIONS%) do (
                    for %%r in (%RUN_TIMES%) do (
                        echo Running with N=%%n, F=%%f, ALPHA=%%a, TLE=%%t, RUN=%%r
                        mvn -q exec:exec -Dexec.executable="java" -Dexec.args="-cp %%classpath com.example.synod.Main %%n %%f %%a %%t"
                    )
                )
            )
        )
    )
)