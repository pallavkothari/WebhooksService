package scheduler;

import com.google.common.base.MoreObjects;

import java.net.URL;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

/**
 * Model for the wire format for this scheduler
 *
 * Created by pallav.kothari on 1/6/17.
 */
 class RedisTrigger {

    private URL callback;
    private String payload;
    private long scheduledTime;   // millis since epoch
    private boolean isRecurring;
    private long delay;
    private TimeUnit timeUnit;
    private int numRecurrences;

    public RedisTrigger() {
        // for deserialization
    }

    /**
     * use this for one-off triggers (no recurrence)
     */
    public RedisTrigger(URL callback, String payload, long scheduledTime) {
        this(callback, payload, scheduledTime, false, 0, null, 1);
    }

    /**
     * use this for recurring triggers
     */
    public RedisTrigger(URL callback, String payload, long initialDelay, long delay, TimeUnit timeUnit) {
        this(callback, payload, System.currentTimeMillis() + TimeUnit.MILLISECONDS.convert(initialDelay, timeUnit),
                true, delay, timeUnit, 1);
    }

    public RedisTrigger(URL callback, String payload, long initialDelay, long delay, TimeUnit timeUnit, int numRecurrences) {
        this(callback, payload, System.currentTimeMillis() + TimeUnit.MILLISECONDS.convert(initialDelay, timeUnit),
                true, delay, timeUnit, numRecurrences);
    }

    private RedisTrigger(URL callback, String payload, long scheduledTime, boolean isRecurring, long delay, TimeUnit timeUnit, int numRecurrences) {
        this.callback = callback;
        this.payload = payload;
        this.scheduledTime = scheduledTime;
        this.isRecurring = isRecurring;
        this.delay = delay;
        this.timeUnit = timeUnit;
        this.numRecurrences = numRecurrences;
    }

    public URL getCallback() {
        return callback;
    }

    public String getPayload() {
        return payload;
    }

    public long getScheduledTime() {
        return scheduledTime;
    }

    public boolean isRecurring() {
        return isRecurring;
    }

    public int getNumRecurrences() {
        return numRecurrences;
    }

    public RedisTrigger next() {
        if (!isRecurring()) return this;
        return new RedisTrigger(this.callback, this.payload, this.nextScheduledTime(), true, this.delay, this.timeUnit, this.nextNumRecurrences());
    }

    private int nextNumRecurrences() {
        return !isRecurring ? this.numRecurrences : Math.max(0, this.numRecurrences - 1);
    }

    long nextScheduledTime() {
        return this.getScheduledTime() + TimeUnit.MILLISECONDS.convert(this.delay, this.timeUnit);
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("callback", callback)
                .add("payload", payload)
                .add("scheduledTime", scheduledTime)
                .add("isRecurring", isRecurring)
                .add("delay", delay)
                .add("timeUnit", timeUnit)
                .add("numRecurrences", numRecurrences)
                .toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        RedisTrigger that = (RedisTrigger) o;
        return scheduledTime == that.scheduledTime &&
                isRecurring == that.isRecurring &&
                delay == that.delay &&
                Objects.equals(callback, that.callback) &&
                Objects.equals(payload, that.payload) &&
                timeUnit == that.timeUnit &&
                numRecurrences == that.numRecurrences;
    }

    @Override
    public int hashCode() {
        return Objects.hash(callback, payload, scheduledTime, isRecurring, delay, timeUnit, numRecurrences);
    }
}
