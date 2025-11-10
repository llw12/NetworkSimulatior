#!/bin/bash
set -e

INI_FILE=$1
NED_FILE=$2
RUN_DIR=$3

# Validate parameters
if [ -z "$INI_FILE" ] || [ -z "$NED_FILE" ] || [ -z "$RUN_DIR" ]; then
    echo "[ERROR] Usage: $0 <ini_file> <ned_file> <run_dir>"
    exit 1
fi

# Set OMNeT++ environment
cd ~/omnetpp-6.1/
source setenv

# Set INET environment
cd ~/omnetpp-6.1/WorkSpace/inet4.5
source setenv

# Change to run directory
cd "$RUN_DIR"

echo "[INFO] Using INI: $INI_FILE"
echo "[INFO] Using NED: $NED_FILE"
echo "[INFO] Working directory: $RUN_DIR"

# Save PID for later termination
echo $$ > "$RUN_DIR/pid"

# Start simulation with Cmdenv (command-line interface)
# -n . : NED files in current directory
# -f : configuration file
# -u : user interface (Cmdenv)
# -c : configuration name (General)
inet -n . -f "$INI_FILE" -u Cmdenv -c General

echo "[INFO] Simulation completed"
