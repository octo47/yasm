package com.github.octo47.yasm;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.BiFunction;
import java.util.function.BiPredicate;
import java.util.function.Function;
import java.util.function.Predicate;

public abstract class StateMachine<S extends Enum<S>, O> {

    interface GuardFeedback {
        // report guardian decision. will return outcome for easy chaining
        boolean addResolution(String string, boolean outcome);
    }

    interface Action {
    }

    public static class AnyAction implements Action {

        final Action inner;

        public AnyAction(Action inner) {
            this.inner = inner;
        }

        @Override
        public String toString() {
            return "AnyAction{}";
        }
    }

    public static class LoopAction implements Action {
        @Override
        public String toString() {
            return "LoopAction{}";
        }
    }

    public static final Class<AnyAction> ANY = AnyAction.class;

    public static class SimpleAction implements Action {
        @Override
        public String toString() {
            return this.getClass().getSimpleName();
        }
    }

    interface Guard<S> extends BiPredicate<S, GuardFeedback> {
    }

    private final Logger logger = LoggerFactory.getLogger(StateMachine.class);
    private final Map<S, Set<Class<? extends Action>>> stateActions;
    private final Map<S, List<Function<O, O>>> stateEntry;
    private final Map<S, List<Function<O, O>>> stateExit;
    private final Map<Pair<S, Class<? extends Action>>, List<Transition<? extends Action>>> transitions;
    private final Function<O, S> stateExtractor;
    private final BiFunction<O, S, O> stateApplier;
    private final Function<O, String> idProvider;
    private final String machineName;

    class Transition<A extends Action> {
        final Class<A> actionClass;
        final S from;
        final S to;
        final Guard<O> guard;
        final BiFunction<A, O, O> actionCallback;

        Transition(Class<A> actionClass, S from, S to, Guard<O> guard, BiFunction<A, O, O> actionCallback) {
            this.actionClass = actionClass;
            this.from = from;
            this.to = to;
            this.guard = guard;
            this.actionCallback = actionCallback;
        }

        @SuppressWarnings("unchecked")
        O onAction(Action action, O from) {
            Preconditions.checkArgument(actionClass.isAssignableFrom(action.getClass()),
                                        "Expected same action class as per creation:", actionClass, action.getClass());
            return ((BiFunction<Action, O, O>) actionCallback).apply(action, from);
        }
    }

    StateBuilder fromState(S from) {
        return new StateBuilder(from);
    }

    public class StateBuilder {
        private final S from;

        private StateBuilder(S from) {
            this.from = from;
        }

        public StateBuilder onEntry(Function<O, O> entryTransformer) {
            stateEntry.computeIfAbsent(from, s -> Lists.newArrayList()).add(entryTransformer);
            return this;
        }

        public StateBuilder onExit(Function<O, O> exitTransformer) {
            stateExit.computeIfAbsent(from, s -> Lists.newArrayList()).add(exitTransformer);
            return this;
        }

        public <A extends Action> StateBuilder permitIf(Class<A> action, S to, Guard<O> guard, BiFunction<A, O, O> objectTransformer) {
            StateMachine.this.addTransition(action, from, to, guard, objectTransformer);
            return this;
        }

        public <A extends Action> StateBuilder permit(Class<A> action, S to, BiFunction<A, O, O> objectTransformer) {
            StateMachine.this.addTransition(action, from, to, StateMachine.this::permitAlways, objectTransformer);
            return this;
        }

        public <A extends Action> StateBuilder permitIf(Class<A> action, S to, Guard<O> guard) {
            StateMachine.this.addTransition(action, from, to, guard, StateMachine.this::identityTransformer);
            return this;
        }

        public <A extends Action> StateBuilder permit(Class<A> action, S to) {
            StateMachine.this.addTransition(action, from, to, StateMachine.this::permitAlways, StateMachine.this::identityTransformer);
            return this;
        }
    }

    private <A extends Action> O identityTransformer(A action, O object) {
        return object;
    }

    StateMachine(String machineName, Function<O, S> stateExtractor, BiFunction<O, S, O> stateApplier, Function<O, String> idProvider) {
        this.machineName = machineName;
        this.stateExtractor = stateExtractor;
        this.stateApplier = stateApplier;
        this.idProvider = idProvider;
        this.transitions = Maps.newHashMap();
        this.stateActions = Maps.newHashMap();
        this.stateEntry = Maps.newHashMap();
        this.stateExit = Maps.newHashMap();
    }

    private <A extends Action> void addTransition(Class<A> eventClass, S from, S to, Guard<O> guard, BiFunction<A, O, O> actionCallback) {
        final List<Transition<? extends Action>> transitions =
                this.transitions.computeIfAbsent(Pair.of(from, eventClass), k -> Lists.newArrayList());
        transitions.add(new Transition<>(eventClass, from, to, guard, actionCallback));
        this.stateActions.computeIfAbsent(from, f -> Sets.newHashSet()).add(eventClass);
    }

    private boolean permitAlways(O object, GuardFeedback feedback) {
        return true;
    }

    protected Guard<O> withMessage(String message, Predicate<O> simpleGuard) {
        return (s, feedback) -> feedback.addResolution(message, simpleGuard.test(s));
    }

    public void validate() {
        // TDB
    }

    public String getMachineName() {
        return machineName;
    }

    public Optional<O> loop(O inputObject) {
        return transition(inputObject, new LoopAction());
    }

    public Optional<O> transition(O inputObject, Action action) {
        final UUID transitionId = UUID.randomUUID();
        final S fromState = stateExtractor.apply(inputObject);
        logger.info("{}:{}:START stateObject={} fromState={} action={}", machineName, transitionId, idProvider.apply(inputObject), fromState, action.toString());
        List<Transition<? extends Action>> anyActionTransitions = this.transitions
                                                                          .getOrDefault(Pair.of(fromState, AnyAction.class), ImmutableList.of());
        final List<Transition<? extends Action>> transitions;
        if (action.getClass() != ANY) {
            transitions = this.transitions
                                  .getOrDefault(Pair.of(fromState, action.getClass()), ImmutableList.of());
        } else {
            transitions = ImmutableList.of();
        }
        for (Transition<? extends Action> transition : Iterables.concat(anyActionTransitions, transitions)) {
            final GuardFeedback guardFeedback = (string, outcome) -> {
                logger.debug("{}:{} stateObject={} fromState={} toState={} action={}: guard feedback: {} -> {}",
                             machineName, transitionId, idProvider.apply(inputObject),fromState, transition.to, action.toString(), string, outcome ? "ACCEPTED" : "REJECT");
                return outcome;
            };
            if (transition.guard.test(inputObject, guardFeedback)) {
                final O outputObject;
                if (!inputObject.equals(transition.to)) {
                    outputObject = handleStateTransition(inputObject, transition.from, transition.to);
                } else {
                    outputObject = inputObject;
                }

                final Action wrappedAction;
                if (transition.actionClass == AnyAction.class) {
                    wrappedAction = new AnyAction(action);
                } else {
                    wrappedAction = action;
                }
                final O result = transition.onAction(wrappedAction, outputObject);
                logger.info("{}:{}:END stateObject={} fromState={} toState={} action={}: transition complete: {}",
                            machineName, transitionId, idProvider.apply(inputObject), fromState, transition.to, action.toString(), result);
                return Optional.of(result);
            }
        }
        logger.info("{}:{}:NOTFOUND stateObject={} fromState={} action={}: No state found {}",
                    machineName, transitionId, idProvider.apply(inputObject), fromState, action.toString(), inputObject);
        return Optional.empty();
    }

    private O handleStateTransition(O from, S fromState, S toState) {
        O object = from;
        for (Function<O, O> transformer : stateExit.getOrDefault(fromState, ImmutableList.of())) {
            object = transformer.apply(object);
        }
        for (Function<O, O> transformer : stateEntry.getOrDefault(toState, ImmutableList.of())) {
            object = transformer.apply(object);
        }
        return stateApplier.apply(object, toState);
    }
}
