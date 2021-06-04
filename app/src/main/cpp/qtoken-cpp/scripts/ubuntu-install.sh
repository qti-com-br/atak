#!/bin/sh

# get required libs
#sudo apt-get -y update && sudo apt-get -y upgrade
#sudo apt-get -y install libboost-all-dev git cmake g++ libssl-dev valgrind unzip wget cppcheck curl libconfig++-dev pkg-config libpoco-dev

sudo cp ./libkademlia.so ./libaff3ct-2.3.5.so /usr/lib
sudo cp -r ./kademlia ./aff3ct-2.3.5 /usr/include

# set up config vars/dirs
./package.sh

# Install VIN
sudo dpkg -i VIN_0.1.0.deb
sudo apt update
sudo apt --fix-broken install

# install poco
sudo cp ./poco/* /usr/local/lib

# Fix perms
sudo chmod 777 /etc/opt/VIN/ /var/log/VIN/logs/ /opt/VIN /opt/VIN/* /opt/VIN/*/*
