#include "benchmark/benchmark.h"

#include "bench_utils.hpp"
#include "coders/decoder.hpp"
#include "coders/encoder.hpp"
#include "coders/concurrent_decoder.hpp"
#include "coders/concurrent_encoder.hpp"
#include "coders/polar_decoder.hpp"
#include "coders/polar_encoder.hpp"
#include "tools/types.hpp"

static void ConcEncodeBits(benchmark::State &state) {
    int overlaid_frames = state.range(0);
    int frames = state.range(1);
    int n = state.range(2);
    int k = state.range(3);

    Bitlist info(frames * k);
    for(int i = 0; i < info.size(); i++){
        info[i] = bool(get_rand_seed() % 2);
    }

    ConcurrentEncoder encoder(overlaid_frames, frames, n, k);
    ConcurrentDecoder decoder(overlaid_frames, frames, n, k);

    while (state.KeepRunning()) {
        Bitlist output = std::move(encoder.encode(info));
        Bitlist decoded = std::move(decoder.decode(output));
    }
}

// Args({overlaid_frames, frames, n, k});
BENCHMARK(ConcEncodeBits)
    ->Args({1, 1, 32, 4})
    ->Args({1, 1, 64, 8})
    ->Args({1, 1000, 32, 4})
    ->Args({1, 10000, 32, 4});

static void PolarEncodeBits(benchmark::State &state) {
    int frames = state.range(0);
    int n = state.range(1);
    int k = state.range(2);

    std::vector<int> info(k * frames);
    std::generate(info.begin(), info.end(), std::rand);
    std::for_each(info.begin(), info.end(), [](int& n) { n = n % 2; });

    PolarEncoder encoder(frames, n, k);
    PolarDecoder decoder(frames, n, k);

    while (state.KeepRunning()) {
        std::vector<int> output = std::move(encoder.encode(info));
        std::vector<int> decoded= std::move(decoder.decode(output));
    }
}

// Args({frames, n, k});
BENCHMARK(PolarEncodeBits)
    ->Args({1, 128, 32})
    ->Args({1, 256, 64})
    ->Args({1000, 128, 32})
    ->Args({10000, 128, 32});