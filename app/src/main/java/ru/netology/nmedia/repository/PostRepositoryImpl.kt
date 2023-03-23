package ru.netology.nmedia.repository

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.internal.EMPTY_REQUEST
import ru.netology.nmedia.BuildConfig
import ru.netology.nmedia.dto.Post
import java.io.IOException
import java.util.concurrent.TimeUnit


class PostRepositoryImpl : PostRepository {
    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .build()
    private val gson = Gson()
    private val typeToken = object : TypeToken<List<Post>>() {}
    private val typeTokenOnePost = object : TypeToken<Post>() {}

    companion object {
        private const val BASE_URL = BuildConfig.NMEDIA_SERVER
        private val jsonType = "application/json".toMediaType()
    }

    private fun getById(id: Long, callback: PostRepository.ResponseCallback<Post>) {
        val request: Request = Request.Builder()
            .url("${BASE_URL}/api/slow/posts/$id")
            .build()

        client.newCall(request)
            .enqueue(object : Callback {
                override fun onResponse(call: Call, response: Response) {
                    val body = response.body?.string() ?: throw RuntimeException("body is null")
                    try {
                        callback.onSuccess(gson.fromJson(body, typeTokenOnePost.type))
                    } catch (e: Exception) {
                        callback.onError(e)
                    }
                }

                override fun onFailure(call: Call, e: IOException) {
                    callback.onError(e)
                }
            })
    }

    override fun getAll(callback: PostRepository.ResponseCallback<List<Post>>) {
        val request: Request = Request.Builder()
            .url("${BASE_URL}/api/slow/posts")
            .build()

        client.newCall(request)
            .enqueue(object : Callback {
                override fun onResponse(call: Call, response: Response) {
                    val body = response.body?.string() ?: throw RuntimeException("body is null")
                    try {
                        callback.onSuccess(gson.fromJson(body, typeToken.type))
                    } catch (e: Exception) {
                        callback.onError(e)
                    }
                }

                override fun onFailure(call: Call, e: IOException) {
                    callback.onError(e)
                }
            })
    }

    override fun likeById(id: Long, callback: PostRepository.ResponseCallback<Post>) {
        getById(id, object : PostRepository.ResponseCallback<Post> {
            override fun onSuccess(result: Post) {
                val request = if (!result.likedByMe) {
                    Request.Builder()
                        .url("${BASE_URL}/api/posts/$id/likes")
                        .post(EMPTY_REQUEST)
                        .build()
                } else {
                    Request.Builder()
                        .url("${BASE_URL}/api/posts/$id/likes")
                        .delete()
                        .build()
                }

                client.newCall(request)
                    .enqueue(object : Callback {
                        override fun onResponse(call: Call, response: Response) {
                            val body = response.body?.string() ?: throw RuntimeException("body is null")
                            try {
                                callback.onSuccess(gson.fromJson(body, typeTokenOnePost.type))
                            } catch (e: Exception) {
                                callback.onError(e)
                            }
                        }

                        override fun onFailure(call: Call, e: IOException) {
                            callback.onError(e)
                        }
                    })
            }

            override fun onError(e: Exception) {
                callback.onError(e)
            }
        })
    }

    override fun save(post: Post, callback: PostRepository.ResponseCallback<Post>) {
        val request: Request = Request.Builder()
            .post(gson.toJson(post).toRequestBody(jsonType))
            .url("${BASE_URL}/api/slow/posts")
            .build()

        client.newCall(request)
            .enqueue(object : Callback {
                override fun onResponse(call: Call, response: Response) {
                    val body = response.body?.string() ?: throw RuntimeException("body is null")
                    try {
                        callback.onSuccess(gson.fromJson(body, typeTokenOnePost.type))
                    } catch (e: Exception) {
                        callback.onError(e)
                    }
                }

                override fun onFailure(call: Call, e: IOException) {
                    callback.onError(e)
                }
            })
    }

    override fun removeById(id: Long, callback: PostRepository.ResponseCallback<Any>) {
        val request: Request = Request.Builder()
            .delete()
            .url("${BASE_URL}/api/slow/posts/$id")
            .build()

        client.newCall(request)
            .enqueue(object : Callback {
                override fun onResponse(call: Call, response: Response) {
                    if (response.body == null) {
                        throw RuntimeException("body is null")
                    }
                    try {
                        callback.onSuccess(Any())
                    } catch (e: Exception) {
                        callback.onError(e)
                    }
                }

                override fun onFailure(call: Call, e: IOException) {
                    callback.onError(e)
                }
            })
    }
}
