package one.oktw.muzeipixivsource.pixiv.model

data class OAuthResponse(
    val accessToken: String,
    val expiresIn: Int,
    val tokenType: String,
    val refreshToken: String,
    val scope: String,
    val user: User,
)
