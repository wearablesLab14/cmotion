package cmotion.wearables.ese.freiburg.de.cmotion;

import android.annotation.SuppressLint;
import android.os.AsyncTask;
import android.os.Build;

/**
 * A helper class to allow multiple tasks to run in parallel on a pool of threads managed by AsyncTask.
 */
class TaskHelper {

    public static <P, T extends AsyncTask<P, ?, ?>> void execute(T task) {
        execute(task, (P[]) null);
    }

    @SuppressLint("NewApi")
    public static <P, T extends AsyncTask<P, ?, ?>> void execute(T task, P... params) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
            task.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, params);
        } else {
            task.execute(params);
        }
    }
}