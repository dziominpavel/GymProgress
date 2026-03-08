# AI Hypertrophy Training Scoring System (10/10 Pro Coach Model)

Version: 1.0\
Purpose: Professional scoring model for fitness applications.

This document describes a **state‑of‑the‑art training quality
algorithm** designed for fitness applications that evaluate workouts,
track hypertrophy stimulus, and provide intelligent progression
recommendations.

The system is designed to mimic how experienced strength coaches
evaluate training.

Core principle:

Training Adaptation = Stimulus -- Fatigue

However for scoring purposes we model:

TrainingQuality = Stimulus / (1 + Fatigue)

This produces stable numeric scores that can be compared across
sessions.

------------------------------------------------------------------------

# 1. System Goals

The algorithm should:

• Identify whether a workout is **better, worse, or similar** to
previous sessions\
• Quantify **hypertrophy stimulus**\
• Estimate **fatigue cost**\
• Detect **progress or regression**\
• Provide **explanations for changes**\
• Work with **minimal data (weight + reps)**\
• Scale to advanced metrics later

The algorithm must work even without:

• RIR\
• RPE\
• Tempo\
• Rest times

But support them in future versions.

------------------------------------------------------------------------

# 2. Data Model

WorkoutEntry

    exerciseName
    weight
    reps[]
    date

Example

    weight = 70
    reps = [8,8,8,9]

Derived metrics

    sets = len(reps)
    totalReps = sum(reps)
    volume = weight * totalReps
    avgReps = totalReps / sets
    firstSet = reps[0]
    lastSet = reps[-1]

------------------------------------------------------------------------

# 3. Target Rep Ranges

Different goals have different ideal rep ranges.

Hypertrophy

    ideal: 8–12
    acceptable: 6–15

Strength

    ideal: 3–6
    acceptable: 1–8

Endurance

    ideal: 15–25
    acceptable: 12–30

For MVP assume **hypertrophy**.

------------------------------------------------------------------------

# 4. Mechanical Tension

Mechanical tension is the most important hypertrophy driver.

    tensionScore = weight / bestWeightHistory

bestWeightHistory = maximum weight ever used for this exercise.

Range:

    0.0 – 1.0

------------------------------------------------------------------------

# 5. Effective Repetitions Model

Research shows that **reps close to failure produce most growth**.

We approximate this without RIR.

Formula

    effectiveRepsPerSet = max(0, reps - (targetUpperRange - 5))

For hypertrophy:

    targetUpperRange = 12

Example

  reps   effective reps
  ------ ----------------
  6      0
  8      1
  9      2
  10     3
  11     4
  12     5
  13     6

Total

    effectiveRepsTotal = sum(effectiveRepsPerSet)

Normalization

    effectiveScore =
    effectiveRepsTotal / bestEffectiveRepsHistory

------------------------------------------------------------------------

# 6. Density Score

Training density measures work efficiency.

    density = totalReps / sets

Normalization

    densityScore =
    density / bestDensityHistory

------------------------------------------------------------------------

# 7. Rep Quality Score

Checks if sets are in hypertrophy range.

For each set

    1.0 → ideal range
    0.7 → acceptable range
    0.3 → outside range

Example

    8–12 = 1.0
    6–7 = 0.7
    13–15 = 0.7
    else = 0.3

Average across sets

    repQuality = average(setScores)

------------------------------------------------------------------------

# 8. Stimulus Score

Hypertrophy stimulus is combination of:

• tension\
• effective reps\
• density\
• rep quality

Formula

    stimulusScore =

    0.40 * tensionScore +
    0.35 * effectiveScore +
    0.15 * densityScore +
    0.10 * repQuality

Range:

    0 – 1

------------------------------------------------------------------------

# 9. Fatigue Model

Fatigue represents the **recovery cost of training**.

Fatigue has four components.

1.  Volume fatigue\
2.  Set fatigue\
3.  Rep drop fatigue\
4.  Intensity fatigue

------------------------------------------------------------------------

# 10. Volume Fatigue

    volumeFatigue = volume / bestVolumeHistory

Volume strongly affects recovery demand.

------------------------------------------------------------------------

# 11. Set Fatigue

Large number of sets increases fatigue non‑linearly.

    setFatigue = (sets / 8)^1.3

Example

  sets   fatigue
  ------ ---------
  3      0.25
  4      0.35
  5      0.48
  6      0.62

------------------------------------------------------------------------

# 12. Rep Drop Fatigue

Measures exhaustion across sets.

    dropRate = 1 - lastSet / firstSet

Penalty

    dropRate ≤ 0.20 → 0
    ≤ 0.35 → 0.05
    ≤ 0.50 → 0.10
    > 0.50 → 0.15

------------------------------------------------------------------------

# 13. Intensity Fatigue

Very heavy loads produce systemic fatigue.

    intensityFatigue =
    (weight / bestWeightHistory)^2 * 0.25

------------------------------------------------------------------------

# 14. Total Fatigue Score

    fatigueScore =

    0.40 * volumeFatigue +
    0.25 * setFatigue +
    0.20 * dropFatigue +
    0.15 * intensityFatigue

Range typically

    0 – 1.5

------------------------------------------------------------------------

# 15. Final Training Quality

    trainingQuality =
    stimulusScore / (1 + fatigueScore)

Typical range

    0 – 10

Higher = better stimulus efficiency.

------------------------------------------------------------------------

# 16. Session Score Normalization

Convert to user-friendly scale.

    sessionScore = trainingQuality * 100

Examples

    450 → very strong session
    300 → solid session
    150 → mediocre
    80 → weak session

------------------------------------------------------------------------

# 17. Progress Comparison

Compare current session to previous session.

    delta = currentScore - previousScore

Decision thresholds

    delta ≥ +5% → BETTER
    delta ≤ -5% → WORSE
    else → SAME

------------------------------------------------------------------------

# 18. Percent Change

    deltaPercent =
    ((currentScore - previousScore)
     / previousScore) * 100

Example

    +8.4%
    -3.1%

------------------------------------------------------------------------

# 19. Root Cause Analysis

Determine why score changed.

Compare component deltas

    tensionDelta
    effectiveDelta
    densityDelta
    fatigueDelta

Largest difference defines explanation.

Examples

    "higher mechanical tension"
    "more effective reps"
    "volume increase"
    "excessive fatigue"

------------------------------------------------------------------------

# 20. Weekly Stimulus Tracking (Advanced)

Track stimulus accumulation per muscle group.

    weeklyStimulus =
    sum(stimulusScore per exercise)

Define ranges

    MEV (minimum effective volume)
    MRV (maximum recoverable volume)

Example

    Chest weekly stimulus: 14
    Target range: 12–20
    Status: optimal

------------------------------------------------------------------------

# 21. Exercise Contribution Matrix

Compound exercises stimulate multiple muscles.

Example

Bench Press

    chest = 1.0
    triceps = 0.6
    frontDelts = 0.4

Stimulus distribution

    muscleStimulus =
    exerciseStimulus * contribution

------------------------------------------------------------------------

# 22. Recovery Index

Track accumulated fatigue.

    recoveryIndex =
    weeklyStimulus / fatigueAccumulation

Interpretation

    >1.2 → good recovery
    1.0 → balanced
    <0.8 → overreaching risk

------------------------------------------------------------------------

# 23. Automatic Weight Recommendation

If sessionScore improves:

    increase weight 2–5%

If score stagnates:

    increase reps

If fatigue high:

    reduce sets

------------------------------------------------------------------------

# 24. Future Extensions

The system supports future signals:

• RIR per set\
• Rest time\
• Tempo\
• Velocity tracking\
• HRV recovery\
• Sleep metrics\
• Periodization phases

------------------------------------------------------------------------

# 25. Why This Model Is Strong

Advantages

• Based on hypertrophy science\
• Separates stimulus and fatigue\
• Handles different training styles\
• Works without complex input\
• Scales into AI coaching systems\
• Provides explainable feedback

This architecture is suitable for **modern fitness applications with
intelligent workout analysis and coaching features**.
