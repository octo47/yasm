# Yet Another State Machine

Simple State machine 'library'. Only single class is all what you might
need for state machine definition.

State machine well explained here: https://en.wikipedia.org/wiki/UML_state_machine

Idea of this implementation is to have 'pure state machine'. It is best used
over some 'immutable' state object that can be copy/mutated with provided handlers.

```
s2 = machine.newState(s1, action)
```

Feature provided by yasm:

* Support for 'pure state machine' implementations
* Actions and States _(*)_
* Enter/Exit transformers
* Declarative transition definitions
* Transition guards
* Stateless and threadsafe _(**)_
* Support for ANY actions _(***)_

(*) _machine uses Action instead of Event as it overloaded term_

(**) _as long as transition functions are thread safe as well

(***) _useful to implement timeouts_ https://github.com/octo47/yasm/blob/master/README.md#any-state

Features not in (yet):
* Verification


## Machine Transitions

To give you some example of how it can be used here a simple class:

```
class Phone {

    enum State {
        Ringing, Connected, OffHook
    }

    static class CallDialed implements StateMachine.Action {
            private final String number;
    }

    private final State state;
    @Nullable
    private final String callingNumber;

    ... rest of the class
}
```

Class is going to be immutable (protobuf generated classes or any IDE plugin for 'immutable objects' or Lombok can be used).
Class can have builders or copying methods

```
Phone withLeftMessage(String leftMessage) {
        return of(getState(), getCallingNumber(), leftMessage, getStarted(), getAccumulated());
}
```

State machine example can be found at: https://github.com/octo47/yasm/blob/master/src/test/java/com/github/octo47/yasm/PhoneCallStateMachine.java

To define machine builder-style calls can be used during StateMachine construction:

```
public PhoneCallStateMachine() {
        fromState(Phone.State.OffHook)
                .onEntry(phone -> phone.withCallingNumber(null).accountAccumulated(clock.instant()))
                .permit(Phone.CallDialed.class, Phone.State.Ringing, (a, phone) -> phone.withCallingNumber(a.getNumber()));

  ...
```

Code defines 'onEntry' method that effectively
erase phone state to not
repeat this each time machine transitioning to OffHook state

As well defines one transition OffHook -> Ringing
triggered by any instance of Phone.CallDialed.class

Last lambda is 'transformer' method that should (by design, but not strictly required) provide new state object.

```
(a, phone) -> phone.withCallingNumber(a.getNumber())
```

_a_ in our case will be an instance of Phone.CallDialed.class and it does have number we are calling

```
static class CallDialed implements StateMachine.Action {
        private final String number;
}
```

Resulting sequence of how this machine can be used illustrated by this test code
https://github.com/octo47/yasm/blob/master/src/test/java/com/github/octo47/yasm/StateMachineTest.java#L12

```
final Phone calling = stateMachine.transition(initialPhone, new Phone.CallDialed(number)).orElseThrow(AssertionError::new);
final Phone connected = stateMachine.transition(calling, new Phone.CallConnected()).orElseThrow(AssertionError::new);
final Phone withMessage = stateMachine.transition(connected, new Phone.LeftMessage(message)).orElseThrow(AssertionError::new);
final Phone hangUp = stateMachine.transition(connected, new Phone.HungUp()).orElseThrow(AssertionError::new);
```

(Optional used to express no state to transition was found)

## ANY State

ANY state can be used to define timeouts. 

Consider following example:
```
 fromState(Phone.State.Connected)
 ...
                .permitIf(AnyAction.class, Phone.State.OffHook, isCallTooLong(clock, maxCall))
 ...
```

For state Connected transition defined as _'Connected -> (ANY) OffHook: if(isCallTooLong)'_

During transition analysis for any action this transition will happen if _isCallTooLong()_ happen to return _true_. To make a bit easier to handle this use case machine provides _loop()_ method that will trigger any 'ANY' transitions if appropriate guard will trigger.

