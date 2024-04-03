#!/bin/bash

# Defining the valid options for each parameter
N_OPTIONS="50 100 150 200"
ALPHA_OPTIONS="0 0.1 0.2 0.3 0.4 0.5 0.6 0.7 0.8 0.9 1.0"
TLE_OPTIONS="5"

# Clear the output file
> data3.csv

# Write the header of the CSV file
echo "n;f;alfa;tle;avg_latency" | tee -a data3.csv

# Perform the experiment for each combination of parameters
for n in $N_OPTIONS; do
  let "f = $n / 2 - 1"
  for alpha in $ALPHA_OPTIONS; do
    for tle in $TLE_OPTIONS; do
      total_latency=0
      # Repeat the experiment 5 times
      for i in {1..5}; do
        output=$(mvn exec:exec -Dexec.executable="java" -Dexec.args="-cp %classpath com.example.synod.Main $n $f $alpha $tle" | grep -m 2 -e 'System started' -e 'decided:' | awk -F":" '{ split($3,a,"."); if (NR == 1) { start = $1 * 3600 * 1000 + $2 * 60 * 1000 + a[1] * 1000 + a[2] } else { end = $1 * 3600 * 1000 + $2 * 60 * 1000 + a[1] * 1000 + a[2] } } END { print end - start }')
        echo "$output"
        total_latency=$(echo "$total_latency + $output" | bc)
      done
      # Calculate the average latency
      avg_latency=$(echo "scale=2; $total_latency / 5" | bc)
      log="$n;$f;$alpha;$tle;$avg_latency"
      echo "$log" | tee -a data3.csv
    done
  done
done