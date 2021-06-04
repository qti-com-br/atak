#include <iomanip>
#include <iostream>
#include <vector>

#include "../../src/coders/pipeline.cpp"
#include "adv_utils.hpp"
#include "bsc.hpp"
#include "channel.hpp"
#include "../coders/concurrent_decoder.hpp"
#include "../coders/concurrent_encoder.hpp"
#include "../coders/entanglement_decoder.hpp"
#include "../coders/entanglement_encoder.hpp"
#include "../coders/pipeline.hpp"
#include "../coders/polar_decoder.hpp"
#include "../coders/polar_encoder.hpp"
#include "jammer.hpp"

const std::string FILE_SIZE_FLAG = "-f";
const int FILE_SIZE_DEFAULT = 10000;
const std::string CHUNK_SIZE_FLAG = "-c";
const int CHUNK_SIZE_DEFAULT = 500;
const std::string FRAME_SIZE_FLAG = "-fr";
const int FRAME_SIZE_DEFAULT = 128;

const std::string P_START_FLAG = "-pstart";
const double P_START_DEFAULT = 0;
const std::string P_END_FLAG = "-pend";
const double P_END_DEFAULT = 10;
const std::string P_STEP_FLAG = "-pstep";
const double P_STEP_DEFAULT = .5;

const std::string CODERS_FLAG = "-pipe";
const std::string CODERS_DEFAULT = "pe";
const char POLAR_CHAR = 'p';
const char CONC_CHAR = 'c';
const char ENT_CHAR = 'e';

const std::string CHANNEL_FLAG = "-chan";
const std::string CHANNEL_DEFAULT = "s";  // Base channel default for now
const char BSC_CHAR = 's';
const char JAM_CHAR = 'j';

const std::string LIST_FLAG = "-l";
const char BASE_CHAR = 'b';  // No op char for empty channel/pipeline
const int COL_SIZE = 15;     // Output column width

struct params {
    std::string channels = CHANNEL_DEFAULT;
    std::string coders = CODERS_DEFAULT;
    int file_size = FILE_SIZE_DEFAULT;
    int chunk_size = CHUNK_SIZE_DEFAULT;
    int frame_size = FRAME_SIZE_DEFAULT;
    double p_start = P_START_DEFAULT;
    double p_end = P_END_DEFAULT;
    double p_step = P_STEP_DEFAULT;
};

struct pipeline {
    Pipeline<Encoder> enc;
    Pipeline<Decoder> dec;
};

void read_args(std::vector<std::string> args, params& input);
void add_coders(params& input, pipeline& p);
void add_channels(params& input, pipeline& p);
void run_p_range(params& input, pipeline p);
void run_p(params& input, pipeline p);
void compare_ch(Chunker& in, Chunker& out, int frame_size, double& ber,
                double& fer);
void print_header(const params& input);
void print_commands();

int main(int argc, char* argv[]) {
    std::vector<std::string> args(argv + 1, argv + argc);
    params input;
    pipeline p;

    read_args(args, input);
    add_coders(input, p);
    run_p_range(input, p);

    return 0;
}

/**
 * Loops through provided probability range
 * calling run_p
 * @param input user input struct
 * @param p encode and decode pipeline
 */
void run_p_range(params& input, pipeline p) {
    print_header(input);

    while (input.p_start <= input.p_end) {
        run_p(input, p);
        input.p_start += input.p_step;
    }
}

/**
 * Attaches channels to pipeline copy
 * Creates random test chunker
 * Runs chunker through pipeline
 * Compares results
 * @param input user input struct
 * @param p encode and decode pipeline
 */
void run_p(params& input, pipeline p) {
    Writer w;
    double ber = 0;
    double fer = 0;
    w.first = alloc_rand_chunker(input.file_size, input.chunk_size);
    w.second = new CryptoReceipt();
    Chunker ch(*w.first);

    add_channels(input, p);

    p.enc.run(w);
    p.dec.run(w);

    compare_ch(*w.first, ch, input.frame_size, ber, fer);

    std::cout << std::left << std::setw(COL_SIZE)
              << std::to_string(input.p_start);
    std::cout << std::left << std::setw(COL_SIZE) << std::to_string(ber);
    std::cout << std::left << std::setw(COL_SIZE) << std::to_string(fer);
    std::cout << std::endl;

    delete w.first;
    delete w.second;
}

/**
 * Compares chunker in and chunker out
 * Calculates bit error rate and frame error rate
 * @param in original chunker
 * @param out chunker run through pipeline
 * @param frame_size frame size for fer
 * @param ber bit error rate %
 * @param fer frame error rate %
 */
void compare_ch(Chunker& in, Chunker& out, int frame_size, double& ber,
                double& fer) {
    auto in_chunks = in.get_padded_chunks();
    auto out_chunks = out.get_padded_chunks();
    int error_frame = 0;
    int min_size;
    int max_size;
    int be = 0;
    int fe = 0;

    // Serialize both chunkers into Bytelist
    Bytelist serial_in_bytes;
    Bytelist serial_out_bytes;

    for (auto shard : in_chunks) {
        serial_in_bytes.insert(serial_in_bytes.end(), shard.begin(),
                               shard.end());
    }

    for (auto shard : out_chunks) {
        serial_out_bytes.insert(serial_out_bytes.end(), shard.begin(),
                                shard.end());
    }

    // Convert to bit format
    Bitlist serial_in = bytes_to_bits<Bitlist>(serial_in_bytes);
    Bitlist serial_out = bytes_to_bits<Bitlist>(serial_out_bytes);

    // Get the smaller and larger of the two sizes
    min_size = serial_in.size() > serial_out.size() ? serial_in.size()
                                                    : serial_out.size();
    max_size = serial_in.size() < serial_out.size() ? serial_in.size()
                                                    : serial_out.size();

    // Add any difference in size to the bit errors and frame errors
    be += abs(int(serial_in.size() - serial_out.size()));
    fe += abs(int(serial_in.size() - serial_out.size())) / frame_size;

    for (int i = 0; i < min_size; i++) {
        // If we find a bit error increment bit errors
        if (serial_in[i] != serial_out[i]) {
            // Mark the frame as errored
            error_frame = 1;
            be++;
        }

        // If we reach the end of the frame having found a frame error
        if (i % frame_size == 0) {
            // Increment frame error and reset for next frame
            fe += error_frame;
            error_frame = 0;
        }
    }

    ber = double(be) / double(max_size);
    fer = double(fe) / double(max_size / frame_size);

    // Convert error rates to percentages
    fer *= 100;
    ber *= 100;
}

/**
 * Reads user input into params struct
 * @param args raw user input
 * @param input input struct
 */
void read_args(std::vector<std::string> args, params& input) {
    for (int i = 0; i < args.size(); i++) {
        if (args[i] == LIST_FLAG) {
            print_commands();
        } else if (args[i] == CHANNEL_FLAG) {
            input.channels = args[i + 1];
        } else if (args[i] == CODERS_FLAG) {
            input.coders = args[i + 1];
        } else if (args[i] == FILE_SIZE_FLAG) {
            input.file_size = std::stoi(args[i + 1]);
        } else if (args[i] == CHUNK_SIZE_FLAG) {
            input.chunk_size = std::stoi(args[i + 1]);
        } else if (args[i] == FRAME_SIZE_FLAG) {
            input.frame_size = std::stoi(args[i + 1]);
        } else if (args[i] == P_START_FLAG) {
            input.p_start = std::stod(args[i + 1]);
        } else if (args[i] == P_END_FLAG) {
            input.p_end = std::stod(args[i + 1]);
        } else if (args[i] == P_STEP_FLAG) {
            input.p_step = std::stod(args[i + 1]);
        }
    }
}

/**
 * Attaches coders specified in input to
 * pipeline p
 * @param input user input
 * @param p pipeline
 */
void add_coders(params& input, pipeline& p) {
    for (char c : input.coders) {
        if (c == POLAR_CHAR) {
            p.enc.add(1, new PolarEncoder());
        } else if (c == CONC_CHAR) {
            p.enc.add(1, new ConcurrentEncoder());
        } else if (c == ENT_CHAR) {
            p.enc.add(1, new EntanglementEncoder());
        } else if (c == BASE_CHAR) {
            // No op
        }
    }

    for (int i = input.coders.size() - 1; i >= 0; i--) {
        if (input.coders[i] == POLAR_CHAR) {
            p.dec.add(1, new PolarDecoder());
        } else if (input.coders[i] == CONC_CHAR) {
            p.dec.add(1, new ConcurrentDecoder());
        } else if (input.coders[i] == ENT_CHAR) {
            p.dec.add(1, new EntanglementDecoder());
        } else if (input.coders[i] == BASE_CHAR) {
            // No op
        }
    }
}

/**
 * Attaches channels specified in input to
 * pipeline p
 * @param input user input
 * @param p pipeline
 */
void add_channels(params& input, pipeline& p) {
    for (char c : input.channels) {
        if (c == BASE_CHAR) {
            p.enc.add(1, new Channel());
        } else if (c == BSC_CHAR) {
            p.enc.add(1, new BSC(input.p_start));
        } else if (c == JAM_CHAR) {
            p.enc.add(1, new Jammer(input.p_start));
        }
    }
}

/**
 * Echoes parameters for run
 * @param input user input
 */
void print_header(const params& input) {
    std::cout << "Run with -l flag to list commands." << std::endl;
    std::cout << "Iterating through p range " << input.p_start << "->"
              << input.p_end << " with step size " << input.p_step << std::endl;
    std::cout << "File size(bytes): " << input.file_size
              << " Chunk size(bytes): " << input.chunk_size
              << " Frame size(bits): " << input.frame_size << std::endl;
    std::cout << "Encoders: " << input.coders << " Channels: " << input.channels
              << std::endl;
    std::cout << std::endl;
    std::cout << std::left << std::setw(COL_SIZE) << "P%";
    std::cout << std::left << std::setw(COL_SIZE) << "BER";
    std::cout << std::left << std::setw(COL_SIZE) << "FER";
    std::cout << std::endl;
}

/**
 * Usage info then exit
 */
void print_commands() {
    std::cout << "Usage:" << std::endl;
    std::cout << "-f <file-size-bytes>" << std::endl;
    std::cout << "-c <chunk-size-bytes>" << std::endl;
    std::cout << "-fr <frame-size-bits>" << std::endl;
    std::cout << "-pstart <min-channel-probability>" << std::endl;
    std::cout << "-pend <max-channel-probability>" << std::endl;
    std::cout << "-pstep <probability-step-size>" << std::endl;
    std::cout << "-pipe <encoder-char-list>" << std::endl;
    std::cout << "b=base(no-op) p=polar c=concurrent e=entanglement"
              << std::endl;
    std::cout << "-chan <channel-char-list>" << std::endl;
    std::cout << "b=base(no-op) s=binary-symmetric j=jammer" << std::endl;
    exit(0);
}