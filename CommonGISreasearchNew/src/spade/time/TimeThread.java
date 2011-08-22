package spade.time;

/**
* A thread used for animation.
*/
public class TimeThread extends Thread {
	public static long minDelay = 80L;
	protected AnimationController animator = null;
	private boolean mustStop = false, running = false;

	public TimeThread(AnimationController aCont) {
		animator = aCont;
	}

	public boolean isRunning() {
		return running;
	}

	public void finish() {
		synchronized (this) {
			mustStop = true;
		}
	}

	@Override
	public void run() {
		if (mustStop)
			return;
		synchronized (this) {
			running = true;
		}
		long start = animator.getCurrentPosition(), len = animator.getIntervalLength();
		int step = animator.getStep();
		for (long i = start; i < len && !mustStop; i += step) {
			long t1 = System.currentTimeMillis();
			animator.moveForth(step);
			if (mustStop) {
				break;
			}
			long t = System.currentTimeMillis() - t1;
			long delay = animator.getDelay();
			if (delay < minDelay) {
				delay = minDelay;
			}
			delay -= t;
			if (mustStop) {
				break;
			}
			if (delay > 0) {
				try {
					sleep(delay);
				} catch (InterruptedException ie) {
				}
			}
			;
		}
		animator.finish();
		synchronized (this) {
			running = false;
		}
	}
}