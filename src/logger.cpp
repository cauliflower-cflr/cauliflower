/* 
 *                                logger.cpp
 * 
 * Author: Nic H.
 * Date: 2015-Sep-01
 * 
 * Logging framework implementation
 */

#include <iostream>
#include <sstream>
#include <iomanip>
#include <chrono>
#include <ctime>

#include "logger.h"

using namespace std;

namespace cflr{

//
// Utility methods
//
inline const char* lvl_word(log_level ll){
    switch(ll){
        case log_level::error :   return "ERROR  ";
        case log_level::warning : return "WARNING";
        case log_level::info :    return "INFO   ";
        case log_level::debug :   return "DEBUG  ";
        case log_level::fine :    return "FINE   ";
        case log_level::finer :   return "FINER  ";
        default :  return "FINEST ";
    }
}

inline const char* lvl_code(log_level ll){
    if(!logger::coloured) return "";
    switch(ll){
        case log_level::error : return "\x1b[31m";
        case log_level::warning : return "\x1b[33m";
        case log_level::info : return "\x1b[36m";
        case log_level::debug : return "\x1b[32m";
        default : return "\x1b[34m";
    }
}

inline const char* lvl_code(){
    if(!logger::coloured) return "";
    return "\x1b[39m";
}

//
// Private Members
//

const std::unique_ptr<logger> logger::instance = std::unique_ptr<logger>(new logger());
bool logger::coloured = false;
log_level logger::level = log_level::info;

std::ostream& logger::get_stream(){
    return cerr;
}

void logger::log_head(log_level lvl){
    auto p = chrono::system_clock::now();
    auto t = chrono::system_clock::to_time_t(p);
    string tm = ctime(&t);
    get_stream() << lvl_code(lvl) << lvl_word(lvl) << "[" << tm.substr(0, tm.size()-1) << "]: ";
}

void logger::log_tail(){
    get_stream() << lvl_code() << endl;
}

} // end namespace cflr

