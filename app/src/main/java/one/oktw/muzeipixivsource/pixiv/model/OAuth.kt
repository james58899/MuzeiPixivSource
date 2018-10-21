package one.oktw.muzeipixivsource.pixiv.model

data class OAuth(
    val response: OAuthResponse? = null,
    val has_error: Boolean = false,
    val errors: Errors? = null
) {
    data class Errors(val system: System) {
        data class System(val message: String, val code: Int = -1)
    }
}