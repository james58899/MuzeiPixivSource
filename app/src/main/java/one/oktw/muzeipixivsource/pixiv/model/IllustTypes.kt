package one.oktw.muzeipixivsource.pixiv.model

import com.google.gson.annotations.SerializedName

@Suppress("unused")
enum class IllustTypes {
    @SerializedName("illust")
    ILLUST,

    @SerializedName("manga")
    MANGA,

    @SerializedName("ugoira")
    UGOIRA
}
