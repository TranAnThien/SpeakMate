package com.project.speakmate.data.remote.firebase

import com.google.firebase.firestore.Source

fun Boolean.toFirestoreSource(): Source = if (this) Source.SERVER else Source.DEFAULT
