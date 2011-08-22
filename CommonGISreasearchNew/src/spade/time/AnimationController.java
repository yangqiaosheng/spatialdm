package spade.time;

/**
* Defines the methods allowing to run animations using the TimeThread
*/
public interface AnimationController {
	/**
	* Reports whether the animation interval is valid
	*/
	public boolean hasValidInterval();

	/**
	* Returns the interval of animation (i.e. the time span fow which there are
	* data available)
	*/
	public FocusInterval getInterval();

	/**
	* Returns the length of the whole interval of animation (the units depend on
	* the time scale, these may be days, seconds, or whatever)
	*/
	public long getIntervalLength();

	/**
	* Returns the current position (moment) within the animation interval,
	* in relative units, depending on the current time scale.
	*/
	public long getCurrentPosition();

	/**
	* Returns the animation step (i.e. by what amount of time units to move
	* on each tick)
	*/
	public int getStep();

	/**
	* Sets the animation step (i.e. by what amount of time units to move
	* on each tick)
	*/
	public void setStep(int step);

	/**
	* Returns the animation delay (i.e. how long to wait before changing the
	* picture)
	*/
	public long getDelay();

	/**
	* Sets the animation delay (i.e. how long to wait before changing the
	* picture)
	*/
	public void setDelay(long delay);

	/**
	* Runs the animation
	*/
	public void run();

	/**
	* Informs if the animation is currently running
	*/
	public boolean isRunning();

	/**
	* Plays the next step of the animation, i.e. moves the current moment forward
	* by the "animation step" (@see getStep()).
	*/
	public void stepForth();

	/**
	* Moves the current moment forward by the specified number of time units.
	*/
	public void moveForth(int nUnits);

	/**
	* Returns to the previous step of the animation, i.e. moves the current moment
	* backward by the "animation step" (@see getStep()).
	*/
	public void stepBack();

	/**
	* Notifies the AnimationController that the animation is over (the end date
	* reached).
	*/
	public void finish();

	/**
	* Stops the animation irrespective of the current display moment.
	*/
	public void stop();

	/**
	* Returns to the beginning of the animation interval.
	*/
	public void reset();
}