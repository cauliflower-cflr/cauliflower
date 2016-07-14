package cauliflower.application;

import cauliflower.util.Logs;

/**
 * CauliflowerException
 * <p>These exceptions cannot be caught, their job is to log the error, recurse back to Cauliflower.main and cause the system to exit</p>
 * Author: nic
 * Date: 14/07/16
 */
public class CauliflowerException extends Exception {

    private final Class<?> throwingClass;
    private final String message;

    public CauliflowerException(Class<?> clazz, String msg){
        this.throwingClass = clazz;
        this.message = msg;
        Logs.forClass(getThrowingClass()).error(getMessage());
    }

    @Override
    public String getMessage() {
        return message;
    }

    public Class<?> getThrowingClass() {
        return throwingClass;
    }

    public int getExitCode(){
        return Info.FAILURE_EXEC;
    }
}
