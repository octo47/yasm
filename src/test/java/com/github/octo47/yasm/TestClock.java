package com.github.octo47.yasm;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

public class TestClock extends Clock {

    private final ZoneId zoneId;
    private final AtomicLong currentTimeMillis;

    public TestClock() {
        this(new AtomicLong(System.currentTimeMillis()), ZoneOffset.UTC);
    }

    public TestClock(long currentTimeMillis, ZoneId zoneId) {
        this(new AtomicLong(currentTimeMillis), zoneId);
    }

    public TestClock(AtomicLong currentTimeMillis, ZoneId zoneId) {
        this.zoneId = zoneId;
        this.currentTimeMillis = currentTimeMillis;
    }

    public TestClock(final long currentTime) {
        this(currentTime, ZoneOffset.UTC);
    }


    public TestClock(final Instant instant) {
        this(instant.toEpochMilli(), ZoneOffset.UTC);
    }

    public void setCurrentTime(long newTime, TimeUnit unit) {
        this.currentTimeMillis.set(unit.toMillis(newTime));
    }

    public long increment(Duration duration) {
        return this.currentTimeMillis.addAndGet(duration.toMillis());
    }

    @Override
    public ZoneId getZone() {
        return zoneId;
    }

    @Override
    public Clock withZone(ZoneId zone) {
        // will share same time source
        return new TestClock(currentTimeMillis, zone);
    }

    @Override
    public Instant instant() {
        return Instant.ofEpochMilli(currentTimeMillis.get());
    }

    @Override
    public String toString() {
        return "TestClock{" +
                       "zoneId=" + zoneId +
                       ", currentTimeMillis=" + currentTimeMillis +
                       '}';
    }
}
