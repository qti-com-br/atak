# Qtoken CLI Documentation and Setup

## Build and Install
1. *Ubuntu 20.04*
<br>For building for x86_64 a fresh ubuntu machine will work to start. For arm64 builds you will need some special environment setup detailed in the dev env setup docs [here](https://gitlab.optimusprime.ai/virgilsystems/prototypes/qtoken-cpp/-/wikis/Cross-Compile#android-compiler). Once you have the environment set up for your chose build arch you can start with the next step.
   - Install cmake 3.16-3.18.1, curl and unzip. The command below should work for installing these tools.
   <br>```sudo apt install cmake curl unzip -y```
   - After this is done, get the libs specific to the target architecture you are building for. These can be found in artifactory as bundles.
      - For ```x86_64```
         <br>`curl -L -uanonymous: -O https://opai.jfrog.io/artifactory/VIN_assets/x86_64_build_libs.zip`
      - For ```arm64```
         <br> `curl -L -uanonymous: -O https://opai.jfrog.io/artifactory/VIN_assets/arm64_build_libs.zip`
   - In the project root directory, make a ```libs``` directory and unzip the build libs you downloaded into it. You should now have a ```libs/<build_arch>``` directory with all your external includes and shared libraries in it organized by library.
   - Follow the commands below to build the project.
      - `mkdir build && cd build`
      - For x86_64
         - `cmake .. -DQTOKEN_BUILD_ARCH=x86_64` or `cmake ..` since x86_64 is the default build arch.
      - For arm64
         - `cmake -DANDROID_STL=c++_shared -DCMAKE_TOOLCHAIN_FILE=$ANDROID_NDK_HOME/build/cmake/android.toolchain.cmake -DANDROID_NDK=$ANDROID_NDK_HOME -DCMAKE_BUILD_TYPE=Release -DANDROID_ABI="arm64-v8a" -DANDROID_TOOLCHAIN_NAME=arm-linux-androideabi-4.9 -DANDROID_TOOLCHAIN=clang -DQTOKEN_BUILD_ARCH=arm64 -DBUILD_TESTING=OFF ..`
      - `make` or `cmake --build .` [Note: you can use `make -jX` where X is the number of threads to speed up install]
   - Once this is done `make package` will make a .deb package for x86_64 builds, and nothing yet for arm64

2. *Raspberry Pi OS [2021-01-11]*
   - Navigate to `qtoken-cpp/scripts` and run `sudo ./raspi-install.sh`
   - Follow step ii. from the Ubuntu install

3. *Any other UNIX OS*

As of now we only support Ubuntu and Raspbian. For other UNIX OSs, consult your package manager for the `apt` dependencies. You can build `aff3ct` and `kademlia` from source and move the libs/includes to the `/usr/lib` equivalent.

4. *Windows*

We currently do not support Windows. We've had it work in WSL1/2 but YMMV. You can use Docker, if not a VM.

## Running tests
After following the above steps, you should have a test executable generated in `qtoken-cpp/build/test` called `VIN_test`. You can run it to run the test suite.

## Running the network

To run the VIN, you can spin up a few nodes on your own machine localhost or spin up nodes in different machines.

1. Localhost:

To spin up a basic network on your machine, you can use the following steps:

- Open 3 terminal sessions at the directory where your `VIN` executable lives.
- On one of them, run `./VIN -b`. This will serve as the bootstrap node and will occupy port 8000 for incoming connections.
- On another, run `./VIN`. This will start a VIN peer on the default port for data (8080) and the default receipt server (9090)
- On the final terminal, run `./VIN -p <DATA_PORT> -r <RECEIPT_PORT>` where `<DATA_PORT>` and `<RECEIPT_PORT>` must be different from the other peer.
- To command the nodes follow the build and usage instructions to get the [VIN_cli](https://gitlab.optimusprime.ai/elewis/vin_cli.git).


2. IP Network:

To spin up a basic VIN on an IP based network (such as AWS, LAN with routers/switches and DHCP, VMware), you can use the following steps:

- Follow the install steps for all devices in the network (n.b: mass deployment is an upcoming feature).
- On any node, from the `VIN` executable directory run `./VIN -b` to start a bootstrap node. Note down the IP address of this device.
- In all subsequent nodes, use a text editor such as `vim` or `nano` to edit the Qtoken config file `/etc/opt/VIN/defaults.cfg`. Replace the `default_bootstrap_address` variable to look like this:
   `default_bootstrap_address = "<BOOTSTRAP_NODE_IP_ADDRESS>:8000";`
    Make sure to leave the quotes (`"`) intact.
- Following this, run `./VIN` on all non-bootstrap nodes.
- To command the nodes follow the build and usage instructions to get the [VIN_cli](https://gitlab.optimusprime.ai/elewis/vin_cli.git).

NOTE: Nodes can be run the background if it is needed with the command `./VIN & > /dev/null` or `/VIN -p <DATA_PORT> -r <RECEIPT_PORT> & > /dev/null`. These nodes can then be shutdown from the [VIN_cli](https://gitlab.optimusprime.ai/elewis/vin_cli.git).
<br>
Please ensure that there are no other processes that are using all ports mentioned above! At this point you have successfully launched a Virgil Integrity Network. Each peer should display a CLI with all available commands.

## Building Build Library Bundles
There are build library bundles used when building for each supported architectures in artifactory in the repo `VIN_assets/build_libs_<target_arch>.zip`. If you need build libraries this is the recommended source for them. However, if it is necessary to rebuild these libs from scratch, then scripts and images are available. The process for which is as follows.
   - Create a docker container for your chosen build arch with...
   <br> `docker build . -f docker/Dockerfile.build_libs_<target_arch> --build-arg SSH_PRIVATE_KEY="$(cat <valid_opai_gitlab_ssh_pkey_file>)"`
   - Once the image is built, create a container from that image with
   <br>`docker run -dit <image_hash> bash`
   - Now copy the scripts folder into the container with...
   <br> `docker cp scripts/ <container_id>:/root/`
   - Enter the docker container with...
   <br> `docker exec -it <container_id> bash`
   <br>Then enter the scripts and run the build_libs.sh script as such...
   <br> `cd /root/scripts/`
   <br> `./build_libs.sh <target_arch> -k /root/kademlia/`
   - This will create a build library bundle at the end name `build_libs_<target_arch>.zip`. These can be

## Disk
All Qtoken Disk IO happens in `/opt/VIN`. Any receipts/reconstructed data will be stored there.

## Command Reference
N.B. Question marks denote optional arguments

1. Put
   - `put <key> <value>`
   - Puts a value with the given key.
2. Get
   - `get <key>`
   - Takes a given key and returns the Key-Value pair from any node. The value returned is saved in `/opt/VIN/outputs`. No other output is displayed.

3. Spread
   - `spread <filepath>`
   - Splits a file of any type located in a given path into Qtokens and then spreads them across the network. An encrypted receipt is then generated and stored in `/opt/VIN/receipts/sent`.
4. Share
   -  `share <filepath> <ip address> <port of another node>`
   -  Spreads a file, then the peer will automatically establish a secure channel with IP Address and receipt port of specified peer in the network and transfer the encrypted receipt over. The receiver peer will automatically call `gather` on the receipt once decrypted.
5. Gather
   - `gather <receipt filepath>`
   - Gathers a spread file using the given receipt. It will be reassembled into the output directory `/opt/VIN/outputs`.
