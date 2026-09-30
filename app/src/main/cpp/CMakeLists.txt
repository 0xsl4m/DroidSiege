cmake_minimum_required(VERSION 3.22.1)
project("droidsiege-vault")

add_library(droidsiege-vault SHARED native-vault.cpp)

find_library(log-lib log)
target_link_libraries(droidsiege-vault ${log-lib})
