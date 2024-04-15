import subprocess

from numpy import linspace

# Define the options
""" 
N_OPTIONS = [40, 50, 60, 70, 80, 90, 100, 110, 120]
F_OPTIONS = [19, 24, 29, 34, 39, 44, 49, 54, 59]
ALPHA_OPTIONS = [0.0, 0.1, 1.0]
TLE_OPTIONS = [5]
RUN_TIMES = [1, 2, 3, 4, 5]

# Iterate over each combination of options
for n in N_OPTIONS:
    for f in F_OPTIONS:
        if f < n / 2 + 1:  # Only continue if f is less than n/2 + 1
            for a in ALPHA_OPTIONS:
                for t in TLE_OPTIONS:
                    for r in RUN_TIMES:
                        # Define the command and arguments
                        command = "mvn"
                        args = [
                            "-q",
                            "exec:exec",
                            "-Dexec.executable=java",
                            f"-Dexec.args=-cp %classpath com.example.synod.Main {n} {f} {a} {t}"
                        ]

                        # Run the command
                        subprocess.run([command] + args)
"""
N_OPTIONS = [50]
ALPHA_OPTIONS = [0.5]
TLE_OPTIONS = [i for i in range(5000, 6250, 250)]



for n in N_OPTIONS:
    for alpha in ALPHA_OPTIONS:
        for tle in TLE_OPTIONS:
            for r in range(1, 6):
                # Define the command and arguments
                if n%2 == 0:
                    f = n/2 - 1
                    f = int(f)
                else:
                    f = n/2
                    f = int(f)
                print("Experment with n = ", n, " f = ", f, " alpha = ", alpha, " tle = ", tle, " run = ", r)
                subprocess.run('mvn -q exec:exec -Dexec.executable="java" -Dexec.args="-cp %classpath com.example.synod.Main {} {} {} {}"'.format(str(n),str(f),str(alpha),str(tle)), shell=True)

# Run the command
