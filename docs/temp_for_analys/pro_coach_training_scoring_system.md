# PRO COACH TRAINING SCORING SYSTEM (v1.0)

## Goal

This algorithm evaluates the **quality of a training session** and
determines whether a new workout is **better, worse, or similar**
compared to previous sessions.

The model is based on modern hypertrophy science and coaching practice.

Core principle:

Training Quality = Stimulus / Fatigue

------------------------------------------------------------------------

# 1. Input Data

Workout entry fields:

-   exerciseName
-   weight (kg)
-   reps (array of repetitions per set)
-   date

Example:

weight = 70 reps = \[8,8,8,9\]

Derived metrics:

sets = len(reps) totalReps = sum(reps) volume = weight \* totalReps
avgReps = totalReps / sets

------------------------------------------------------------------------

# 2. Hypertrophy Stimulus Model

Muscle growth stimulus depends on three factors:

1.  Mechanical Tension
2.  Effective Repetitions
3.  Training Density

------------------------------------------------------------------------

# 3. Mechanical Tension Score

Heavy loads create higher fiber recruitment.

tensionScore = weight / bestWeightHistory

Where:

bestWeightHistory = max weight previously performed for this exercise.

Range:

0.0 -- 1.0

------------------------------------------------------------------------

# 4. Effective Repetitions

Only repetitions close to failure create maximal hypertrophy stimulus.

Approximation without RIR:

effectiveRepsPerSet = max(0, reps - (targetUpperRange - 5))

For hypertrophy:

targetUpperRange = 12

Example:

reps \| effective reps 6 \| 0 8 \| 1 10 \| 3 12 \| 5 15 \| 8

effectiveRepsTotal = sum(effectiveRepsPerSet)

------------------------------------------------------------------------

# 5. Density Score

More work in fewer sets increases stimulus.

densityScore = totalReps / sets

Normalize:

densityScore = densityScore / bestDensityHistory

------------------------------------------------------------------------

# 6. Stimulus Score

stimulusScore = 0.5 \* tensionScore + 0.35 \* (effectiveRepsTotal /
bestEffectiveRepsHistory) + 0.15 \* densityScore

------------------------------------------------------------------------

# 7. Fatigue Model

Fatigue accumulates from:

1.  Volume
2.  Number of Sets
3.  Rep Drop
4.  Intensity Load

------------------------------------------------------------------------

# 8. Volume Fatigue

volumeFatigue = volume / bestVolumeHistory

------------------------------------------------------------------------

# 9. Set Fatigue

More sets increase fatigue exponentially.

setFatigue = (sets / 8) \^ 1.2

------------------------------------------------------------------------

# 10. Drop Fatigue

Rep drop indicates local exhaustion.

dropRate = 1 - lastSet / firstSet

dropFatigue:

dropRate \<= 0.20 → 0 dropRate \<= 0.35 → 0.05 dropRate \<= 0.50 → 0.10
dropRate \> 0.50 → 0.15

------------------------------------------------------------------------

# 11. Intensity Fatigue

Very heavy loads increase systemic fatigue.

intensityFatigue = (weight / bestWeightHistory) \^ 2 \* 0.2

------------------------------------------------------------------------

# 12. Total Fatigue

fatigueScore =

0.4 \* volumeFatigue + 0.25 \* setFatigue + 0.2 \* dropFatigue + 0.15 \*
intensityFatigue

------------------------------------------------------------------------

# 13. Final Training Quality

trainingQuality = stimulusScore / (1 + fatigueScore)

Range typically:

0 -- 10

------------------------------------------------------------------------

# 14. Progress Comparison

delta = currentQuality - previousQuality

delta \>= 0.05 → BETTER delta \<= -0.05 → WORSE else → SAME

------------------------------------------------------------------------

# 15. Percent Progress

deltaPercent = ((currentQuality - previousQuality) / previousQuality) \*
100

------------------------------------------------------------------------

# 16. Root Cause Analysis

Compare component changes:

stimulusDelta fatigueDelta tensionDelta

Return explanation:

Examples:

"+6% higher tension" "+4% more effective reps" "-5% excessive fatigue"
"+3 sets volume increase"

------------------------------------------------------------------------

# 17. Optional Future Improvements

The algorithm supports additional signals:

RIR per set Tempo Rest time Exercise type (compound/isolation) Muscle
group fatigue Weekly fatigue accumulation Bodyweight normalization

------------------------------------------------------------------------

# 18. Why This Model Is Strong

Advantages:

• Based on hypertrophy science • Separates stimulus and fatigue • Works
without RPE data • Scales with advanced metrics • Suitable for AI
coaching systems • Avoids bias of pure volume models

------------------------------------------------------------------------

# 19. Example

Workout A:

60kg 10 10 10 10

Workout B:

70kg 8 8 8 9

Algorithm evaluates:

-   effective reps
-   tension
-   fatigue

Then returns:

Workout A Quality = 6.25 Workout B Quality = 5.40

Result:

Workout A = BETTER (+15% stimulus efficiency)
