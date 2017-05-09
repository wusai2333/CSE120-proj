package nachos.threads;

import nachos.machine.*;

/**
 * A <i>communicator</i> allows threads to synchronously exchange 32-bit
 * messages. Multiple threads can be waiting to <i>speak</i>, and multiple
 * threads can be waiting to <i>listen</i>. But there should never be a time
 * when both a speaker and a listener are waiting, because the two threads can
 * be paired off at this point.
 */
public class Communicator {
	/**
	 * Allocate a new communicator.
	 */
	public Communicator() {
		mutex = new Lock();
		message = null;
		speakers = new Condition2(mutex);
		listeners = new Condition2(mutex);
		acknowledge = new Condition2(mutex);
	}

	/**
	 * Wait for a thread to listen through this communicator, and then transfer
	 * <i>word</i> to the listener.
	 *
	 * <p>
	 * Does not return until this thread is paired up with a listening thread.
	 * Exactly one listener should receive <i>word</i>.
	 *
	 * @param word the integer to transfer.
	 */
	public void speak(int word) {
		mutex.acquire();
		while (message != null)
		{
			speakers.sleep();
		}
		message = new Integer(word);
		listeners.wake();
		acknowledge.sleep();
		mutex.release();
	}

	/**
	 * Wait for a thread to speak through this communicator, and then return the
	 * <i>word</i> that thread passed to <tt>speak()</tt>.
	 *
	 * @return the integer transferred.
	 */
	public int listen() {
		mutex.acquire();
		while(message == null ) {
			speakers.wake();
			listeners.sleep();
		}
		int receivedWord = message.intValue();
		message = null;
		acknowledge.wake();
		mutex.release();
		return receivedWord;
	}

	private static class Speaker implements Runnable {
		Speaker(Communicator com, String name, int message) {
			this.com = com;
			this.name = name;
			this.message = message;
		}

		public void run() {
			System.out.println(name + " says " + message);
			System.out.println(name + " is done.");
			com.speak(message);

		}
		private Communicator com;
		private String name;
		private int message;
	}

	private static class Listener implements Runnable {
		Listener(Communicator com, String name) {
			this.com = com;
			this.name = name;
		}

		public void run() {
			System.out.println(name + " is ready to listen.");
			int receivedWord = com.listen();
			System.out.println(name + " hears " + receivedWord);
			System.out.println(name + " is done. ");
		}
		private Communicator com;
		private String name;
	}

	public static void selfTest() {
		Communicator com1 = new Communicator();

		KThread thread1 = new KThread(new Speaker(com1, "sai", 1));
		KThread thread2 = new KThread(new Listener(com1, "xinyang"));
		KThread thread3 = new KThread(new Speaker(com1, "aaa", 2));
		thread1.fork();
		thread2.fork();
		thread3.fork();

		new Listener(com1, "billy").run();
	}

	private Lock mutex;
	private Condition2 speakers;
	private Condition2 listeners;
	private Condition2 acknowledge;
	private int listening;
	private Integer message;
}
