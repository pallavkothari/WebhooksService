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
 class Webhook {

    private URL callback;
    private String payload;
    private long scheduledTime;   // millis since epoch
    private boolean isRecurring;
    private long delay;
    private TimeUnit timeUnit;
    private int numRecurrences;

    public Webhook() {
        // defaults for deserialization
        this.numRecurrences = 1;
        this.scheduledTime = System.currentTimeMillis();
    }

    /**
     * use this for one-off webhooks (no recurrence)
     */
    public Webhook(URL callback, String payload, long scheduledTime) {
        this(callback, payload, scheduledTime, false, 0, null, 1);
    }

    /**
     * use this for recurring webhooks
     */
    public Webhook(URL callback, String payload, long delay, TimeUnit timeUnit) {
        this(callback, payload, System.currentTimeMillis(), true, delay, timeUnit, 1);
    }

    public Webhook(URL callback, String payload, long delay, TimeUnit timeUnit, int numRecurrences) {
        this(callback, payload, System.currentTimeMillis(), true, delay, timeUnit, numRecurrences);
    }

    private Webhook(URL callback, String payload, long scheduledTime, boolean isRecurring, long delay, TimeUnit timeUnit, int numRecurrences) {
        this.callback = callback;
        this.payload = payload;
        this.scheduledTime = scheduledTime;
        this.isRecurring = isRecurring;
        this.delay = delay;
        this.timeUnit = timeUnit;
        this.numRecurrences = numRecurrences;
    }

    public URL getUrl() {
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

    public Webhook next() {
        if (!isRecurring()) return this;
        return new Webhook(this.callback, this.payload, this.nextScheduledTime(), true, this.delay, this.timeUnit, this.nextNumRecurrences());
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
        Webhook that = (Webhook) o;
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
