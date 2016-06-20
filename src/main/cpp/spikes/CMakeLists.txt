#==
# CMAKE META
#==
cmake_minimum_required(VERSION 3.1.0 FATAL_ERROR) # this version is needed for compiler-support checking

# release or debug build
if (NOT DEFINED ENV{CAULI_DEBUG})
    set(CMAKE_BUILD_TYPE Release) # for the O3 flag
else()
    set(CMAKE_BUILD_TYPE Debug)
endif()

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

#==
# COMPILER REQS
#==
set(needed_features cxx_constexpr cxx_auto_type cxx_static_assert cxx_nullptr)

#==
# EXECUTABLES
#==
add_executable(main main.cpp)
add_executable(b b.cpp)
add_executable(t t.cpp)
add_executable(multiply_time multiply_time.cpp)

target_compile_features(main PRIVATE ${needed_features})
target_compile_features(b PRIVATE ${needed_features})
target_compile_features(t PRIVATE ${needed_features})
target_compile_features(multiply_time PRIVATE ${needed_features})
