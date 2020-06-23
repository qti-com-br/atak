#!/bin/sh

(cd .. && \
        gzip -d ./depends/gdal-2.2.3-mod.tar.gz && \
        cp depends/gdal-2.2.3-mod.tar . && \
        tar xf gdal-2.2.3-mod.tar)

(cd ../takthirdparty && make TARGET=android-armeabi-v7a \
	build_spatialite \
	build_commoncommo \
	build_gdal \
	build_assimp) &
(cd ../takthirdparty && make TARGET=android-arm64-v8a \
	build_spatialite \
	build_commoncommo \
	build_gdal \
	build_assimp) &
(cd ../takthirdparty && make TARGET=android-x86 \
	build_spatialite \
	build_commoncommo \
	build_gdal \
	build_assimp) &
wait

(cd ../takengine/thirdparty && git clone https://github.com/synesissoftware/STLSoft-1.9.git stlsoft)
