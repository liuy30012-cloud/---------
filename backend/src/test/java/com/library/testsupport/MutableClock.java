package com.library.testsupport;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Objects;

public class MutableClock extends Clock {

    private Instant currentInstant;
    private final ZoneId zoneId;

    public MutableClock(Instant currentInstant, ZoneId zoneId) {
        this.currentInstant = Objects.requireNonNull(currentInstant, "currentInstant");
        this.zoneId = Objects.requireNonNull(zoneId, "zoneId");
    }

    @Override
    public ZoneId getZone() {
        return zoneId;
    }

    @Override
    public Clock withZone(ZoneId zone) {
        return new MutableClock(currentInstant, zone);
    }

    @Override
    public Instant instant() {
        return currentInstant;
    }

    public void advance(Duration duration) {
        currentInstant = currentInstant.plus(duration);
    }
}
