import subprocess

from numpy import linspace

# Define the options

N_OPTIONS = [100]
ALPHA_OPTIONS = [0]
TLE_OPTIONS = [100000]


for n in N_OPTIONS:
    for alpha in ALPHA_OPTIONS:
        for tle in TLE_OPTIONS:
            for r in range(1, 10):
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
