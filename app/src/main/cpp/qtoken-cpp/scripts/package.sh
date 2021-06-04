#!/bin/bash

BUILD_ARCH="x86_64"

while getopts ":arch:" opt; do
    case $opt in
      arch)
        BUILD_ARCH=$2
        ;;
    esac
done
        

# TODO: Versioning support
VER='0.1.0'
PACKAGE_NAME=VIN_$VER

# Destroy any existing package info
rm -rf $PACKAGE_NAME

# Build package structure
mkdir $PACKAGE_NAME
mkdir $PACKAGE_NAME/DEBIAN
mkdir -p $PACKAGE_NAME/usr/bin/
cp control $PACKAGE_NAME/DEBIAN/control
# cp postinst $PACKAGE_NAME/DEBIAN/
if [[ -e ../build/apps/VIN ]]; then
    cp ../build/apps/VIN $PACKAGE_NAME/usr/bin/
elif [[ -e apps/VIN ]]; then
    cp apps/VIN $PACKAGE_NAME/usr/bin/
else
    echo "Default VIN exe not found!"
    exit
fi

# Install Kademlia
mkdir -p $PACKAGE_NAME/usr/lib/
mkdir -p $PACKAGE_NAME/usr/include/kademlia
cp -r ../libs/$BUILD_ARCH/kademlia/lib/kademlia/libkademlia* $PACKAGE_NAME/usr/lib/
cp -r ../libs/$BUILD_ARCH/kademlia/include/kademlia/* $PACKAGE_NAME/usr/include/kademlia

# Install aff3ct
mkdir $PACKAGE_NAME/usr/include/aff3ct-2.3.5
cp -r ../libs/$BUILD_ARCH/aff3ct/include/aff3ct-2.3.5/* $PACKAGE_NAME/usr/include/aff3ct-2.3.5/
cp -r ../libs/$BUILD_ARCH/aff3ct/lib/libaff3ct-2.3.5.so $PACKAGE_NAME/usr/lib/


# Handle config files
mkdir -p $PACKAGE_NAME/opt/VIN/
mkdir -p $PACKAGE_NAME/opt/VIN/keys/
mkdir -p $PACKAGE_NAME/opt/VIN/outputs/
mkdir -p $PACKAGE_NAME/opt/VIN/receipts/received/
mkdir -p $PACKAGE_NAME/opt/VIN/receipts/sent/

mkdir -p $PACKAGE_NAME/etc/opt/VIN/

if [[ -e ../defaults.cfg ]]; then
    cp ../defaults.cfg $PACKAGE_NAME/etc/opt/VIN/
elif [[ -e ./defaults.cfg ]]; then
    cp defaults.cfg $PACKAGE_NAME/etc/opt/VIN/
else
    echo "Default config not found!"
fi

mkdir -p $PACKAGE_NAME/var/log/VIN/logs/

chmod 0755 preinst
chmod 0755 postinst

dpkg-deb --build $PACKAGE_NAME
