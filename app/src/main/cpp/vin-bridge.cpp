#include <string.h>
#include <jni.h>
#include <dlfcn.h>
#include <string>
#include <android/log.h>
#include <iostream>
#include <fstream>
#include <vector>

#include "Poco/AutoPtr.h"
#include "Poco/FormattingChannel.h"
#include "Poco/Logger.h"
#include "Poco/PatternFormatter.h"
#include "Poco/SimpleFileChannel.h"

#include "Poco/Foundation.h"
#include "Poco/Channel.h"

#include "globals/globals.hpp"
#include "globals/logger.hpp"
#include "nodes/node.hpp"
#include "receipt/crypto_receipt.hpp"

using Poco::AutoPtr;
using Poco::FormattingChannel;
using Poco::Logger;
using Poco::PatternFormatter;
using Poco::SimpleFileChannel;
using Poco::Channel;
using Poco::Formatter;

using namespace Qtoken;
using namespace std;

std::string booststrap_ip_str;
std::string node_port_str;
std::string receipt_port_str;
std::string http_port_str;
std::string root_folder_str;

Node *node;

extern "C" {

    /**
     * This function creates the Node on Android
     * @param env Environment where the SDK is running
     * @param clazz Class that call this function
     * @param booststrap_ip The IP of the Bootstrap running at the network
     * @param root_folder Root folder where the VIN structure is
     */
    JNIEXPORT int JNICALL
    Java_com_virgilsystems_qtoken_QToken_run(JNIEnv *env, jclass clazz,
                                             jstring booststrap_ip,
                                             jstring node_port,
                                             jstring receipt_port,
                                             jstring root_folder) {

        const char *booststrap_ip_chars = env->GetStringUTFChars(booststrap_ip, NULL);
        booststrap_ip_str = booststrap_ip_chars;

        const char *node_port_chars = env->GetStringUTFChars(node_port, NULL);
        node_port_str = node_port_chars;

        const char *receipt_port_chars = env->GetStringUTFChars(receipt_port, NULL);
        receipt_port_str = receipt_port_chars;

        const char *root_folder_chars = env->GetStringUTFChars(root_folder, NULL);
        root_folder_str = root_folder_chars;

        // http port is unused in android
        http_port_str = "0";

        string addr = booststrap_ip_str + ":8000";

        try {

            log_message(addr);

            string cfg_file = root_folder_str + "/VIN/defaults.cfg";

            struct stat st = {0};
            log_message("###QTOKEN | readFile");
            Config::init_config(cfg_file);

            log_message("###QTOKEN | new Node");
            node = new Node(node_port_str, receipt_port_str, http_port_str, addr, true);

            log_message("###QTOKEN | run");
            return node->run(); // 0 = EXIT_SUCCESS , <>0 = EXIT_FAILURE

        } catch (std::exception e) {
            log_message(e.what());
        }
    }

    /**
     * This function send messages to the network (Hardcoded "Hello World")
     * After call this function do "get Hello" at another Node.
     * @param env Environment where the SDK is running
     * @param clazz Class that call this function
     */
    JNIEXPORT void JNICALL
    Java_com_virgilsystems_qtoken_QToken_put(JNIEnv *env, jclass clazz,
                                             jstring key, jstring message) {
        try {
            const char *key_chars = env->GetStringUTFChars(key, NULL);
            const char *message_chars = env->GetStringUTFChars(message, NULL);

            std::string k = key_chars;
            std::string v = message_chars;

            std::vector<unsigned char> vec(v.begin(), v.end());
            log_message("###QTOKEN | doPut");
            node->doPut(k, vec);

        } catch (...) {
            log_message("###QTOKEN 6 | ...");
        }

        log_message("###QTOKEN 7 | put End");
    }

    /**
     * This function get a message from the network that was sent by the put function
     * @param env Environment where the SDK is running
     * @param clazz Class that call this function
     */
    JNIEXPORT jstring JNICALL
    Java_com_virgilsystems_qtoken_QToken_get(JNIEnv *env, jclass clazz, jstring key) {
        try {
            const char *key_chars = env->GetStringUTFChars(key, NULL);
            std::string key_str = key_chars;
            log_message("###QTOKEN | get before");
            vector<unsigned char> msg_vector = node->doGet(key_str);
            log_message("###QTOKEN | get after");

//            char* msg_char_array = &msg_vector[0];
            int size = msg_vector.size();

            jbyteArray array = env->NewByteArray(size);
            env->SetByteArrayRegion(array, 0, size, (const jbyte*)msg_vector.data());
            jstring strEncode = env->NewStringUTF("UTF-8");
            jclass cls = env->FindClass("java/lang/String");
            jmethodID ctor = env->GetMethodID(cls, "<init>", "([BLjava/lang/String;)V");
            jstring object = (jstring) env->NewObject(cls, ctor, array, strEncode);
            return object;

        } catch (...) {
            log_message("###QTOKEN 6 | ...");
        }
    }

    /**
     * This function share a selected file to some Node
     * running at the same IP that the bootstrap and the receive port 9091
     * @param env Environment where the SDK is running
     * @param clazz Class that call this function
     * @param file_path Path with the filename to be sent
     */
    JNIEXPORT void JNICALL // share
    Java_com_virgilsystems_qtoken_QToken_share(JNIEnv *env, jclass clazz, jstring file_path) {

        const char *file_path_chars = env->GetStringUTFChars(file_path, NULL);
        std::string file_path_str;
        file_path_str.append(file_path_chars);

        node->doShare(file_path_str, "10.88.111.5", "9090");
    }

    /**
     * This function send a selected file to the network
     * The file will be saved at/as "/VIN/receipts/sent/test.txt"
     * @param Environment where the SDK is running
     * @param clazz Class that call this function
     * @param file_path Path with the filename to be sent
     */
    JNIEXPORT void JNICALL // calls put
    Java_com_virgilsystems_qtoken_QToken_spread(JNIEnv *env, jclass clazz, jstring file_path) {

        const char *file_path_chars = env->GetStringUTFChars(file_path, NULL);
        std::string file_path_str;
        file_path_str.append(file_path_chars);

        auto receipt = node->doSpread(file_path_str);

        receipt.save(root_folder_str + "/VIN/receipts/sent/test.txt");
    }

    /**
     * This function will gather a file that was spreaded at the network
     * The function will look at "/VIN/receipts/sent/test.txt"
     * And saved at "/VIN/outputs/test.txt"
     * @param Environment where the SDK is running
     * @param clazz Class that call this function
     */
    JNIEXPORT void JNICALL // calls get
    Java_com_virgilsystems_qtoken_QToken_gather(JNIEnv *env, jclass clazz) {
        string test_file = root_folder_str + "/VIN/receipts/sent/test.txt";

        auto ch = node->doGather(test_file);

        ch.rebuild(root_folder_str + "/VIN/outputs/test.txt");
    }

}
