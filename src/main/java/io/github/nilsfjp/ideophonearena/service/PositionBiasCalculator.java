package io.github.nilsfjp.ideophonearena.service;

import org.springframework.stereotype.Component;

// Signal-detection statistics for the position-bias fairness check, isolated
// from the aggregation so the math is unit-testable without a database or the
// shuffle replay.
@Component
public class PositionBiasCalculator {

    // The two SDT measures; each is null when the inputs cannot support an
    // estimate (an empty stimulus class).
    public static final class SignalDetection {

        private final Double dPrime;
        private final Double criterion;

        public SignalDetection(Double dPrime, Double criterion) {
            this.dPrime = dPrime;
            this.criterion = criterion;
        }

        public Double getDPrime() {
            return dPrime;
        }

        public Double getCriterion() {
            return criterion;
        }
    }

    // d' (sensitivity) and criterion (response bias) for a yes/no detection
    // task. Signal trials are those where the target sat on the left (so
    // respond-left == correct) and `hits` is how many of those were answered
    // left; noise trials are target-on-right and `falseAlarms` is how many were
    // still answered left. The log-linear correction (Hautus 1995: +0.5 to each
    // response cell, +1 to each stimulus total) keeps the rates strictly inside
    // (0,1) so z never diverges. Returns nulls when either stimulus class is
    // empty -- there is nothing to estimate from.
    public SignalDetection signalDetection(long hits, long signalTrials, long falseAlarms, long noiseTrials) {
        if (signalTrials <= 0 || noiseTrials <= 0) {
            return new SignalDetection(null, null);
        }
        double hitRate = (hits + 0.5) / (signalTrials + 1.0);
        double falseAlarmRate = (falseAlarms + 0.5) / (noiseTrials + 1.0);
        double zHit = probit(hitRate);
        double zFalseAlarm = probit(falseAlarmRate);
        double dPrime = zHit - zFalseAlarm;
        double criterion = -0.5 * (zHit + zFalseAlarm);
        return new SignalDetection(dPrime, criterion);
    }

    // Inverse standard-normal CDF (probit) via Peter Acklam's rational
    // approximation -- relative error < 1.15e-9 over (0,1), no external
    // dependency. Precondition: 0 < p < 1 (guaranteed by the log-linear
    // correction above).
    double probit(double p) {
        if (p <= 0.0 || p >= 1.0) {
            throw new IllegalArgumentException("probit requires 0 < p < 1, got " + p);
        }
        final double[] a = {
            -3.969683028665376e+01, 2.209460984245205e+02, -2.759285104469687e+02,
            1.383577518672690e+02, -3.066479806614716e+01, 2.506628277459239e+00
        };
        final double[] b = {
            -5.447609879822406e+01, 1.615858368580409e+02, -1.556989798598866e+02,
            6.680131188771972e+01, -1.328068155288572e+01
        };
        final double[] c = {
            -7.784894002430293e-03, -3.223964580411365e-01, -2.400758277161838e+00,
            -2.549732539343734e+00, 4.374664141464968e+00, 2.938163982698783e+00
        };
        final double[] d = {
            7.784695709041462e-03, 3.224671290700398e-01, 2.445134137142996e+00,
            3.754408661907416e+00
        };
        final double pLow = 0.02425;
        final double pHigh = 1.0 - pLow;
        if (p < pLow) {
            double q = Math.sqrt(-2.0 * Math.log(p));
            return (((((c[0] * q + c[1]) * q + c[2]) * q + c[3]) * q + c[4]) * q + c[5])
                    / ((((d[0] * q + d[1]) * q + d[2]) * q + d[3]) * q + 1.0);
        }
        if (p <= pHigh) {
            double q = p - 0.5;
            double r = q * q;
            return (((((a[0] * r + a[1]) * r + a[2]) * r + a[3]) * r + a[4]) * r + a[5]) * q
                    / (((((b[0] * r + b[1]) * r + b[2]) * r + b[3]) * r + b[4]) * r + 1.0);
        }
        double q = Math.sqrt(-2.0 * Math.log(1.0 - p));
        return -(((((c[0] * q + c[1]) * q + c[2]) * q + c[3]) * q + c[4]) * q + c[5])
                / ((((d[0] * q + d[1]) * q + d[2]) * q + d[3]) * q + 1.0);
    }
}
