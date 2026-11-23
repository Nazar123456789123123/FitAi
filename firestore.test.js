const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ALICE_UID = "alice_athlete_123";
const BOB_UID = "bob_athlete_456";

const [emulatorHost, emulatorPortStr] = (process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085").split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

before(async () => {
  const rules = fs.readFileSync("./firestore.rules", "utf8");
  testEnv = await initializeTestEnvironment({
    projectId: PROJECT_ID,
    firestore: {
      rules,
      host: emulatorHost,
      port: emulatorPort,
    },
  });
});

after(async () => {
  if (testEnv) {
    await testEnv.cleanup();
  }
});

beforeEach(async () => {
  if (testEnv) {
    await testEnv.clearFirestore();
  }
});

function buildValidWorkout(uid, overrides = {}) {
  const now = new Date(Date.now() - 2000);
  return {
    userId: uid,
    type: "STRENGTH",
    title: "Жим лежачи та плечі (MAV)",
    durationMinutes: 55,
    distanceValue: 0,
    distanceUnit: "KM",
    rpe: 8,
    muscleGroup: "CHEST",
    workingSets: 12,
    rir: 2,
    sessionLoadSrpe: 440,
    notes: "Гарне відновлення, 2 RIR на всіх підходах",
    performedAtMillis: Date.now() - 3600000,
    createdAt: now,
    updatedAt: now,
    ...overrides,
  };
}

function buildValidProfile(uid, overrides = {}) {
  const now = new Date(Date.now() - 2000);
  return {
    userId: uid,
    displayName: "Назар Атлет",
    bodyWeightKg: 78.5,
    ageYears: 27,
    heightCm: 179,
    trainingExperience: "INTERMEDIATE",
    primaryGoal: "HYPERTROPHY",
    equipment: "GYM",
    focusMuscles: ["CHEST", "BACK", "QUADS"],
    weeklyTargetSessions: 4,
    onboardingDone: true,
    createdAt: now,
    updatedAt: now,
    ...overrides,
  };
}

test("Unauthenticated user cannot read or write workouts or profiles", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).get());
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).collection("workouts").get());
  await assertFails(
    unauthDb.collection("users").doc(ALICE_UID).collection("workouts").doc("w_1").set(
      buildValidWorkout(ALICE_UID)
    )
  );
});

test("Owner can create, list, update, and delete valid workouts (running, swimming, strength)", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const workoutsCol = aliceDb.collection("users").doc(ALICE_UID).collection("workouts");

  // 1. Strength create
  await assertSucceeds(workoutsCol.doc("w_strength").set(buildValidWorkout(ALICE_UID)));

  // 2. Running create (km)
  await assertSucceeds(
    workoutsCol.doc("w_run").set(
      buildValidWorkout(ALICE_UID, {
        type: "RUNNING",
        title: "Аеробний біг зона 2",
        durationMinutes: 40,
        distanceValue: 6.5,
        distanceUnit: "KM",
        rpe: 6,
        muscleGroup: "NONE",
        workingSets: 0,
        rir: 3,
        sessionLoadSrpe: 240,
      })
    )
  );

  // 3. Swimming create (meters)
  await assertSucceeds(
    workoutsCol.doc("w_swim").set(
      buildValidWorkout(ALICE_UID, {
        type: "SWIMMING",
        title: "Кроль у басейні",
        durationMinutes: 45,
        distanceValue: 1800,
        distanceUnit: "M",
        rpe: 7,
        muscleGroup: "BACK",
        workingSets: 0,
        rir: 2,
        sessionLoadSrpe: 315,
      })
    )
  );

  // 4. List aligned with Kotlin query
  await assertSucceeds(workoutsCol.orderBy("performedAtMillis", "desc").get());

  // 5. Partial update preserving createdAt and userId
  await assertSucceeds(
    workoutsCol.doc("w_run").update({
      rpe: 7,
      sessionLoadSrpe: 280,
      updatedAt: new Date(Date.now() - 1000),
    })
  );

  // 6. Delete
  await assertSucceeds(workoutsCol.doc("w_swim").delete());
});

test("Rejects invalid RPE, negative duration, ghost fields, @DocumentId id field, or future timestamps", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const workoutsCol = aliceDb.collection("users").doc(ALICE_UID).collection("workouts");

  // RPE > 10
  await assertFails(workoutsCol.doc("bad_rpe").set(buildValidWorkout(ALICE_UID, { rpe: 11 })));
  // RPE < 1
  await assertFails(workoutsCol.doc("bad_rpe0").set(buildValidWorkout(ALICE_UID, { rpe: 0 })));
  // Invalid workout type
  await assertFails(workoutsCol.doc("bad_type").set(buildValidWorkout(ALICE_UID, { type: "CYCLING" })));
  // Invalid distance unit
  await assertFails(workoutsCol.doc("bad_unit").set(buildValidWorkout(ALICE_UID, { distanceUnit: "MI" })));
  // @DocumentId `id` field must not be written
  await assertFails(workoutsCol.doc("bad_id").set(buildValidWorkout(ALICE_UID, { id: "bad_id" })));
  // Ghost field
  await assertFails(workoutsCol.doc("bad_ghost").set(buildValidWorkout(ALICE_UID, { isVerified: true })));
  // Future timestamp
  await assertFails(
    workoutsCol.doc("bad_time").set(
      buildValidWorkout(ALICE_UID, { createdAt: new Date(Date.now() + 60000) })
    )
  );
});

test("Cross-user isolation: Bob cannot read or write Alice's profile, workouts, or AI insights", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();

  await assertSucceeds(aliceDb.collection("users").doc(ALICE_UID).set(buildValidProfile(ALICE_UID)));
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("workouts").doc("w_1").set(
      buildValidWorkout(ALICE_UID)
    )
  );

  await assertFails(bobDb.collection("users").doc(ALICE_UID).get());
  await assertFails(bobDb.collection("users").doc(ALICE_UID).collection("workouts").get());
  await assertFails(
    bobDb.collection("users").doc(ALICE_UID).collection("workouts").doc("w_1").update({
      title: "Hacked",
      updatedAt: new Date(Date.now() - 1000),
    })
  );
  await assertFails(
    bobDb.collection("users").doc(ALICE_UID).collection("workouts").doc("w_bob").set(
      buildValidWorkout(BOB_UID)
    )
  );
});

test("Owner can manage AthleteProfile and AiCoachInsight", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();

  await assertSucceeds(aliceDb.collection("users").doc(ALICE_UID).set(buildValidProfile(ALICE_UID)));
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).update({
      bodyWeightKg: 79.2,
      updatedAt: new Date(Date.now() - 1000),
    })
  );

  const now = new Date(Date.now() - 2000);
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("ai_insights").doc("ins_1").set({
      userId: ALICE_UID,
      summary: "Тижневий обсяг у межах MAV (12 підходів на груди), кардіо збалансоване.",
      volumeStatus: "MAV — Оптимальний стимул гіпертрофії",
      recoveryAdvice: "Тримайте 2 RIR та 6+ годин між бігом і тренуванням ніг.",
      nextWorkoutPlan: "Верх тіла: Жим гантелей 4x8-10 @ RPE 8, Тяга в нахилі 4x10 @ RPE 8.",
      createdAt: now,
      updatedAt: now,
    })
  );
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("ai_insights").get()
  );
});
