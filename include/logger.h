/* 
 *                                 logger.h
 * 
 * Author: Nic H.
 * Date: 2015-Aug-31
 */

#ifndef __LOGGER_H__
#define __LOGGER_H__

#include <vector>
#include <memory>
#include <iostream>

template<typename T>
std::ostream& operator<<(std::ostream& os, const std::vector<T>& vec){
    os << "[";
    for(auto i=vec.begin(); i!=vec.end(); ++i){
        if(i != vec.begin()) os << ",";
        os << *i;
    }
    return os << "]";
}

namespace cflr {

enum log_level{
    error,
    warning,
    info,
    debug,
    fine,
    finer,
    finest
};

class logger final {
private:
    static const std::unique_ptr<logger> instance;

    std::ostream& get_stream();
    void log_head(log_level);
    void log_tail();

    void log_body() {} //base case do nothing
    template<typename T, typename...Args>
    void log_body(const T& t, const Args&... args){
        instance->get_stream() << t;
        log_body(args...);
    }
public:
    static bool coloured;
    static log_level level;

    template<typename... Args>
    static void log(log_level lvl, const Args&... args){
        if(lvl <= level){
            instance->log_head(lvl);
            instance->log_body(args...);
            instance->log_tail();
        }
    }

    template<typename... Args>
    static inline void error(const Args&... args){
        log(log_level::error, args...);
    }
    template<typename... Args>
    static inline void warning(const Args&... args){
        log(log_level::warning, args...);
    }
    template<typename... Args>
    static inline void info(const Args&... args){
        log(log_level::info, args...);
    }
    template<typename... Args>
    static inline void debug(const Args&... args){
        log(log_level::debug, args...);
    }
    template<typename... Args>
    static inline void fine(const Args&... args){
        log(log_level::fine, args...);
    }
    template<typename... Args>
    static inline void finer(const Args&... args){
        log(log_level::finer, args...);
    }
    template<typename... Args>
    static inline void finest(const Args&... args){
        log(log_level::finest, args...);
    }
};

} // end namespace cflr

#endif /* __LOGGER_H__ */
