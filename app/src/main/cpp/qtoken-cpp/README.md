# QToken-cpp
The C++ implementation of QToken.

## Build+Run
Refer to `docs/setup.md`.

## Docker
- With the latest version of Docker (tested on 19.03.13-ce) installed, run `docker build . -f <docker_file>` from project root.
- After the build is complete, it will display a hash, for example: `Successfully built d41cc18a46a2`
- You can run the container `docker run -dit [HASH_FROM_PREVIOUS_STEP]`
- The above command will generate a container ID that you can find using `docker ps`
- Finally, you can access the container using `docker exec -it [CONTAINER_ID_FROM_PREVIOUS_STEP] /bin/sh`

## Aff3ct
The precompiled library can be downloaded at:
https://aff3ct.github.io/download.html

Alternatively clone and install the Aff3ct library:
- `git clone --recursive https://github.com/aff3ct/aff3ct.git`
- `mkdir aff3ct/build`
- `cd aff3ct/build`
- `cmake .. -DCMAKE_INSTALL_PREFIX="/usr" -DCMAKE_BUILD_TYPE="Install" -DAFF3CT_COMPILE_EXE=OFF -DAFF3CT_COMPILE_STATIC_LIB=ON`
- `sudo make install`

The install defaults to /usr
- /usr/lib should contain the static library and a cmake folder
- /usr/include should contain header files for the project

More Aff3ct documentation can be found here:
https://aff3ct.readthedocs.io/en/latest/user/installation/installation/installation.html#

## Debian Install
VIN can compiled and packaged as a debian to be installed on another debian system. This process is documented in the normal build process. The .deb will come with all the needed libs for normal functionality.

### Installing
- Put the .deb from the previous steps onto your target machine.
- run:
  - `apt install <vin_pacakge>.deb`
- now run `VIN -[options]` as normal.

Virgil Systems® 2021
