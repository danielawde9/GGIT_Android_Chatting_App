package com.daniel.ggit.chattingappfinal

// Model
data class FriendlyMessage(
    // accept text (body message),
    var text: String="",
    // name of the user
    var name: String="",
    // user photo url
    var photoUrl: String=""
) {

    // each message has seperate id
    var id: String? = null

}