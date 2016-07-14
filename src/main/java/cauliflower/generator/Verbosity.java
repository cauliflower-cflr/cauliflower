package cauliflower.generator;

import cauliflower.application.Configuration;

/**
 * Verbosity
 * <p>
 * Author: nic
 * Date: 14/07/16
 */
public class Verbosity {

    private boolean time;
    private boolean size;
    private boolean report;
    private boolean status;

    public Verbosity(Configuration conf){
        this(conf.timers || conf.optimise, conf.optimise, conf.reports, conf.statuses);
    }

    public Verbosity(boolean timers, boolean sizes, boolean reports, boolean statuses){
        this.time = timers;
        this.size = sizes;
        this.report = reports;
        this.status = statuses;
    }

    public boolean isTimed() {
        return time;
    }

    public boolean isSized() {
        return size;
    }

    public boolean isReporting() {
        return report;
    }

    public boolean isStatusing() {
        return status;
    }
}
