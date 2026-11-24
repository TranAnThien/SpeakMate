package com.project.speakmate.data.remote.firebase

import com.google.firebase.firestore.SnapshotMetadata

fun SnapshotMetadata.isServerConfirmed(): Boolean = !isFromCache && !hasPendingWrites()
