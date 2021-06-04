#!/bin/bash

# Get Kademlia directory if specified
while getopts ":k:" flag
do
    case "${flag}" in
        k) $KAD_SOURCE_PATH==${OPTARG};;
    esac
done

mkdir -p libs_x86/x86_64
cd libs_x86

BASE_DIR=$(pwd)

echo "Building Kademlia"
KADEMLIA_LIBS_DIR=$BASE_DIR/x86_64/kademlia
mkdir -p $KADEMLIA_LIBS_DIR
if [ "$1" != "-k" ]; then
    cp -r $KAD_SOURCE_PATH .
else
    git clone git@gitlab.optimusprime.ai:virgilsystems/prototypes/kademlia.git

fi
cd kademlia
mkdir build && cd build
cmake -DCMAKE_INSTALL_PREFIX=$KADEMLIA_LIBS_DIR -DOPENSSL_ROOT_DIR=$SSL_LIBS_DIR -DOPENSSL_INCLUDE_DIR=$SSL_LIBS_DIR/include -DBoost_INCLUDE_DIR=$BOOST_LIBS_DIR/include ..
cmake --build . -j9
cp -p src/kademlia/libkademlia* $KADEMLIA_LIBS_DIR/lib/
cp -a ../include/kademlia $KADEMLIA_LIBS_DIR/include/
echo "Done Kademlia"