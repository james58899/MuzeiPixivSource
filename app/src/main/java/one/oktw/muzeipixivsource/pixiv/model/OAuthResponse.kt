package one.oktw.muzeipixivsource.pixiv.model

import android.os.Parcel
import android.os.Parcelable

data class OAuthResponse(
    val accessToken: String,
    val expiresIn: Int,
    val tokenType: String,
    val refreshToken: String,
    val user: User,
    val deviceToken: String
) : Parcelable {
    constructor(parcel: Parcel) : this(
        parcel.readString()!!,
        parcel.readInt(),
        parcel.readString()!!,
        parcel.readString()!!,
        parcel.readParcelable(User::class.java.classLoader)!!,
        parcel.readString()!!
    )

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(accessToken)
        parcel.writeInt(expiresIn)
        parcel.writeString(tokenType)
        parcel.writeString(refreshToken)
        parcel.writeParcelable(user, flags)
        parcel.writeString(deviceToken)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<OAuthResponse> {
        override fun createFromParcel(parcel: Parcel): OAuthResponse {
            return OAuthResponse(parcel)
        }

        override fun newArray(size: Int): Array<OAuthResponse?> {
            return arrayOfNulls(size)
        }
    }
}