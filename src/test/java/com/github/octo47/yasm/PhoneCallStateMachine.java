package com.github.octo47.yasm;

import com.google.common.base.Preconditions;

import java.time.Clock;
import java.time.Duration;

public class PhoneCallStateMachine extends StateMachine<Phone.State, Phone> {

    public PhoneCallStateMachine(final Clock clock) {
        this(clock, Duration.ofMillis(Long.MAX_VALUE));
    }

    PhoneCallStateMachine(final Clock clock, final Duration maxCall) {
        super("PhoneCall", Phone::getState, Phone::withState, Phone::toString);

        fromState(Phone.State.OffHook)
                .onEntry(phone -> phone.withCallingNumber(null).accountAccumulated(clock.instant()))
                .permit(Phone.CallDialed.class, Phone.State.Ringing, (a, phone) -> phone.withCallingNumber(a.getNumber()));

        fromState(Phone.State.Ringing)
                .permit(Phone.HungUp.class, Phone.State.OffHook, (hungUp, phone) -> phone.withCallingNumber(null))
                .permit(Phone.CallConnected.class, Phone.State.Connected);

        fromState(Phone.State.Connected)
                .onEntry(phone -> {
                    Preconditions.checkArgument(phone.getStarted() == null, "No call should be active");
                    return phone.withStarted(clock.instant());
                })
                .onExit(phone -> {
                    Preconditions.checkArgument(phone.getStarted() != null, "No call should be active");
                    return phone.accountAccumulated(clock.instant());
                })
                .permitIf(AnyAction.class, Phone.State.OffHook, isCallTooLong(clock, maxCall))
                .permit(Phone.LeftMessage.class, Phone.State.OffHook, (leftMessage, phone) -> phone.withLeftMessage(leftMessage.getLeftMessage()))
                .permit(Phone.HungUp.class, Phone.State.OffHook);
    }

    private Guard<Phone> isCallTooLong(Clock clock, Duration maxCall) {
        return (phone, feedback) -> phone.getStarted() != null
                && Duration.between(phone.getStarted(), clock.instant()).compareTo(maxCall) > 0;
    }
}
