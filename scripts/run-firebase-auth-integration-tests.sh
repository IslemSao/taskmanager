#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT_DIR"

firebase emulators:exec \
  --project demo-taskmanager \
  --only auth,firestore \
  "./gradlew :app:connectedDebugAndroidTest \
    -Pandroid.testInstrumentationRunnerArguments.useFirebaseEmulator=true \
    -Pandroid.testInstrumentationRunnerArguments.firebaseEmulatorProjectId=demo-taskmanager \
    -Pandroid.testInstrumentationRunnerArguments.firebaseEmulatorHost=10.0.2.2 \
    -Pandroid.testInstrumentationRunnerArguments.firebaseAuthEmulatorPort=9099 \
    -Pandroid.testInstrumentationRunnerArguments.firebaseFirestoreEmulatorPort=8080"
