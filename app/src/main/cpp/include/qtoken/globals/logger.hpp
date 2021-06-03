#ifndef LOGGER_H
#define LOGGER_H

#include <fstream>
#include <iostream>
#include <unordered_map>

#include <Poco/AsyncChannel.h>
#include <Poco/AutoPtr.h>
#include <Poco/ConsoleChannel.h>
#include <Poco/FormattingChannel.h>
#include <Poco/Logger.h>
#include <Poco/PatternFormatter.h>
#include <Poco/SimpleFileChannel.h>
#include <Poco/SplitterChannel.h>

#ifdef __ANDROID__
#include <android/log.h>
#endif

#include "globals/config.hpp"
#include "globals/globals.hpp"

namespace Qtoken {

class Log {
public:
    Log() = delete;

#ifndef __ANDROID__
    /**
     * Initializes log_name if not already present.
     * Sends message to logger.
     * @param msg text to be logged
     * @param log_name logger to log msg to
     */
    static void message(std::string log_name, std::string msg) {
        if (logs.find(log_name) == logs.end()) {
            init_log(log_name);
        }

        Poco::Message m;
        m.setText(msg);
        m.setSource(log_name);
        m.setPriority(Poco::Message::Priority::PRIO_INFORMATION);

        logs[log_name]->log(m);
    }
#else
    static void message(std::string log_name, std::string msg) {
        log_name = "### Qtoken - " + log_name;
        __android_log_print(ANDROID_LOG_DEBUG, log_name.data(), "%s",
                            msg.data());
    }
#endif

private:
    /**
     * Initializes logger log_name by adding
     * the appropriate channels and saving the log
     * handle to the logs map.
     * @param log_name used as log file name and map name
     */
    static void init_log(const std::string log_name) {
        // Console channel
        Poco::AutoPtr<Poco::ConsoleChannel> cons_chan(new Poco::ConsoleChannel);

        // Console formatting channel
        Poco::AutoPtr<Poco::PatternFormatter> cons_pat(
            new Poco::PatternFormatter);
        cons_pat->setProperty("pattern", "M:%M S:%S MS:%i %s: %t");
        Poco::AutoPtr<Poco::FormattingChannel> cons_form(
            new Poco::FormattingChannel(cons_pat, cons_chan));

        // File channel
        Poco::AutoPtr<Poco::SimpleFileChannel> file_chan(
            new Poco::SimpleFileChannel);
        file_chan->setProperty("path", Config::get("files.logs") + log_name);
        file_chan->setProperty("rotation", "500 K");

        // File formatting channel
        Poco::AutoPtr<Poco::PatternFormatter> file_pat(
            new Poco::PatternFormatter);
        file_pat->setProperty("pattern", "%Y-%m-%d %H:%M:%S %i %s [%p]: %t");
        Poco::AutoPtr<Poco::FormattingChannel> file_form(
            new Poco::FormattingChannel(file_pat, file_chan));

        // Splitter channel
        Poco::AutoPtr<Poco::SplitterChannel> split_chan(
            new Poco::SplitterChannel);
        split_chan->addChannel(cons_form);
        split_chan->addChannel(file_form);

        // Asynchronous channel
        Poco::AutoPtr<Poco::AsyncChannel> async_chan(
            new Poco::AsyncChannel(split_chan));

        Poco::Logger::get(log_name).setChannel(async_chan);

        logs[log_name] = async_chan;
    }

    inline static std::unordered_map<std::string,
                                     Poco::AutoPtr<Poco::AsyncChannel>>
        logs;
};

}  // namespace Qtoken

#endif  // LOGGER_H
