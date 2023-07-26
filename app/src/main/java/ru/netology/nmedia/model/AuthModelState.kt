package ru.netology.nmedia.model

data class AuthModelState(
    val acting: AuthModelActing = AuthModelActing.IDLE,
    val error: Boolean = false,
    val response: AuthResponse = AuthResponse(),
)

enum class AuthModelActing {
    IDLE,
    SIGN_IN,
    COMPLETE,
}

data class AuthResponse(
    val code: Int = 0,
    val message: String? = null
)
