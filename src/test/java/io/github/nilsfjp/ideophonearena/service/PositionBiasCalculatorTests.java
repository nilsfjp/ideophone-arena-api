package io.github.nilsfjp.ideophonearena.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.nilsfjp.ideophonearena.service.PositionBiasCalculator.SignalDetection;
import org.junit.jupiter.api.Test;

/**
 * Pure math for the position-bias SDT numbers, exercised without a database or
 * the shuffle replay. `probit` is validated against known normal quantiles;
 * `signalDetection` is then checked to compose those with the log-linear
 * correction correctly.
 */
class PositionBiasCalculatorTests {

    private final PositionBiasCalculator calculator = new PositionBiasCalculator();

    @Test
    void probitMatchesKnownNormalQuantiles() {
        assertEquals(0.0, calculator.probit(0.5), 1e-6);
        // Phi(1) = 0.8413447..., Phi(1.959963...) = 0.975.
        assertEquals(1.0, calculator.probit(0.8413447460685429), 1e-4);
        assertEquals(1.959963984540054, calculator.probit(0.975), 1e-4);
        assertEquals(-1.959963984540054, calculator.probit(0.025), 1e-4);
        assertEquals(-calculator.probit(0.3), calculator.probit(0.7), 1e-9);
    }

    @Test
    void signalDetectionIsZeroWhenResponsesAreUnbiasedAndUninformative() {
        // Equal hit and false-alarm rates -> no sensitivity and no bias.
        SignalDetection sdt = calculator.signalDetection(50, 100, 50, 100);
        assertEquals(0.0, sdt.getDPrime(), 1e-9);
        assertEquals(0.0, sdt.getCriterion(), 1e-9);
    }

    @Test
    void signalDetectionComposesProbitWithLogLinearCorrection() {
        long hits = 95;
        long signalTrials = 100;
        long falseAlarms = 50;
        long noiseTrials = 100;
        double hitRate = (hits + 0.5) / (signalTrials + 1.0);
        double falseAlarmRate = (falseAlarms + 0.5) / (noiseTrials + 1.0);
        double expectedDPrime = calculator.probit(hitRate) - calculator.probit(falseAlarmRate);
        double expectedCriterion = -0.5 * (calculator.probit(hitRate) + calculator.probit(falseAlarmRate));

        SignalDetection sdt = calculator.signalDetection(hits, signalTrials, falseAlarms, noiseTrials);
        assertEquals(expectedDPrime, sdt.getDPrime(), 1e-9);
        assertEquals(expectedCriterion, sdt.getCriterion(), 1e-9);
        assertTrue(sdt.getDPrime() > 0.0, "more hits than false alarms -> positive sensitivity");
        assertTrue(sdt.getCriterion() < 0.0, "answering left more often than chance -> liberal criterion");
    }

    @Test
    void signalDetectionIsNullWhenAStimulusClassIsEmpty() {
        assertNull(calculator.signalDetection(0, 0, 0, 0).getDPrime());
        assertNull(calculator.signalDetection(0, 0, 0, 0).getCriterion());
        assertNull(calculator.signalDetection(5, 10, 0, 0).getDPrime(), "no noise trials -> undefined");
        assertNull(calculator.signalDetection(0, 0, 5, 10).getCriterion(), "no signal trials -> undefined");
    }
}
