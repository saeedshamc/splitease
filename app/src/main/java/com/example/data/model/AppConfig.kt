package com.example.data.model

data class AppUpdatePolicy(
    val latestVersionCode: Int = 1,
    val latestVersionName: String = "1.0.0",
    val isMandatory: Boolean = false,
    val releaseNotes: String = "",
    val upcomingFeaturesTeaser: String? = null,
    val downloadUrl: String = "https://example.com/download"
)

data class AdminMessage(
    val id: String = "msg_1",
    val title: String = "",
    val message: String = "",
    val type: String = "INFO", // "INFO", "WARNING", "FEATURE_TEASER"
    val isActive: Boolean = true,
    val timestamp: Long = System.currentTimeMillis()
)

data class AppBackendResponse(
    val updatePolicy: AppUpdatePolicy? = null,
    val activeMessage: AdminMessage? = null
)
