#==
# CMAKE META
#==
cmake_minimum_required(VERSION 3.1.0 FATAL_ERROR) # this version is needed for compiler-support checking

set(CMAKE_BUILD_TYPE Debug)

set( CMAKE_EXPORT_COMPILE_COMMANDS 1 )

#==
# INCLUDES
#==
include_directories(../../dist/include/)
include_directories(../../dist/include/cpp-btree-1.0.1/)

#==
# COMPILER FLAGS
#==
add_definitions(-DCSV_IO_NO_THREAD) # force csv-reading to be single-thread

find_package(OpenMP)
if (OPENMP_FOUND)
    set (CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} ${OpenMP_CXX_FLAGS}")
endif()

find_package(Boost)
if (Boost_FOUND)
    include_directories(${Boost_INCLUDE_DIRS})
endif()

#==
# COMPILER REQS
#==
set(needed_features cxx_constexpr cxx_auto_type cxx_static_assert cxx_nullptr)

#==
# EXECUTABLES
#==
add_executable(test_exe test.cpp
    test_neighbourhood_map.cpp
    test_registrar.cpp
    test_relation_buffer.cpp
    test_pmap.cpp
    )

target_compile_features(test_exe PRIVATE ${needed_features})

enable_testing()

add_test(all test_exe "--log_level=test_suite" "--report_level=detailed")
