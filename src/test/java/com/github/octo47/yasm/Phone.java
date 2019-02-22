package com.github.octo47.yasm;


import java.time.Duration;
import java.time.Instant;
import java.util.Objects;

import javax.annotation.Nullable;

final class Phone {

    enum State {
        Ringing, Connected, OffHook
    }

    static class CallDialed implements StateMachine.Action {
        private final String number;

        String getNumber() {
            return number;
        }

        CallDialed(String number) {
            this.number = number;
        }

        @Override
        public String toString() {
            return "CallDialed{" +
                    "number='" + number + '\'' +
                    '}';
        }
    }

    static class CallConnected extends StateMachine.SimpleAction {
    }

    static class LeftMessage implements StateMachine.Action {
        private final String leftMessage;

        LeftMessage(String leftMessage) {
            this.leftMessage = leftMessage;
        }

        String getLeftMessage() {
            return leftMessage;
        }

        @Override
        public String toString() {
            return "LeftMessage{" +
                    "leftMessage='" + leftMessage + '\'' +
                    '}';
        }
    }

    static class HungUp extends StateMachine.SimpleAction {
    }

    private final State state;
    @Nullable
    private final String callingNumber;
    @Nullable
    private final String leftMessage;
    @Nullable
    private final Instant started;
    private final Duration accumulated;

    private Phone(State state, @Nullable String callingNumber, @Nullable String leftMessage, @Nullable Instant started, Duration accumulated) {
        this.state = state;
        this.callingNumber = callingNumber;
        this.leftMessage = leftMessage;
        this.started = started;
        this.accumulated = accumulated;
    }

    Phone withState(State state) {
        return of(state, getCallingNumber(), getLeftMessage(), getStarted(), getAccumulated());
    }

    Phone withCallingNumber(@Nullable String callingNumber) {
        return of(getState(), callingNumber, getLeftMessage(), getStarted(), getAccumulated());
    }

    Phone withLeftMessage(String leftMessage) {
        return of(getState(), getCallingNumber(), leftMessage, getStarted(), getAccumulated());
    }

    Phone withStarted(@Nullable Instant started) {
        return of(getState(), getCallingNumber(), getLeftMessage(), started, getAccumulated());
    }

    Phone withAccumulated(Duration accumulated) {
        return of(getState(), getCallingNumber(), getLeftMessage(), getStarted(), accumulated);
    }

    Phone accountAccumulated(Instant now) {
        if (getStarted() != null) {
            return withStarted(null).withAccumulated(Duration.between(getStarted(), now));
        } else {
            return this;
        }
    }

    static Phone of(State state, @Nullable String callingNumber, @Nullable String leftMessage, @Nullable Instant started, Duration accumulated) {
        return new Phone(state, callingNumber, leftMessage, started, accumulated);
    }

    State getState() {
        return state;
    }

    @Nullable
    String getCallingNumber() {
        return callingNumber;
    }

    @Nullable
    Instant getStarted() {
        return this.started;
    }

    @Nullable
    String getLeftMessage() {
        return leftMessage;
    }

    Duration getAccumulated() {
        return this.accumulated;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Phone phone = (Phone) o;
        return accumulated == phone.accumulated &&
                state == phone.state &&
                Objects.equals(callingNumber, phone.callingNumber) &&
                Objects.equals(leftMessage, phone.leftMessage) &&
                Objects.equals(started, phone.started);
    }

    @Override
    public int hashCode() {
        return Objects.hash(state, callingNumber, leftMessage, started, accumulated);
    }

    @Override
    public String toString() {
        return "Phone{" +
                "state=" + state +
                ", callingNumber='" + callingNumber + '\'' +
                ", leftMessage='" + leftMessage + '\'' +
                ", started=" + started +
                ", accumulated=" + accumulated +
                '}';
    }

}

