#include "bootstrap.hpp"

using namespace Qtoken;

Bootstrap::Bootstrap(const std::string& add, std::istream* inp)
    : Host(add) {}
int Bootstrap::run() {
    firstNode = std::make_shared<kademlia::first_session>(
        kademlia::endpoint{boot_address, boot_port},
        kademlia::endpoint{"::", boot_port});

    auto main_loop_result =
        async(std::launch::async, &kademlia::first_session::run, firstNode);

    log_message("Starting Bootstrap node DHT at port " + boot_port);

    printCommands();

    processInput();

    log_message("Exiting VIN Bootstrap node at " + boot_address + ":" +
                boot_port);

    firstNode->abort();
    main_loop_result.get();

    return EXIT_SUCCESS;
}

void Bootstrap::processInput() {
    std::string line;
    while (true) {
        getline(*input, line);
        std::stringstream command(line);
        if (!handleCommands(command))
            break;
        (*input).clear();
    }
}
int Bootstrap::handleCommands(std::stringstream& command_line) {
    std::string command; /*, key, value, receipt_file_path, file_path;*/
    command_line >> command;

    transform(command.begin(), command.end(), command.begin(), ::tolower);

    try {
        if (command == "") {
            std::cout << "\r";
            return 1;
        }
        log_message("Received command: " + command);

        if (command == "exit") {
            std::cout << "exiting..." << std::endl;
            return 0;
        }
        if (command != "help") {
            std::cout << "unknown command '" << command << "'" << std::endl;
        }
        printCommands();
    } catch (const std::exception& e) {
        std::cerr << e.what() << '\n';
    }
    return 1;
}
void Bootstrap::printCommands() {
    std::cout << "Type \"exit\" to finish" << std::endl;
}
