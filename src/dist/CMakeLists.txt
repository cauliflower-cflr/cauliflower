#============================================================================#
#                               CMake Template                               #
#                                                                            #
# Author: nic                                                                #
# Date: 2016-May-19                                                          #
#                                                                            #
# This template is used to compile the executable for the Caulifloer-        #
# generated front-end. It must be templatized with two environtment          #
# variables:                                                                 #
#                                                                            #
#   CAULI_FRONT       Absolute path to the front-end source file.            #
#   CAULI_NAME        Name of the executable                                 #
#============================================================================#

#==
# CMAKE META
#==
cmake_minimum_required(VERSION 3.1.0 FATAL_ERROR) # this version is needed for compiler-support checking

set(CMAKE_BUILD_TYPE Release) # for the O3 flag

# checking environtment variables are defined
if (NOT DEFINED ENV{CAULI_FRONT})
    message( FATAL_ERROR "CAULI_FRONT is not defined" )
endif()
if (NOT DEFINED ENV{CAULI_NAME})
    message( FATAL_ERROR "CAULI_NAME is not defined" )
endif()

message( "Name:    " $ENV{CAULI_NAME})
message( "Src:     " $ENV{CAULI_FRONT})

#==
# INCLUDES
#==
include_directories(include/)
include_directories(include/cpp-btree-1.0.1/)

#==
# COMPILER FLAGS
#==
add_definitions(-DCSV_IO_NO_THREAD) # force csv-reading to be single-thread

find_package(OpenMP)
if (OPENMP_FOUND)
    set (CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} ${OpenMP_CXX_FLAGS}")
endif()
set (CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -O3")

#==
# COMPILER REQS
#==
set(needed_features cxx_constexpr cxx_auto_type cxx_static_assert cxx_nullptr)

#==
# EXECUTABLES
#==
add_executable($ENV{CAULI_NAME} $ENV{CAULI_FRONT})
target_compile_features($ENV{CAULI_NAME} PRIVATE ${needed_features})
