#include <signal.h>
#include <iostream>

#include "globals/globals.hpp"
#include "globals/logger.hpp"
#include "globals/config.hpp"
#include "nodes/bootstrap.hpp"
#include "nodes/node.hpp"

using namespace Qtoken;

const std::string LIST_FLAG = "-l";
const std::string BOOT_FLAG = "-b";
const std::string CONFIG_DIR_FLAG = "-c";
const std::string CONFIG_DIR_DEFAULT = "/etc/opt/VIN/defaults.cfg";

const std::string BOOTSTRAP_IP_FLAG = "-a";
const std::string BOOTSTRAP_PORT_FLAG = "-s";
const std::string KADEMLIA_PORT_FLAG = "-p";
const std::string RECEIPT_PORT_FLAG = "-r";
const std::string HTTP_PORT_FLAG = "-h";
const std::string LVM_PORT_FLAG = "-v";

struct Params {
    bool boot = false;
    std::string config_dir = CONFIG_DIR_DEFAULT;
    std::string bootstrap_ip;
    std::string bootstrap_port;
    std::string kademlia_port;
    std::string receipt_port;
    std::string http_port;
    std::string lvm_port;
};

void sigshutdown_handler(int signum);
void read_cli_params(Params& p, std::vector<std::string> args);
void read_config_params(Params& p);
void print_flags();

int main(int argc, char* argv[]) {
    std::vector<std::string> args(argv + 1, argv + argc);
    Params p;

    Config::init_config(p.config_dir);

    read_cli_params(p, args);
    read_config_params(p);

    // Register signal handlers
    signal(SIGINT, sigshutdown_handler);
    signal(SIGTERM, sigshutdown_handler);

    if (p.boot) {
        Bootstrap boot(p.bootstrap_ip + ":" + p.bootstrap_port, &std::cin);
        boot.run();
    } else {
        Node node(p.kademlia_port, p.receipt_port, p.http_port,
                  p.bootstrap_ip + ":" + p.bootstrap_port, false);
        //https://gitlab.optimusprime.ai/virgilsystems/prototypes/qtoken-cpp/-/issues/13
        global_node = &node;
        node.run();
    }

    return 0;
}

/**
 * Catch ^C and ^Z and shutdown the https request server
 * in response. 
 * @param signum
 */
void sigshutdown_handler(int signum) {
    Log::message("root", "Received signal. Shutting down node.");
    server_wait_for_shutdown->set();
}

/**
 * Read parameters off the command line.
 * @param p program params
 * @param args command line args
 */
void read_cli_params(Params& p, std::vector<std::string> args) {
    for (int i = 0; i < args.size(); i++) {
        if (args[i] == LIST_FLAG) {
            print_flags();
        } else if (args[i] == BOOT_FLAG) {
            p.boot = true;
        } else if (args[i] == CONFIG_DIR_FLAG) {
            p.config_dir = args[i + 1];
        } else if (args[i] == BOOTSTRAP_IP_FLAG) {
            p.bootstrap_ip = args[i + 1];
        } else if (args[i] == BOOTSTRAP_PORT_FLAG) {
            p.bootstrap_port = args[i + 1];
        } else if (args[i] == KADEMLIA_PORT_FLAG) {
            p.kademlia_port = args[i + 1];
        } else if (args[i] == RECEIPT_PORT_FLAG) {
            p.receipt_port = args[i + 1];
        } else if (args[i] == HTTP_PORT_FLAG) {
            p.http_port = args[i + 1];
        } else if (args[i] == LVM_PORT_FLAG) {
            p.lvm_port = args[i + 1];
        }
    }
}

/**
 * Read program params from config file.
 * Note: Params already set will not be replaced.
 * @param p program params 
 */
void read_config_params(Params& p) {
    if(!p.bootstrap_ip.size()){
        p.bootstrap_ip = Config::get("bootstrap_ip");
    }
    if(!p.bootstrap_port.size()){
        p.bootstrap_port = Config::get("bootstrap_port");
    }
    if(!p.kademlia_port.size()){
        p.kademlia_port = Config::get("kademlia_port");
    }
    if(!p.receipt_port.size()){
        p.receipt_port = Config::get("receipt_port");
    }
    if(!p.http_port.size()){
        p.http_port = Config::get("http_port");
    }
    if(!p.lvm_port.size()){
        p.lvm_port = Config::get("lvm_port");
    }
}

/**
 * Print available flags and usage.
 * Exits program.
 */
void print_flags(){
    std::cout << "Usage:" << std::endl;
    std::cout << "-b launch bootstrap node" << std::endl;
    std::cout << "-c <config-file-path>" << std::endl;
    std::cout << "-a <bootstrap-ip>" << std::endl;
    std::cout << "-p <bootstrap-port>" << std::endl;
    std::cout << "-k <kademlia-port>" << std::endl;
    std::cout << "-r <receipt-server-port>" << std::endl;
    std::cout << "-h <http-server-port>" << std::endl;
    std::cout << "-v <lvm-server-port>" << std::endl;
    exit(0);
}