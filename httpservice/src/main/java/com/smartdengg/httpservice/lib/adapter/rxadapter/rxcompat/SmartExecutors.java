package com.smartdengg.httpservice.lib.adapter.rxadapter.rxcompat;

import android.os.Build;
import android.os.Process;
import com.smartdengg.httpservice.lib.utils.Util;
import java.io.File;
import java.io.FileFilter;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static android.os.Process.THREAD_PRIORITY_BACKGROUND;

/**
 * Created by Joker on 2015/8/24.
 */
public class SmartExecutors {

    public static final int DEVICE_INFO_UNKNOWN = 0;
    public static ExecutorService eventExecutor;
    private static final int CPU_COUNT = SmartExecutors.getCountOfCPU();
    private static final int CORE_POOL_SIZE = CPU_COUNT + 1;
    private static final int MAXIMUM_POOL_SIZE = CPU_COUNT * 2 + 1;
    private static final int KEEP_ALIVE = 1;
    private static final BlockingQueue<Runnable> eventPoolWaitQueue =
            new PriorityBlockingQueue<>(128);
    private static final ThreadFactory eventThreadFactory = new ThreadFactory() {
        private final AtomicInteger mCount = new AtomicInteger(1);

        public Thread newThread(Runnable runnable) {
            return new SmartThread(Util.checkNotNull(runnable, "Runnable == null"), "EventAsyncOrBackground #" + mCount.getAndIncrement());
        }
    };

    private static final RejectedExecutionHandler eventHandler =
            new ThreadPoolExecutor.DiscardOldestPolicy();

    static {
        eventExecutor =
                new ThreadPoolExecutor(CORE_POOL_SIZE, MAXIMUM_POOL_SIZE, KEEP_ALIVE, TimeUnit.SECONDS, eventPoolWaitQueue, eventThreadFactory, eventHandler);
    }

    /**
     * Linux中的设备都是以文件的形式存在，CPU也不例外，因此CPU的文件个数就等价与核数。
     * Android的CPU 设备文件位于/sys/devices/system/cpu/目录，文件名的的格式为cpu\d+。
     * 引用：http://www.jianshu.com/p/f7add443cd32#，感谢 liangfeizc :)
     * https://github.com/facebook/device-year-class
     */
    public static int getCountOfCPU() {

        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.GINGERBREAD_MR1) {
            return 1;
        }
        int count;
        try {
            count = new File("/sys/devices/system/cpu/").listFiles(CPU_FILTER).length;
        } catch (SecurityException | NullPointerException e) {
            count = DEVICE_INFO_UNKNOWN;
        }
        return count;
    }

    private static final FileFilter CPU_FILTER = new FileFilter() {
        @Override
        public boolean accept(File pathname) {

            String path = pathname.getName();
            if (path.startsWith("cpu")) {
                for (int i = 3; i < path.length(); i++) {
                    if (path.charAt(i) < '0' || path.charAt(i) > '9') {
                        return false;
                    }
                }
                return true;
            }
            return false;
        }
    };

    private static class SmartThread extends Thread {

        public SmartThread(Runnable runnable, String threadName) {
            super(runnable, threadName);
        }

        @Override
        public void run() {
            Process.setThreadPriority(THREAD_PRIORITY_BACKGROUND);
            super.run();
        }
    }
}
