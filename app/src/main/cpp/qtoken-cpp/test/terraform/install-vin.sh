#!/bin/bash
# All commands run as root
apt update
apt install -y unzip awscli libboost-all-dev libssl-dev libpoco-dev
touch /tmp/ready_check.txt
