package org.bamof.sim;

import org.bamof.model.Algorithm;
import org.bamof.scheduler.AmrpScheduler;
import org.bamof.scheduler.BadpPlusScheduler;
import org.bamof.scheduler.BadpScheduler;
import org.bamof.scheduler.CdaScalerScheduler;
import org.bamof.scheduler.CeduleScheduler;
import org.bamof.scheduler.CemaScheduler;
import org.bamof.scheduler.EdfScheduler;
import org.bamof.scheduler.HpaScheduler;
import org.bamof.scheduler.Scheduler;

public final class SchedulerFactory {
    private SchedulerFactory() {
    }

    public static Scheduler create(Algorithm algorithm) {
        return switch (algorithm) {
            case EDF -> new EdfScheduler();
            case HPA -> new HpaScheduler();
            case AMRP -> new AmrpScheduler();
            case CDASCALER -> new CdaScalerScheduler();
            case CEMA -> new CemaScheduler();
            case CEDULE -> new CeduleScheduler();
            case BADP -> new BadpScheduler();
            case BADP_PLUS -> new BadpPlusScheduler();
        };
    }
}
