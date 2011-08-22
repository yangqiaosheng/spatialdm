package spade.time;

/**
* A non-interface component providing methods for running animation using a
* given FocusInterval.
*/
public class Animator implements AnimationController {
	/**
	* The focus time interval manipulated by means of animation
	*/
	protected FocusInterval focusInt = null;
	/**
	* The animation step (i.e. by what amount of time units to move on each tick)
	*/
	protected int step = 1;
	/**
	* The animation delay (i.e. how long to wait before changing the
	* picture)
	*/
	protected long delay = 0L;
	/**
	* The thread used for animation
	*/
	protected TimeThread tThread = null;
	/**
	 * A reference to an animator controlled by this animator.
	 * An animator may control another animator when the user wishes two
	 * animations to be synchronised.
	 */
	protected Animator slaveAnimator = null;

	/**
	* Sets the focus time interval to be manipulated by means of animation
	*/
	public void setFocusInterval(FocusInterval fint) {
		focusInt = fint;
	}

	public FocusInterval getFocusInterval() {
		return focusInt;
	}

	/**
	* Reports whether the animation interval is valid
	*/
	@Override
	public boolean hasValidInterval() {
		return focusInt != null && focusInt.hasValidDataInterval();
	}

	/**
	* Returns the interval of animation (i.e. the time span fow which there are
	* data available)
	*/
	@Override
	public FocusInterval getInterval() {
		return focusInt;
	}

	/**
	* Returns the length of the whole interval of animation (the units depend on
	* the time scale, these may be days, seconds, or whatever)
	*/
	@Override
	public long getIntervalLength() {
		if (focusInt == null)
			return 0l;
		return focusInt.getDataIntervalLength();
	}

	/**
	* Returns the current position (moment) within the animation interval,
	* in relative units, depending on the current time scale.
	*/
	@Override
	public long getCurrentPosition() {
		if (focusInt == null)
			return 0l;
		if (focusInt.getWhatIsFixed() == FocusInterval.END)
			return focusInt.getCurrStartPos();
		return focusInt.getCurrEndPos();
	}

	/**
	* Returns the animation step (i.e. by what amount of time units to move
	* on each tick)
	*/
	@Override
	public int getStep() {
		return step;
	}

	/**
	* Sets the animation step (i.e. by what amount of time units to move
	* on each tick)
	*/
	@Override
	public void setStep(int step) {
		if (step > 0 && step < getIntervalLength()) {
			this.step = step;
		}
	}

	/**
	* Returns the animation delay (i.e. how long to wait before changing the
	* picture)
	*/
	@Override
	public long getDelay() {
		return delay;
	}

	/**
	* Sets the animation delay (i.e. how long to wait before changing the
	* picture)
	*/
	@Override
	public void setDelay(long delay) {
		if (delay >= 0) {
			this.delay = delay;
		}
	}

	/**
	 * Sets a reference to another animator, which will be controlled by this
	 * animator. An animator may control another animator when the user wishes two
	 * animations to be synchronised.
	 */
	public void setSlaveAnimator(Animator anim) {
		slaveAnimator = anim;
		if (slaveAnimator != null && focusInt != null) {
			FocusInterval fint = slaveAnimator.getFocusInterval();
			if (fint != null) {
				fint.setPrecision(focusInt.getPrecision());
				slaveAnimator.setStep(step);
				slaveAnimator.setDelay(delay);
				controlSlave();
			} else {
				slaveAnimator = null;
			}
		}
	}

	/**
	* Runs the animation
	*/
	@Override
	public void run() {
		if (hasValidInterval() && tThread == null) {
			if (getCurrentPosition() + step >= focusInt.getDataIntervalLength()) {
				reset();
			}
			tThread = new TimeThread(this);
			tThread.start();
			focusInt.notifyAnimationStart();
			if (slaveAnimator != null) {
				slaveAnimator.getFocusInterval().notifyAnimationStart();
			}
		}
	}

	/**
	* Informs if the animation is currently running
	*/
	@Override
	public boolean isRunning() {
		return tThread != null && tThread.isRunning();
	}

	/**
	* Plays the next step of the animation, i.e. moves the current moment forward
	* by the "animation step" (@see getStep()).
	*/
	@Override
	public void stepForth() {
		moveForth(step);
	}

	private void controlSlave() {
		if (slaveAnimator != null) {
			TimeMoment t1 = focusInt.getCurrIntervalStart(), t2 = focusInt.getCurrIntervalEnd();
			slaveAnimator.getFocusInterval().setCurrInterval(t1.getCopy(), t2.getCopy());
		}
	}

	/**
	* Moves the current moment forward by the specified number of time units.
	*/
	@Override
	public void moveForth(int nUnits) {
		if (!hasValidInterval())
			return;
		switch (focusInt.getWhatIsFixed()) {
		case FocusInterval.START:
			if (!focusInt.moveEndBy(nUnits)) {
				stop();
			}
			break;
		case FocusInterval.END:
			if (!focusInt.moveStartBy(nUnits)) {
				stop();
			}
			break;
		case FocusInterval.LENGTH:
			if (!focusInt.moveIntervalBy(nUnits)) {
				stop();
			}
			break;
		}
		controlSlave();
	}

	/**
	* Returns to the previous step of the animation, i.e. moves the current moment
	* backward by the "animation step" (@see getStep()).
	*/
	@Override
	public void stepBack() {
		if (!hasValidInterval())
			return;
		switch (focusInt.getWhatIsFixed()) {
		case FocusInterval.START:
			if (!focusInt.moveEndBy(-step)) {
				stop();
			}
			break;
		case FocusInterval.END:
			if (!focusInt.moveStartBy(-step)) {
				stop();
			}
			break;
		case FocusInterval.LENGTH:
			if (!focusInt.moveIntervalBy(-step)) {
				stop();
			}
			break;
		}
		controlSlave();
	}

	/**
	* Notifies the AnimationController that the animation is over (the end date
	* reached).
	*/
	@Override
	public void finish() {
		if (tThread != null) {
			tThread = null;
		}
		focusInt.notifyAnimationStop();
		if (slaveAnimator != null) {
			slaveAnimator.getFocusInterval().notifyAnimationStop();
		}
	}

	/**
	* Stops the animation irrespective of the current display moment.
	*/
	@Override
	public void stop() {
		if (tThread != null) {
			tThread.finish();
			tThread = null;
		}
		focusInt.notifyAnimationStop();
		if (slaveAnimator != null) {
			slaveAnimator.getFocusInterval().notifyAnimationStop();
		}
	}

	/**
	* Returns to the beginning of the animation interval.
	*/
	@Override
	public void reset() {
		if (!hasValidInterval())
			return;
		TimeMoment t1 = (focusInt.getWhatIsFixed() == FocusInterval.START) ? focusInt.getCurrIntervalStart() : focusInt.getDataIntervalStart(), t2 = (focusInt.getWhatIsFixed() == FocusInterval.END) ? focusInt.getCurrIntervalEnd() : t1.getCopy();
		if (focusInt.getWhatIsFixed() == FocusInterval.LENGTH) {
			t2.add((int) focusInt.getCurrIntervalLength());
		}
		focusInt.setCurrInterval(t1, t2);
		controlSlave();
	}
}