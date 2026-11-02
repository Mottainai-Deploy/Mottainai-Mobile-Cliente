package com.mottainai.cliente.notifications;

import android.app.job.JobParameters;
import android.app.job.JobService;
import android.os.Handler;
import android.os.Looper;
import java.util.concurrent.Future;

/** Consulta periódica sujeita a rede, bateria e agendamento do Android; não representa push instantâneo. */
public final class NotificationPollJob extends JobService {
    private Future<?> work;
    private Object active;
    private final Handler main = new Handler(Looper.getMainLooper());

    /** Executa a consulta fora da thread principal e conclui somente a execução ainda ativa. */
    @Override public boolean onStartJob(JobParameters params) {
        Object run = new Object(); active = run;
        work = CustomerNotifications.IO.submit(() -> {
            CustomerNotifications.poll(getApplicationContext());
            main.post(() -> { if (active == run) { active = null; jobFinished(params, false); } });
        });
        return true;
    }

    /** Interrompe a consulta quando o sistema revoga as condições de execução. */
    @Override public boolean onStopJob(JobParameters params) {
        active = null;
        if (work != null) work.cancel(true);
        return true;
    }
}
