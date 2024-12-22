package com.social100.todero.scheduler;

import java.util.Timer;
import java.util.TimerTask;

public class TaskScheduler {

    private Timer timer;

    public TaskScheduler() {
        timer = new Timer();
    }

    /**
     * Schedule a task to run periodically.
     *
     * @param task     The task to be executed.
     * @param interval The interval in milliseconds between executions.
     */
    public void scheduleTask(Runnable task, long interval) {
        TimerTask timerTask = new TimerTask() {
            @Override
            public void run() {
                task.run();
            }
        };

        // Schedule the task to run periodically
        timer.scheduleAtFixedRate(timerTask, 0, interval);
    }

    /**
     * Stop the scheduler and cancel all scheduled tasks.
     */
    public void stop() {
        if (timer != null) {
            timer.cancel();
            timer = null;
        }
    }
}
