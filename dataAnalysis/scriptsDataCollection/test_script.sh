#!/bin/bash

# Defining the valid options for each parameter
N_OPTIONS=("3" "10" "100")
F_OPTIONS=("1" "4" "49")
ALPHA_OPTIONS="0 0.1 1"
TLE_OPTIONS="500 1000 1500 2000"

# Clear the output file
> output.txt

# Perform the experiment for each combination of parameters
for index in ${!N_OPTIONS[@]}; do
  n=${N_OPTIONS[$index]}
  f=${F_OPTIONS[$index]}
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
      log="N: $n F: $f alpha: $alpha tle: $tle Average Time (ms): $avg_latency"
      echo "$log" | tee -a output.txt
    done
  done
done