package com.example

import com.example.data.model.AthleteProfile
import com.example.data.model.DistanceUnit
import com.example.data.model.EquipmentTier
import com.example.data.model.MuscleGroup
import com.example.data.model.PrimaryGoal
import com.example.data.model.TrainingExperience
import com.example.data.model.WorkoutLog
import com.example.data.model.WorkoutType
import com.example.data.model.buildWorkoutCreateMap
import com.example.data.model.deriveRirFromRpe
import com.example.data.science.NaturalHypertrophyScience
import com.example.data.science.VolumeZone
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {

    @Test
    fun rpeToRirMapping_matchesNaturalHypertrophyScience() {
        assertEquals(0, deriveRirFromRpe(10))
        assertEquals(1, deriveRirFromRpe(9))
        assertEquals(2, deriveRirFromRpe(8))
        assertEquals(3, deriveRirFromRpe(7))
    }

    @Test
    fun distanceUnitConversion_handlesKmAndMeters() {
        val swim = WorkoutLog(
            type = WorkoutType.SWIMMING.name,
            distanceValue = 1500.0,
            distanceUnit = DistanceUnit.M.name
        )
        assertEquals(1.5, swim.distanceInKm(), 0.001)
        assertEquals("1500 м", swim.formattedDistance())

        val run = WorkoutLog(
            type = WorkoutType.RUNNING.name,
            distanceValue = 7.25,
            distanceUnit = DistanceUnit.KM.name
        )
        assertEquals(7.25, run.distanceInKm(), 0.001)
        assertEquals("7.25 км", run.formattedDistance())
    }

    @Test
    fun weeklyAnalysis_computesMavVolumeAndSrpeLoad() {
        val now = 1_700_000_000_000L
        val profile = AthleteProfile(
            userId = "u1",
            bodyWeightKg = 80.0,
            trainingExperience = TrainingExperience.INTERMEDIATE.name
        )
        val workouts = listOf(
            WorkoutLog(
                id = "1",
                type = WorkoutType.STRENGTH.name,
                durationMinutes = 60,
                rpe = 8,
                muscleGroup = MuscleGroup.CHEST.name,
                workingSets = 12,
                sessionLoadSrpe = 480,
                performedAtMillis = now - 3600_000L
            ),
            WorkoutLog(
                id = "2",
                type = WorkoutType.SWIMMING.name,
                durationMinutes = 40,
                distanceValue = 1600.0,
                distanceUnit = DistanceUnit.M.name,
                rpe = 6,
                sessionLoadSrpe = 240,
                performedAtMillis = now - 7200_000L
            )
        )

        val summary = NaturalHypertrophyScience.analyzeLastSevenDays(workouts, profile, now)
        assertEquals(2, summary.totalSessions)
        assertEquals(100, summary.totalMinutes)
        assertEquals(1.6, summary.totalDistanceKm, 0.001)
        assertEquals(720, summary.totalSrpeLoad)
        assertEquals(128, summary.dailyProteinGramsMin)
        assertEquals(176, summary.dailyProteinGramsMax)
        assertFalse(summary.interferenceIsHigh)

        val chest = summary.muscleVolumes.first { it.muscleGroup == MuscleGroup.CHEST }
        assertEquals(12, chest.completedSets)
        assertEquals(VolumeZone.MAV_OPTIMAL, chest.zone)
    }

    @Test
    fun createMap_neverIncludesIdField() {
        val map = buildWorkoutCreateMap(
            userId = "u1",
            type = WorkoutType.RUNNING,
            title = "Інтервали",
            durationMinutes = 35,
            distanceValue = 6.0,
            distanceUnit = DistanceUnit.KM,
            rpe = 8,
            muscleGroup = MuscleGroup.NONE,
            workingSets = 0,
            rir = 2,
            notes = "",
            performedAtMillis = 123456L
        )
        assertFalse(map.containsKey("id"))
        assertTrue(map["sessionLoadSrpe"] == 280)
    }

    @Test
    fun dailyChartBuckets_coverSevenDaysAndPreviousWeekComparison() {
        val now = 1_700_000_000_000L
        val oneDay = 24L * 60L * 60L * 1000L
        val profile = AthleteProfile(userId = "u1", bodyWeightKg = 78.0)

        val workouts = listOf(
            WorkoutLog(
                id = "today_run",
                type = WorkoutType.RUNNING.name,
                durationMinutes = 40,
                distanceValue = 6.5,
                distanceUnit = DistanceUnit.KM.name,
                rpe = 7,
                workingSets = 0,
                sessionLoadSrpe = 280,
                performedAtMillis = now - 1_000L
            ),
            WorkoutLog(
                id = "yesterday_strength",
                type = WorkoutType.STRENGTH.name,
                durationMinutes = 55,
                rpe = 8,
                workingSets = 14,
                sessionLoadSrpe = 440,
                performedAtMillis = now - oneDay
            ),
            WorkoutLog(
                id = "prev_week_swim",
                type = WorkoutType.SWIMMING.name,
                durationMinutes = 30,
                distanceValue = 1200.0,
                distanceUnit = DistanceUnit.M.name,
                rpe = 6,
                workingSets = 0,
                sessionLoadSrpe = 180,
                performedAtMillis = now - 9L * oneDay
            )
        )

        val summary = NaturalHypertrophyScience.analyzeLastSevenDays(workouts, profile, now)
        assertEquals(7, summary.dailyPoints.size)
        assertTrue(summary.dailyPoints.last().isToday)
        assertEquals(280, summary.dailyPoints.last().totalSrpe)
        assertEquals(7.0, summary.dailyPoints.last().averageRpe, 0.01)
        assertEquals(440, summary.dailyPoints[5].totalSrpe)
        assertEquals(14, summary.dailyPoints[5].totalSets)
        assertEquals(180, summary.previousWeekSrpe)
        assertEquals(720, summary.totalSrpeLoad)
    }

    @Test
    fun athleteProfileBiometrics_computeBmiTdeeAndTargetCalories() {
        val profile = AthleteProfile(
            bodyWeightKg = 80.0,
            ageYears = 25L,
            heightCm = 180L,
            primaryGoal = PrimaryGoal.HYPERTROPHY.name,
            equipment = EquipmentTier.HOME_GYM.name,
            weeklyTargetSessions = 4L
        )
        // BMI = 80 / (1.8^2) = 24.69...
        assertEquals(24.69, profile.bmi(), 0.05)
        assertEquals("Здорова норма", profile.bmiStatusUk())
        // BMR = 800 + 1125 - 125 + 5 = 1805; TDEE = 1805 * 1.55 = 2798
        assertEquals(2798, profile.estimatedTdeeKcal())
        assertEquals(2798 + 250, profile.recommendedTargetKcal())
    }

    @Test
    fun weeklyAnalysis_detectsDeloadLaggingMuscleAndSplit() {
        val now = 1_700_000_000_000L
        val profile = AthleteProfile(
            userId = "u1",
            trainingExperience = TrainingExperience.BEGINNER.name,
            focusMuscles = listOf(MuscleGroup.BACK.name, MuscleGroup.CHEST.name),
            weeklyTargetSessions = 4L
        )
        // High RPE streak >= 3 across 4 sessions -> triggers deloadRecommended
        val workouts = listOf(
            WorkoutLog(
                id = "w1",
                type = WorkoutType.STRENGTH.name,
                durationMinutes = 60,
                rpe = 9,
                muscleGroup = MuscleGroup.CHEST.name,
                workingSets = 10,
                sessionLoadSrpe = 540,
                performedAtMillis = now - 10_000L
            ),
            WorkoutLog(
                id = "w2",
                type = WorkoutType.STRENGTH.name,
                durationMinutes = 50,
                rpe = 9,
                muscleGroup = MuscleGroup.QUADS.name,
                workingSets = 8,
                sessionLoadSrpe = 450,
                performedAtMillis = now - 20_000L
            ),
            WorkoutLog(
                id = "w3",
                type = WorkoutType.STRENGTH.name,
                durationMinutes = 45,
                rpe = 10,
                muscleGroup = MuscleGroup.SHOULDERS.name,
                workingSets = 8,
                sessionLoadSrpe = 450,
                performedAtMillis = now - 30_000L
            ),
            WorkoutLog(
                id = "w4",
                type = WorkoutType.RUNNING.name,
                durationMinutes = 30,
                rpe = 7,
                sessionLoadSrpe = 210,
                performedAtMillis = now - 40_000L
            )
        )

        val summary = NaturalHypertrophyScience.analyzeLastSevenDays(workouts, profile, now)
        assertTrue(summary.deloadRecommended)
        assertNotNull(summary.laggingMuscleGroup)
        assertEquals(MuscleGroup.BACK, summary.laggingMuscleGroup)
        assertEquals(4, summary.recommendedSplitDays.size)
    }
}
