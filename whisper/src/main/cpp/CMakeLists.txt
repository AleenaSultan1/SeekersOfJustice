cmake_minimum_required(VERSION 3.22)
project(whisper_jni C CXX)

set(WHISPER_LIB_DIR ${CMAKE_SOURCE_DIR}/../../../../third_party/whisper.cpp)

set(WHISPER_BUILD_EXAMPLES OFF CACHE BOOL "" FORCE)
set(WHISPER_BUILD_TESTS OFF CACHE BOOL "" FORCE)
set(WHISPER_BUILD_SERVER OFF CACHE BOOL "" FORCE)

add_subdirectory(${WHISPER_LIB_DIR} whisper_build EXCLUDE_FROM_ALL)

add_library(whisper_jni SHARED jni.c)

target_link_libraries(whisper_jni whisper log android)
