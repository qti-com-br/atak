#include "benchmark/benchmark.h"

#include "../../src/coders/pipeline.cpp"
#include "bench_utils.hpp"
#include "coders/concurrent_decoder.hpp"
#include "coders/concurrent_encoder.hpp"
#include "coders/decoder.hpp"
#include "coders/encoder.hpp"
#include "coders/entanglement_decoder.hpp"
#include "coders/entanglement_encoder.hpp"
#include "coders/pipeline.hpp"
#include "coders/polar_decoder.hpp"
#include "coders/polar_encoder.hpp"
#include "receipt/crypto_receipt.hpp"
#include "tools/chunker.hpp"
#include "tools/types.hpp"

static void FullPipeline(benchmark::State &state) {
    int file_size = state.range(0);
    int chunk_size = state.range(1);

    Pipeline<Encoder> p_encode;
    p_encode.add(3, new PolarEncoder(), new ConcurrentEncoder(), new EntanglementEncoder());

    Pipeline<Decoder> p_decode;
    p_decode.add(3, new EntanglementDecoder(), new ConcurrentDecoder(), new PolarDecoder());

    while (state.KeepRunning()) {
        // Timer pause so we don't time writer allocation
        state.PauseTiming();

        // To avoid reusing the same receipt we re-allocate
        // each iteration
        Writer w;
        w.first = alloc_rand_chunker(file_size, chunk_size);
        w.second = new CryptoReceipt();
        
        state.ResumeTiming();

        p_encode.run(w);
        p_decode.run(w);

        delete w.first;
        delete w.second;
    }    
}

// Args({file_size, chunk_size});
BENCHMARK(FullPipeline)
    ->Args({1024, 256})
    ->Args({10000, 50});

static void ConcPipeline(benchmark::State &state) {
    int file_size = state.range(0);
    int chunk_size = state.range(1);

    Pipeline<Encoder> p_encode;
    p_encode.add(1, new ConcurrentEncoder());

    Pipeline<Decoder> p_decode;
    p_decode.add(1, new ConcurrentDecoder());

    while (state.KeepRunning()) {
        // Timer pause so we don't time writer allocation
        state.PauseTiming();

        // To avoid reusing the same receipt we re-allocate
        // each iteration
        Writer w;
        w.first = alloc_rand_chunker(file_size, chunk_size);
        w.second = new CryptoReceipt();
        
        state.ResumeTiming();

        p_encode.run(w);
        p_decode.run(w);

        delete w.first;
        delete w.second;
    }    
}

// Args({file_size, chunk_size});
BENCHMARK(ConcPipeline)
    ->Args({1024, 256})
    ->Args({10000, 50});
    
static void PolarPipeline(benchmark::State &state) {
    int file_size = state.range(0);
    int chunk_size = state.range(1);

    Pipeline<Encoder> p_encode;
    p_encode.add(1, new PolarEncoder());

    Pipeline<Decoder> p_decode;
    p_decode.add(1, new PolarDecoder());

    while (state.KeepRunning()) {
        // Timer pause so we don't time writer allocation
        state.PauseTiming();

        // To avoid reusing the same receipt we re-allocate
        // each iteration
        Writer w;
        w.first = alloc_rand_chunker(file_size, chunk_size);
        w.second = new CryptoReceipt();
        
        state.ResumeTiming();

        p_encode.run(w);
        p_decode.run(w);

        delete w.first;
        delete w.second;
    }    
}

// Args({file_size, chunk_size});
BENCHMARK(PolarPipeline)
    ->Args({1024, 256})
    ->Args({10000, 50});

static void EntPipeline(benchmark::State &state) {
    int file_size = state.range(0);
    int chunk_size = state.range(1);

    Pipeline<Encoder> p_encode;
    p_encode.add(1, new EntanglementEncoder());

    Pipeline<Decoder> p_decode;
    p_decode.add(1, new EntanglementDecoder());

    while (state.KeepRunning()) {
        // Timer pause so we don't time writer allocation
        state.PauseTiming();

        // To avoid reusing the same receipt we re-allocate
        // each iteration
        Writer w;
        w.first = alloc_rand_chunker(file_size, chunk_size);
        w.second = new CryptoReceipt();
        
        state.ResumeTiming();

        // p_encode.run(w);
        // p_decode.run(w);

        delete w.first;
        delete w.second;
    }    
}

// Args({file_size, chunk_size});
BENCHMARK(EntPipeline)
    ->Args({1024, 256})
    ->Args({10000, 50})
    ->Iterations(100);