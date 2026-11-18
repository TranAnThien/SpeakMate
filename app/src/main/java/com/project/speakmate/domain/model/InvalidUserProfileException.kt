package com.project.speakmate.domain.model

class InvalidUserProfileException(field: String) : IllegalArgumentException("Invalid user profile field: $field")
