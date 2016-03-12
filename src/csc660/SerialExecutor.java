package csc660;

import java.util.ArrayDeque;
import java.util.Queue;
import java.util.concurrent.Executor;

/**
 * Useful serial executor class. Executes the given runnables in order without
 * blocking the main thread.
 * 
 * Found at
 * https://docs.oracle.com/javase/7/docs/api/java/util/concurrent/Executor.html
 */
public class SerialExecutor implements Executor {
	final Queue<Runnable> tasks = new ArrayDeque<Runnable>();
	final Executor executor;
	Runnable active;

	SerialExecutor(Executor executor) {
		this.executor = executor;
	}

	public synchronized void execute(final Runnable r) {
		tasks.offer(new Runnable() {
			public void run() {
				try {
					r.run();
				} finally {
					scheduleNext();
				}
			}
		});
		if (active == null) {
			scheduleNext();
		}
	}

	private synchronized void scheduleNext() {
		if ((active = tasks.poll()) != null) {
			executor.execute(active);
		}
	}
}