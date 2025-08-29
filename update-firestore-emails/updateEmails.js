const admin = require('firebase-admin');
const serviceAccount = require('./serviceAccountKey.json');

admin.initializeApp({
  credential: admin.credential.cert(serviceAccount)
});

async function updateAllUsersWithEmail() {
  const auth = admin.auth();
  const db = admin.firestore();

  let nextPageToken;
  do {
    const listUsersResult = await auth.listUsers(1000, nextPageToken);
    for (const userRecord of listUsersResult.users) {
      const uid = userRecord.uid;
      const email = userRecord.email ? userRecord.email.toLowerCase() : null;
      if (!email) continue;

      // Update Firestore user document
      await db.collection('users').doc(uid).set({ email }, { merge: true });
      console.log(`Updated user ${uid} with email ${email}`);
    }
    nextPageToken = listUsersResult.pageToken;
  } while (nextPageToken);
}

updateAllUsersWithEmail().then(() => {
  console.log('All users updated!');
  process.exit(0);
}).catch(err => {
  console.error('Error updating users:', err);
  process.exit(1);
}); 