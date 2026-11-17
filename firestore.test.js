const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ALICE_UID = "alice_123";
const BOB_UID = "bob_456";
const ADMIN_EMAIL = "dayaramasuthara5@gmail.com";

const [emulatorHost, emulatorPortStr] = (process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085").split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

function serverTs(db) {
  // rules-unit-testing exposes firebase-admin/firestore FieldValue via doc/set,
  // or we can pass a past/current Date which Firestore converts to Timestamp <= request.time
  return new Date(Date.now() - 1000);
}

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

test("Unauthenticated user: cannot read users, posts, stories, chats, or reports", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection("users").where("isPublic", "==", true).get());
  await assertFails(unauthDb.collection("posts").where("visibility", "==", "public").get());
  await assertFails(unauthDb.collection("stories").where("visibility", "==", "active").get());
  await assertFails(unauthDb.collection("reports").get());
});

test("User profile: owner can create & update, cannot self-suspend or inject ghost field", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const now = serverTs(aliceDb);

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).set({
      userId: ALICE_UID,
      username: "alice.dev",
      displayName: "Alice",
      bio: "Capturing light",
      isPublic: true,
      isSuspended: false,
      followersCount: 0,
      followingCount: 0,
      postsCount: 0,
      createdAt: now,
      updatedAt: now,
    })
  );

  // Shadow update with ghost field must fail
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).update({
      isVerified: true,
      updatedAt: serverTs(aliceDb),
    })
  );

  // Self-suspension attempt must fail
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).update({
      isSuspended: true,
      updatedAt: serverTs(aliceDb),
    })
  );

  // Bob cannot edit Alice's bio
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  await assertFails(
    bobDb.collection("users").doc(ALICE_UID).update({
      bio: "Hacked",
      updatedAt: serverTs(bobDb),
    })
  );
});

test("PII Isolation: private/info is readable and writable ONLY by owner or admin", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("private").doc("info").set({
      userId: ALICE_UID,
      email: "alice@example.com",
      phoneNumber: "+15551234567",
      updatedAt: serverTs(aliceDb),
    })
  );

  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  await assertFails(
    bobDb.collection("users").doc(ALICE_UID).collection("private").doc("info").get()
  );

  const adminDb = testEnv.authenticatedContext("admin_uid", {
    email: ADMIN_EMAIL,
    email_verified: true,
  }).firestore();
  await assertSucceeds(
    adminDb.collection("users").doc(ALICE_UID).collection("private").doc("info").get()
  );
});

test("Posts & Reels: query alignment, likes, comments, and delete ownership", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const now = serverTs(aliceDb);

  await assertSucceeds(
    aliceDb.collection("posts").doc("post_1").set({
      authorId: ALICE_UID,
      authorUsername: "alice.dev",
      postKind: "post",
      mediaType: "image",
      mediaDataUrl: "data:image/jpeg;base64,abc",
      caption: "Hello #instaconnect",
      hashtags: ["instaconnect"],
      visibility: "public",
      likesCount: 0,
      commentsCount: 0,
      sharesCount: 0,
      createdAt: now,
      updatedAt: now,
    })
  );

  // Unfiltered list without visibility or authorId must fail (rules are not filters)
  await assertFails(bobDb.collection("posts").get());

  // Filtered public list succeeds
  await assertSucceeds(bobDb.collection("posts").where("visibility", "==", "public").get());

  // Bob can like and increment likesCount by +1
  await assertSucceeds(
    bobDb.collection("posts").doc("post_1").collection("likes").doc(BOB_UID).set({
      postId: "post_1",
      userId: BOB_UID,
      isActive: true,
      createdAt: serverTs(bobDb),
    })
  );
  await assertSucceeds(
    bobDb.collection("posts").doc("post_1").update({
      likesCount: 1,
      updatedAt: serverTs(bobDb),
    })
  );

  // Bob cannot jump likesCount by +50
  await assertFails(
    bobDb.collection("posts").doc("post_1").update({
      likesCount: 50,
      updatedAt: serverTs(bobDb),
    })
  );

  // Bob cannot delete Alice's post
  await assertFails(bobDb.collection("posts").doc("post_1").delete());
});

test("Chats & Messages: strictly isolated to participants", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const charlieDb = testEnv.authenticatedContext("charlie_789").firestore();
  const chatId = "alice_123_bob_456";
  const now = serverTs(aliceDb);

  await assertSucceeds(
    aliceDb.collection("chats").doc(chatId).set({
      participantIds: [ALICE_UID, BOB_UID],
      lastMessageText: "Hey Bob!",
      lastMessageType: "text",
      lastSenderId: ALICE_UID,
      createdAt: now,
      updatedAt: now,
    })
  );

  await assertSucceeds(
    bobDb.collection("chats").doc(chatId).collection("messages").doc("msg_1").set({
      chatId,
      senderId: BOB_UID,
      participantIds: [ALICE_UID, BOB_UID],
      messageType: "text",
      text: "Hey Alice!",
      createdAt: serverTs(bobDb),
    })
  );

  // Charlie cannot read chat, direct message, unfiltered messages, or Alice's messages
  await assertFails(charlieDb.collection("chats").doc(chatId).get());
  await assertFails(charlieDb.collection("chats").doc(chatId).collection("messages").doc("msg_1").get());
  await assertFails(charlieDb.collection("chats").doc(chatId).collection("messages").get());
  await assertFails(
    charlieDb.collection("chats").doc(chatId).collection("messages")
      .where("participantIds", "array-contains", ALICE_UID).get()
  );
});

test("Admin & Reports: spoofed email rejected, verified admin can moderate posts and resolve reports", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const now = serverTs(aliceDb);

  await assertSucceeds(
    aliceDb.collection("reports").doc("rep_1").set({
      reporterId: ALICE_UID,
      targetType: "post",
      targetId: "post_1",
      targetSummary: "Spam post",
      reason: "Suspicious spam content",
      status: "open",
      createdAt: now,
      updatedAt: now,
    })
  );

  // Spoofed unverified admin email must be rejected
  const spoofAdminDb = testEnv.authenticatedContext("spoof_uid", {
    email: ADMIN_EMAIL,
    email_verified: false,
  }).firestore();
  await assertFails(spoofAdminDb.collection("reports").get());

  // Non-admin cannot resolve report
  await assertFails(
    aliceDb.collection("reports").doc("rep_1").update({
      status: "resolved",
      updatedAt: serverTs(aliceDb),
    })
  );

  // Verified admin can list and resolve report once
  const adminDb = testEnv.authenticatedContext("admin_uid", {
    email: ADMIN_EMAIL,
    email_verified: true,
  }).firestore();
  await assertSucceeds(adminDb.collection("reports").get());
  await assertSucceeds(
    adminDb.collection("reports").doc("rep_1").update({
      status: "resolved",
      updatedAt: serverTs(adminDb),
    })
  );

  // Terminal state lock: cannot update resolved report again
  await assertFails(
    adminDb.collection("reports").doc("rep_1").update({
      status: "dismissed",
      updatedAt: serverTs(adminDb),
    })
  );
});
