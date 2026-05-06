cmake_minimum_required(VERSION 3.22.1)
project("accesschecker_native")

add_library(accesschecker_native SHARED native_checks.c)
target_link_libraries(accesschecker_native log)
