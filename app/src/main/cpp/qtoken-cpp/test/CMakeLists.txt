enable_testing()

# Install and set GTest location needed by both unit and benchmarks
find_package(GTest REQUIRED)

add_subdirectory(adversarial_tests)
add_subdirectory(benchmark_tests)
add_subdirectory(unit_tests)
