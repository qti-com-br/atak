## ATAK
This is how to setup ATAK for VINTAK. (_You just need to do this once._)
<br />
<br />VINTAK is a git submodule of this repository.
<br />
<br />To get the lastest VINTAK version, run the command below (**After follow this tutorial**):
<br />git submodule update --init --recursive
<br /><br />

```
Before anything else: make sure android studio is installed, and make sure NDK (side by side) and cmake are added under SDK Tools.
```

**Ubuntu Dependencies:**
```
sudo apt install build-essential git openjdk-8-jdk dos2unix autoconf automake libtool patch make tcl8.6 cmake swig ant
```


**JDK:**

**NOTE: You must use the JDK 1.8. If your environment uses another JDK the build will fail.**

```
sudo apt-get install openjdk-8-jdk

After the installation verify if the folder below exist:
/usr/lib/jvm/java-8-openjdk-amd64

If not, check this tutorial: https://docs.datastax.com/en/jdk-install/doc/jdk-install/installOpenJdkDeb.html
```


**NDK:** (r12b = android-ndk-r12b)
```
cd ~/Android/Sdk/ndk
wget https://dl.google.com/android/repository/android-ndk-r12b-linux-x86_64.zip
unzip android-ndk-r12b-linux-x86_64.zip
mv "android-ndk-r12b" r12b
```


**Environment variables:**
```
sudo nano /etc/environment
JAVA_HOME=/usr/lib/jvm/java-8-openjdk-amd64
ANDROID_NDK=~/Android/Sdk/ndk/r12b
source /etc/environment
export JAVA_HOME=/usr/lib/jvm/java-8-openjdk-amd64
export ANDROID_NDK=~/Android/Sdk/ndk/r12b
```


**ATAK**
```
git clone https://gitlab.optimusprime.ai/virgilsystems/prototypes/atak.git
```


**Run on atak project folder:**
```
mkdir takengine/thirdparty
cd takengine/thirdparty
git rm -r stlsoft/
git submodule add https://github.com/synesissoftware/STLSoft-1.9.git stlsoft

```


**VINTAK**
```
cd atak
git rm -r ATAK/
```
** HTTP **
```
git submodule add https://gitlab.optimusprime.ai/virgilsystems/prototypes/vintak.git ATAK
```
** SSL **
```
git submodule add git@gitlab.optimusprime.ai:virgilsystems/prototypes/vintak.git ATAK
```


**Dependencies:**
```
cd takthirdparty
make TARGET=android-arm64-v8a build_spatialite build_gdal build_commoncommo build_assimp

_This will take a long time... coffee time =)_
```

<br /><br />
Ok, now let's back to the tutorial:
<br /><br />
Open the ATAK project on Android Studio.
<br /><br />

**Edit the local.properties file:**
```
sdk.dir=/home/carlos/Android/Sdk
android.ndkVersion=r12b

cmake.dir=/home/carlos/Android/Sdk/cmake/3.10.2.4988404

takReleaseKeyFile=keystore/debug.keystore
takReleaseKeyPassword=android
takDebugKeyFilePassword=android
takDebugKeyPassword=android
takReleaseKeyAlias=androiddebugkey
takReleaseKeyFilePassword=android
takDebugKeyFile=keystore/debug.keystore
takDebugKeyAlias=androiddebugkey
```

<br /><br />
Now you should be able to Sync your project with gradle.
<br /><br />
If you are having issues, check the **known errors** below. Or if you have a different issue, please let me know.
<br /><br />

**## Troubleshooting**

<br /><br />
**Error:**
```
cp: cannot stat '../assimp': No such file or directory**
```
or
```
tinygltf/tiny_gltf.h: No such file or directory**
```
**Solution:**
```
Unzip and copy files from depends folder to takengine/thirdparty (parentFolder -> childFolder):

assimp-4.0.1-mod.tar.gz -> assimp

gdal-2.4.4-mod.tar.gz -> gdal

tinygltf-2.4.1-mod.tar.gz -> tinygltf

tinygltfloader-0.9.5-mod.tar.gz -> tinygltfloader

Copy the assimp folder from other build (get it from different locations) and paste on:
~/atakand~/atak/takthirdparty/builds/android-arm64-v8a-release

Delete this file (if exist):
/home/<YOUR_USER>/atak/takthirdparty/builds/android-arm64-v8a-release/assimp/ttpbuild/CMakeCache.txt
```

<br /><br />
**Error:**
```
package com.ekito.simpleKML.model does not exist**
```
**Solution:**
```
git clone https://github.com/Ekito/Simple-KML.git (to outside of the atak project)

copy libs/simple-xml-2.6.3.jar to atat/ATAK/app/libscopy src/com/ekito folder to atak/ATAK/app/src/main/java/com
```

<br /><br />
**Error:**
```
FAILURE: Build failed with an exception.**

Execution failed for task ':ATAK:app:stripCivDebugDebugSymbols'.
> No version of NDK matched the requested version 21.0.6113669. Versions available locally: 12.1.2977051, 21.1.6352462, 21.4.7075529
```
**Solution:**
```
Go to SDK Manager and install the requested NDK (21.0.6113669)
```
<br /><br />

######################################################################

# AndroidTacticalAssaultKit-CIV

This repository contains the source code for the Android Tactical Assault Kit for Civilian Use (ATAK-CIV), the official geospatial-temporal and situational awareness tool used by the US Government. ATAK-CIV is managed by the Tactical Assault Kit Configuration Steering Board (TAK CSB) and is designed for used by federal employees. 

This website's source code is made available to the open source community with the hope that community contributions will improve functionality, add features, and mature this work.

## Dependencies
ATAK-CIV makes use of many open source libraries as dependencies. While each of these has a license that allows for use within ATAK-CIV, be sure to check that their license authorizes your particular use. A list of all dependencies and the license that binary/source is distributed under can be found in [THIRDPARTY.md](THIRDPARTY.md).

## Contributing
For details on setting up your development environment and contributing to this project, see CONTRIBUTING.md.

## License
As part of the TAK CSB's goal of bringing technology industry practices to the U.S. Department of Defense, we welcome contributions to this repository from the open source community. If you are interested in contributing to this project, please review CONTRIBUTING.md and LICENSE.md. Those files describe how to contribute to this work. A list of contributors to this project is maintained in CONTRIBUTORS.md.

Works created by U.S. Federal employees as part of their jobs typically are not eligible for copyright in the United States. In places where the contributions of U.S. Federal employees are not eligible for copyright, this work is in the public domain. In places where it is eligible for copyright, such as some foreign jurisdictions, this work is licensed as described in LICENSE.md.



