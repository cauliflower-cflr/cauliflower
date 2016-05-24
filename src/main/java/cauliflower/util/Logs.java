package cauliflower.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

/**
 * A wrapper for whatever logging framework i'll use
 */
public class Logs {

    private static Map<Class<?>, Logger> logMap = new HashMap<>();

    public static Logger forClass(Class<?> cls){
        if(!logMap.containsKey(cls)) logMap.put(cls, LoggerFactory.getLogger(cls));
        return logMap.get(cls);
    }
}
