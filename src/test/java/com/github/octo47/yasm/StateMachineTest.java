package com.github.octo47.yasm;

import org.junit.Assert;
import org.junit.Test;

import java.time.Duration;
import java.util.Optional;

public class StateMachineTest {

    @Test
    public void testMachine() {

        final TestClock clock = new TestClock();
        final PhoneCallStateMachine stateMachine = new PhoneCallStateMachine(clock, Duration.ofSeconds(Long.MAX_VALUE));
        final Phone initialPhone = Phone.of(Phone.State.OffHook, null, null, null, Duration.ZERO);

        final String number = "333-33-33";
        // OffHook -> Ringing
        final Phone calling = stateMachine.transition(initialPhone, new Phone.CallDialed(number)).orElseThrow(AssertionError::new);
        Assert.assertEquals(Phone.State.Ringing, calling.getState());
        Assert.assertEquals(number, calling.getCallingNumber());
        Assert.assertEquals(Duration.ZERO, calling.getAccumulated());

        // time passing
        final Duration callDuration = Duration.ofSeconds(10);
        clock.increment(callDuration);

        // Ringing -> Connected
        final Phone connected = stateMachine.transition(calling, new Phone.CallConnected()).orElseThrow(AssertionError::new);

        // time passing
        clock.increment(callDuration);

        String message = "hello!";
        // Connected -> LeftMessage
        Phone withMessage = stateMachine.transition(connected, new Phone.LeftMessage(message)).orElseThrow(AssertionError::new);
        Assert.assertEquals(Phone.State.OffHook, withMessage.getState());
        Assert.assertEquals(message, withMessage.getLeftMessage());
        Assert.assertNull(withMessage.getCallingNumber());
        Assert.assertEquals(callDuration, withMessage.getAccumulated());

        Phone hangUp = stateMachine.transition(connected, new Phone.HungUp()).orElseThrow(AssertionError::new);
        Assert.assertEquals(Phone.State.OffHook, hangUp.getState());
        Assert.assertNull(hangUp.getLeftMessage());
        Assert.assertNull(hangUp.getCallingNumber());
        Assert.assertEquals(callDuration, hangUp.getAccumulated());

    }

    @Test
    public void testAnyActions() {

        final TestClock clock = new TestClock();
        final Duration maxCallDuration = Duration.ofHours(1);
        final PhoneCallStateMachine stateMachine = new PhoneCallStateMachine(clock, maxCallDuration);
        final Phone initialPhone = Phone.of(Phone.State.OffHook, null, null, null, Duration.ZERO);

        final String number = "333-33-33";
        final Phone connected = stateMachine.transition(initialPhone, new Phone.CallDialed(number))
                .flatMap(phone -> stateMachine.transition(phone, new Phone.CallConnected()))
                .orElseThrow(AssertionError::new);
        // we apply same action and we shouldn't have any transitions
        Optional<Phone> looped = stateMachine.loop(connected);
        Assert.assertFalse(looped.isPresent());

        // now advance time
        final Duration callDuration = maxCallDuration.plusMinutes(1);
        clock.increment(callDuration);
        Optional<Phone> timedOut = stateMachine.loop(connected);
        Assert.assertTrue(timedOut.isPresent());
        timedOut.ifPresent(phone -> {
            Assert.assertEquals(Phone.State.OffHook, phone.getState());
            Assert.assertEquals(callDuration, phone.getAccumulated());
            Assert.assertNull(phone.getStarted());
        });
    }


    @Test
    public void testFallback() {

        final TestClock clock = new TestClock();
        final PhoneCallStateMachine stateMachine = new PhoneCallStateMachine(clock);
        final Phone initialPhone = Phone.of(Phone.State.OffHook, null, null, null, Duration.ZERO);

        // no such transition as OffHook -> CallConnected
        final Optional<Phone> calling = stateMachine.transition(initialPhone, new Phone.CallConnected());
        Assert.assertFalse(calling.isPresent());
    }

}
