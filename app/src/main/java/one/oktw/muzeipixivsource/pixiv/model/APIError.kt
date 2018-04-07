package one.oktw.muzeipixivsource.pixiv.model

data class APIError(
    val userMessage: String,
    val message: String,
    val reason: String
)