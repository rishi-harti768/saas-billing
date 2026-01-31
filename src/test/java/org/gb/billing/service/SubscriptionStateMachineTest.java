package org.gb.billing.service;

import org.gb.billing.entity.Subscription;
import org.gb.billing.entity.SubscriptionState;
import org.gb.billing.entity.SubscriptionTransitionLog;
import org.gb.billing.exception.InvalidStateTransitionException;
import org.gb.billing.repository.SubscriptionTransitionLogRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class SubscriptionStateMachineTest {

    @Mock
    private SubscriptionTransitionLogRepository transitionLogRepository;

    private SubscriptionStateMachine stateMachine;

    private Subscription subscription;
    private Long userId;

    @BeforeEach
    void setUp() {
        userId = 1L;
        subscription = new Subscription();
        subscription.setId(UUID.randomUUID());
        subscription.setStatus(SubscriptionState.ACTIVE);
        stateMachine = new SubscriptionStateMachine(transitionLogRepository);
    }

    @Test
    void shouldValidateAllowedTransitions() {
        // ACTIVE -> PAST_DUE
        stateMachine.validateTransition(SubscriptionState.ACTIVE, SubscriptionState.PAST_DUE);
        
        // ACTIVE -> CANCELED
        stateMachine.validateTransition(SubscriptionState.ACTIVE, SubscriptionState.CANCELED);
        
        // PAST_DUE -> ACTIVE
        stateMachine.validateTransition(SubscriptionState.PAST_DUE, SubscriptionState.ACTIVE);
        
        // PAST_DUE -> CANCELED
        stateMachine.validateTransition(SubscriptionState.PAST_DUE, SubscriptionState.CANCELED);
    }

    @Test
    void shouldThrowExceptionForInvalidTransition() {
        // ACTIVE -> ACTIVE (Self transition not explicitly in map, handled by logic?)
        // The map defines allowed target states. EnumSet.of(PAST_DUE, CANCELED) for ACTIVE.
        // So ACTIVE -> ACTIVE should fail.
        assertThatThrownBy(() -> stateMachine.validateTransition(SubscriptionState.ACTIVE, SubscriptionState.ACTIVE))
                .isInstanceOf(InvalidStateTransitionException.class);

        // CANCELED -> ACTIVE (Terminal state)
        assertThatThrownBy(() -> stateMachine.validateTransition(SubscriptionState.CANCELED, SubscriptionState.ACTIVE))
                .isInstanceOf(InvalidStateTransitionException.class);
    }

    @Test
    void shouldTransitionToNewStateAndLog() {
        // Given
        subscription.setStatus(SubscriptionState.ACTIVE);
        String reason = "Payment failed";

        // When
        stateMachine.transitionTo(subscription, SubscriptionState.PAST_DUE, reason, userId);

        // Then
        assertThat(subscription.getStatus()).isEqualTo(SubscriptionState.PAST_DUE);
        verify(transitionLogRepository).save(any(SubscriptionTransitionLog.class));
    }

    @Test
    void shouldCheckTerminalState() {
        assertThat(stateMachine.isTerminalState(SubscriptionState.CANCELED)).isTrue();
        assertThat(stateMachine.isTerminalState(SubscriptionState.ACTIVE)).isFalse();
        assertThat(stateMachine.isTerminalState(SubscriptionState.PAST_DUE)).isFalse();
    }
    
    @Test
    void shouldGetAllowedTransitions() {
        Set<SubscriptionState> transitions = stateMachine.getAllowedTransitions(SubscriptionState.ACTIVE);
        assertThat(transitions).containsExactlyInAnyOrder(SubscriptionState.PAST_DUE, SubscriptionState.CANCELED);
    }
}
