package nachos.threads;

import nachos.machine.*;
import java.util.PriorityQueue;
import java.util.Comparator;
/**
 * Uses the hardware timer to provide preemption, and to allow threads to sleep
 * until a certain time.
 */
public class Alarm {
	/**
	 * Allocate a new Alarm. Set the machine's timer interrupt handler to this
	 * alarm's callback.
	 *
	 * <p>
	 * <b>Note</b>: Nachos will not function correctly with more than one alarm.
	 */
	public Alarm() {
		Machine.timer().setInterruptHandler(new Runnable() {
			public void run() {
				timerInterrupt();
			}
		});
	}

	/**
	 * The timer interrupt handler. This is called by the machine's timer
	 * periodically (approximately every 500 clock ticks). Causes the current
	 * thread to yield, forcing a context switch if there is another thread that
	 * should be run.
	 */
	public void timerInterrupt() {
		Machine.interrupt().disable();
		while (!sleepedThreads.isEmpty() && sleepedThreads.peek().wakeTime <= Machine.timer().getTime())
		{
			KThread thread = sleepedThreads.poll().thread;
			if (thread != null)
				thread.ready();
		}
		KThread.currentThread().yield();
		Machine.interrupt().enable();
	}

	/**
	 * Put the current thread to sleep for at least <i>x</i> ticks, waking it up
	 * in the timer interrupt handler. The thread must be woken up (placed in
	 * the scheduler ready set) during the first timer interrupt where
	 *
	 * <p>
	 * <blockquote> (current time) >= (WaitUntil called time)+(x) </blockquote>
	 *
	 * @param x the minimum number of clock ticks to wait.
	 *
	 * @see nachos.machine.Timer#getTime()
	 */
	public void waitUntil(long x) {
		// for now, cheat just to get something working (busy waiting is bad)
		long wakeTime = Machine.timer().getTime() + x;
		// while (wakeTime > Machine.timer().getTime())
		// 	KThread.yield();
		KThread thread = KThread.currentThread();
		ThreadTime threadtime = new ThreadTime(thread, wakeTime);
		Machine.interrupt().disable();
		sleepedThreads.add(threadtime);
		KThread.sleep();
		Machine.interrupt().enable();
	}

	// Comparator<KThread> cmp = new Comparator<KThread>() {
	// 	public int compare(KThread a, KThread b) {
	// 		return a.timer - b.timer < 0 ? -1 : 1;
	// 	}
	// };
	PriorityQueue<ThreadTime> sleepedThreads = new PriorityQueue<ThreadTime>(new Ascend());

	private class Ascend implements Comparator<ThreadTime> {
		public int compare(ThreadTime a, ThreadTime b) {
			return a.wakeTime - b.wakeTime < 0 ? -1 : 1;
		}
	}

	private class ThreadTime {

		public ThreadTime(KThread thread, long wakeTime){
			this.thread = thread;
			this.wakeTime = wakeTime;
		}

		private KThread thread;
		private long wakeTime;
	}

	private static class PingAlarmTest implements Runnable {
		PingAlarmTest(int which, Alarm alarm) {
			this.which = which;
			this.alarm = alarm;
		}
		// Alarm alarm;
		public void run() {
			System.out.println("thread " + which + " started.");
			alarm.waitUntil(which);
			System.out.println("thread "+ which + " ran.");
		}
		private int which;
	}

	public static void selfTest() {
		Alarm myAlarm = new Alarm();
		System.out.println("Alarm self test:");
		KThread thread1 = new Thread(new PingAlarmTest(10000, myAlarm));
		thread1.fork();
		KThread thread2 = new Thread(new PingAlarmTest(5000, myAlarm));
		thread2.fork();

		new PingAlarmTest(20000, myAlarm).run();

		System.out.println("exiting Alarm self test!")
	}
}
