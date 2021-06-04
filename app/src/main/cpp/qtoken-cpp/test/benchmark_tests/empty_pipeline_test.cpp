#include "benchmark/benchmark.h"

#include "../../src/coders/pipeline.cpp"
#include "bench_utils.hpp"
#include "../coders/decoder.hpp"
#include "../coders/encoder.hpp"
#include "../coders/pipeline.hpp"
#include "../receipt/crypto_receipt.hpp"
#include "../tools/chunker.hpp"
#include "../tools/types.hpp"

static void EmptyPipeline(benchmark::State &state) {
    Pipeline<Encoder> p;

    Writer w;
    w.first = alloc_rand_chunker(state.range(0), state.range(1));
    w.second = new CryptoReceipt();

    while (state.KeepRunning()) {
        p.run(w);
    }

    delete w.first;
    delete w.second;
}

// Args({file_size, chunk_size});
BENCHMARK(EmptyPipeline)
    ->Args({1024, 256})
    ->Args({100000, 500})
    ->Args({1000000, 5000});