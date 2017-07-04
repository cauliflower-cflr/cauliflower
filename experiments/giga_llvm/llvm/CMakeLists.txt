cmake_minimum_required(VERSION 2.8.8)
project(cauli)

set(CMAKE_EXPORT_COMPILE_COMMANDS "ON")

find_package(LLVM REQUIRED CONFIG)

message(STATUS "Found LLVM ${LLVM_PACKAGE_VERSION}")
message(STATUS "Using LLVMConfig.cmake in: ${LLVM_DIR}")

include(CheckCXXCompilerFlag)
CHECK_CXX_COMPILER_FLAG("-std=c++11" COMPILER_SUPPORTS_CXX11)
if(COMPILER_SUPPORTS_CXX11)
	set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -std=c++11 -fno-rtti -fPIC -Wall")
else()
	message(STATUS "The compiler ${CMAKE_CXX_COMPILER} has no C++11 support. Please use a different C++ compiler.")
endif()

include_directories(${LLVM_INCLUDE_DIRS} ../../include/cpp-btree-1.0.1/ ../../include/)
add_definitions(${LLVM_DEFINITIONS})

add_subdirectory (cauli)

