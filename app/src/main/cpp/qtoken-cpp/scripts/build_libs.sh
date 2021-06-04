#!/bin/bash
if [ $# -eq 0 ]; then
    echo "Supply a build arch please."
    exit 1
elif [ $1 == "x86_64" ] || [ $1 == "arm64" ] || [ $1 == "armv7l" ]; then
    BUILD_ARCH=$1
    echo "====== BUILDING LIBS FOR $BUILD_ARCH ARCHITECTURE ======"
else
    echo "Please suppply a valid architecture as argument 1. (x86_64, arm64, or armv7l)."
    exit 1 
fi
mkdir -p libs/$BUILD_ARCH
cd libs
mkdir -p logs/$BUILD_ARCH

BASE_DIR=$(pwd)

echo "Building OpenSSL..."
SSL_LIBS_DIR=$BASE_DIR/$BUILD_ARCH/openssl
SSL_LOG_FILE=$BASE_DIR/logs/$BUILD_ARCH/ssl_build_logs.txt
touch $SSL_LOG_FILE
wget https://www.openssl.org/source/openssl-1.1.1i.tar.gz >> $SSL_LOG_FILE
mkdir -p $SSL_LIBS_DIR
tar -zxphf openssl-1.1.1i.tar.gz
cd openssl-1.1.1i
if [ $BUILD_ARCH == "x86_64" ]; then
    ./config --prefix=$SSL_LIBS_DIR --openssldir=$SSL_LIBS_DIR no-threads shared zlib >> $SSL_LOG_FILE
elif [ $BUILD_ARCH == "arm64" ]; then
    ./Configure android-arm64 -D__ANDROID_API__=$ANDROID_API --prefix=$SSL_LIBS_DIR --openssldir=$SSL_LIBS_DIR >> $SSL_LOG_FILE
fi
echo "building..."
make >> $SSL_LOG_FILE
echo "installing..."
make install >> $SSL_LOG_FILE
echo $'OpenSSL Done \n \n'

cd $BASE_DIR

echo "Building Boost .so"
BOOST_LIBS_DIR=$BASE_DIR/$BUILD_ARCH/boost
BOOST_LOG_FILE=$BASE_DIR/logs/$BUILD_ARCH/boost_build_logs.txt
touch $BOOST_LOG_FILE
wget https://boostorg.jfrog.io/artifactory/main/release/1.75.0/source/boost_1_75_0.tar.bz2 >> $BOOST_LOG_FILE
mkdir -p $BOOST_LIBS_DIR
tar -jxphf boost_1_75_0.tar.bz2
cd boost_1_75_0
cp tools/build/example/user-config.jam $HOME
if [ $BUILD_ARCH == "x86_64" ]; then
    echo "building..."
    cp tools/build/example/user-config.jam $HOME
    ./bootstrap.sh --prefix=$BOOST_LIBS_DIR >> $BOOST_LOG_FILE
    echo "installing..."
    ./b2 install --prefix=$BOOST_LIBS_DIR -j4
elif [ $BUILD_ARCH == "arm64" ]; then
    echo "using clang : 9.0.9 : aarch64-linux-android26-clang++ : <architecture>arm ;" >> $HOME/user-config.jam
    echo "building..."
    ./bootstrap.sh --with-toolset=clang --prefix=$BOOST_LIBS_DIR >> $BOOST_LOG_FILE
    echo "installing..."
    ./b2 toolset=clang install --prefix=$BOOST_LIBS_DIR -j4  >> $BOOST_LOG_FILE
fi
echo $'Boost Done \n \n'

cd $BASE_DIR

echo "Building Poco"
POCO_LIBS_DIR=$BASE_DIR/$BUILD_ARCH/poco
POCO_LOG_FILE=$BASE_DIR/logs/$BUILD_ARCH/poco_build_logs.txt
touch $POCO_LOG_FILE
mkdir -p $POCO_LIBS_DIR
git clone https://github.com/pocoproject/poco.git
cd poco
mkdir cmake-build
cd cmake-build
if [ $BUILD_ARCH == "x86_64" ]; then
    cmake -DCMAKE_INSTALL_PREFIX=$POCO_LIBS_DIR -DOPENSSL_ROOT_DIR=$SSL_LIBS_DIR .. >> $POCO_LOG_FILE
elif [ $BUILD_ARCH == "arm64" ]; then
    cmake -G'Ninja' -DCMAKE_BUILD_TYPE=Release -DCMAKE_MAKE_PROGRAM=ninja -DCMAKE_TOOLCHAIN_FILE=$ANDROID_NDK_HOME/build/cmake/android.toolchain.cmake -DANDROID_NATIVE_API_LEVEL=26 -DANDROID_ABI=arm64-v8a -DCMAKE_INSTALL_PREFIX=$POCO_LIBS_DIR  -DOPENSSL_ROOT_DIR=$SSL_LIBS_DIR  .. >> $POCO_LOG_FILE
fi
echo "building..."
cmake --build . -j9 >> $POCO_LOG_FILE
echo "installing..."
cmake --build . --target install  >> $POCO_LOG_FILE
echo $'Poco Done \n \n'

cd $BASE_DIR

echo "Building GFlags"
GFLAGS_LIBS_DIR=$BASE_DIR/$BUILD_ARCH/gflags
GFLAGS_LOG_FILE=$BASE_DIR/logs/$BUILD_ARCH/gflags_build_logs.txt
touch $GFLAGS_LOG_FILE
mkdir -p $GFLAGS_LIBS_DIR
git clone https://github.com/gflags/gflags.git
cd gflags
mkdir build && cd build
if [ $BUILD_ARCH == "x86_64" ]; then
    cmake -DCMAKE_INSTALL_PREFIX=$GFLAGS_LIBS_DIR -DBUILD_SHARED_LIBS=ON -DBUILD_gflags_LIB=OFF -DGFLAGS_BUILD_gflags_nothreads_LIB=ON .. >> $GFLAGS_LOG_FILE
elif [ $BUILD_ARCH == "arm64" ]; then
    cmake -DCMAKE_C_COMPILER=aarch64-linux-android26-clang -DCMAKE_CXX_COMPILER=aarch64-linux-android26-clang++ -DCMAKE_INSTALL_PREFIX=$GFLAGS_LIBS_DIR -DBUILD_SHARED_LIBS=ON -DBUILD_gflags_LIB=OFF .. >> $GFLAGS_LOG_FILE
fi
echo "building..."
cmake --build . -j9 >> $GFLAGS_LOG_FILE
echo "installing..."
cmake --build . --target install >> $GFLAGS_LOG_FILE
echo $'GFlags Done \n \n'

cd $BASE_DIR

# NOTE: Testing seems to be broken for arm64
echo "Building Kademlia"
KADEMLIA_LIBS_DIR=$BASE_DIR/$BUILD_ARCH/kademlia
KADEMLIA_LOG_FILE=$BASE_DIR/logs/$BUILD_ARCH/kademlia_build_logs.txt
touch $KADEMLIA_LOG_FILE
mkdir -p $KADEMLIA_LIBS_DIR
if [ "$2" == "-k" ]; then
    # Copy the kademlia directory at path $2 to here
    cp -r $3 .
else
    git clone git@gitlab.optimusprime.ai:virgilsystems/prototypes/kademlia.git
fi
cd kademlia
mkdir build && cd build
if [ $BUILD_ARCH == "x86_64" ]; then
    cmake -DCMAKE_INSTALL_PREFIX=$KADEMLIA_LIBS_DIR -DOPENSSL_ROOT_DIR=$SSL_LIBS_DIR -DOPENSSL_INCLUDE_DIR=$SSL_LIBS_DIR/include -DBoost_INCLUDE_DIR=$BOOST_LIBS_DIR/include .. >> $KADEMLIA_LOG_FILE 
elif [ $BUILD_ARCH == "arm64" ]; then
    cmake -DCMAKE_C_COMPILER=aarch64-linux-android26-clang -DCMAKE_CXX_COMPILER=aarch64-linux-android26-clang++ -DCMAKE_INSTALL_PREFIX=$KADEMLIA_LIBS_DIR  -DOPENSSL_ROOT_DIR=$SSL_LIBS_DIR -DOPENSSL_INCLUDE_DIR=$SSL_LIBS_DIR/include -DBoost_INCLUDE_DIR=$BOOST_LIBS_DIR/include -DBUILD_TESTING=OFF .. >> $KADEMLIA_LOG_FILE
fi
echo "building..."
cmake --build . -j9 >> $KADEMLIA_LOG_FILE
echo "installing..."
cmake --build . --target install >> $KADEMLIA_LOG_FILE
echo $'Done Kademlia \n \n'

cd $BASE_DIR

echo "Building LibConfig"
LIBCONFIG_LIB_DIR=$BASE_DIR/$BUILD_ARCH/libconfig
LIBCONFIG_LOG_FILE=$BASE_DIR/logs/$BUILD_ARCH/libconfig_build_logs.txt
touch $LIBCONFIG_LOG_FILE
mkdir -p $LIBCONFIG_LIB_DIR
git clone https://github.com/hyperrealm/libconfig.git
cd libconfig
mkdir build && cd build
if [ $BUILD_ARCH == "x86_64" ]; then
    cmake -DCMAKE_INSTALL_PREFIX=$LIBCONFIG_LIB_DIR .. >> $LIBCONFIG_LOG_FILE
elif [ $BUILD_ARCH == "arm64" ]; then
    cmake -DCMAKE_C_COMPILER=aarch64-linux-android26-clang -DCMAKE_CXX_COMPILER=aarch64-linux-android26-clang++ -DCMAKE_INSTALL_PREFIX=$LIBCONFIG_LIB_DIR  ..
fi
echo "building..."
cmake --build . -j9 >> $LIBCONFIG_LOG_FILE
echo "installing..."
cmake --build . --target install >> $LIBCONFIG_LOG_FILE
echo $'Done LibConfig \n \n'

cd $BASE_DIR

# Aff3ct needs some diffs for it work for us.
# Make sure that you have the diffs in your home
# directory for this section
echo "Building Aff3ct"
AFF3CT_LIB_DIR=$BASE_DIR/$BUILD_ARCH/aff3ct
AFF3CT_LOG_FILE=$BASE_DIR/logs/$BUILD_ARCH/aff3ct_build_logs.txt
touch $AFF3CT_LOG_FILE
mkdir -p $AFF3CT_LIB_DIR
git clone https://github.com/aff3ct/aff3ct.git
cd aff3ct
git submodule update --init -- lib/MIPP/
git submodule update --init -- lib/rang/
git submodule update --init -- lib/date/
# Apply backtrace diffs
cp $HOME/scripts/aff3ct_patches/aff3ct_backtrace_patch.patch .
cp $HOME/scripts/aff3ct_patches/MIPP_backtrace_patch.patch lib/MIPP/
git apply aff3ct_backtrace_patch.patch
cd lib/MIPP
git apply MIPP_backtrace_patch.patch
cd ../../
mkdir build && cd build
if [ $BUILD_ARCH == "x86_64" ]; then
    cmake -DCMAKE_INSTALL_PREFIX=$AFF3CT_LIB_DIR -DAFF3CT_BACKTRACE="OFF" -DAFF3CT_COMPILE_STATIC_LIB="ON" -DAFF3CT_COMPILE_SHARED_LIB="ON" .. >> $AFF3CT_LOG_FILE
elif [ $BUILD_ARCH == "arm64" ]; then
    cmake -DCMAKE_C_COMPILER=aarch64-linux-android26-clang -DCMAKE_CXX_COMPILER=aarch64-linux-android26-clang++ -DCMAKE_INSTALL_PREFIX=$AFF3CT_LIB_DIR -DAFF3CT_BACKTRACE="OFF" -DAFF3CT_COMPILE_STATIC_LIB="ON" -DAFF3CT_COMPILE_SHARED_LIB="ON" .. >> $AFF3CT_LOG_FILE
fi
echo "building..."
cmake --build . -j9 >> $AFF3CT_LOG_FILE
echo "installing..."
cmake --build . --target install
echo $'Done Aff3ct \n \n'

cd $BASE_DIR

zip -r ${BUILD_ARCH}_build_libs.zip ${BUILD_ARCH}