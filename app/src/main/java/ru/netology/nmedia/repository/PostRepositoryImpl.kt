package ru.netology.nmedia.repository

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import ru.netology.nmedia.BuildConfig
import ru.netology.nmedia.api.Api
import ru.netology.nmedia.dao.PostDao
import ru.netology.nmedia.dto.Attachment
import ru.netology.nmedia.dto.AttachmentType
import ru.netology.nmedia.dto.Media
import ru.netology.nmedia.dto.Post
import ru.netology.nmedia.entity.PostEntity
import ru.netology.nmedia.entity.toDto
import ru.netology.nmedia.entity.toEntity
import ru.netology.nmedia.error.ApiError
import ru.netology.nmedia.error.AppError
import ru.netology.nmedia.error.NetworkError
import ru.netology.nmedia.error.UnknownError
import ru.netology.nmedia.model.AuthModel
import java.io.File
import java.io.IOException


class PostRepositoryImpl(private val postDao: PostDao) : PostRepository {
    override val posts: Flow<List<Post>> = postDao.getAll()
        .map(List<PostEntity>::toDto)
        .flowOn(Dispatchers.Default)

//    private fun getById(id: Long, callback: PostRepository.ResponseCallback<Post>) {
//        ApiPosts.retrofitService.getById(id).enqueue(object : Callback<Post> {
//            override fun onResponse(call: Call<Post>, response: Response<Post>) {
//                if (!response.isSuccessful) {
//                    callback.onError(response.code(), response.message())
//                } else {
//                    callback.onSuccess(requireNotNull(response.body()) { "body is null" })
//                }
//            }
//
//            override fun onFailure(call: Call<Post>, t: Throwable) {
//                callback.onFailure(Exception(t))
//            }
//        })
//    }

    override suspend fun getAll() {
        try {
            val response = Api.retrofitService.getAll()
            if (!response.isSuccessful) {
                throw ApiError(response.code(), response.message())
            }
            val body = response.body() ?: throw ApiError(response.code(), response.message())
            postDao.insert(body.toEntity())
        } catch (e: IOException) {
            throw NetworkError
        } catch (e: Exception) {
            throw UnknownError
        }
    }

    override fun getNewerCount(): Flow<Int> = flow<Int> {
        while (true) {
            delay(30_000L)
            val id = if (postDao.isEmpty()) 0L else postDao.getLatestId()
            val response = Api.retrofitService.getNewer(id)
            if (!response.isSuccessful) {
                throw ApiError(response.code(), response.message())
            }
            val body = response.body() ?: throw ApiError(response.code(), response.message())
            postDao.insert(body.toEntity().map { it.copy(hidden = true) })
            val count = postDao.getNumHidden()
            emit(count)
            if (BuildConfig.DEBUG) {
                Log.d("REPOSITORY", "unread posts: $count")
            }
        }
    }
        .catch { e -> throw AppError.from(e) }
        .flowOn(Dispatchers.Default)

    override suspend fun showNewer() {
        postDao.updateNewer()
    }

    override suspend fun save(post: Post) {
        try {
            val response = Api.retrofitService.save(post)
            if (!response.isSuccessful) {
                throw ApiError(response.code(), response.message())
            }
            val body = response.body() ?: throw ApiError(response.code(), response.message())
            postDao.insert(PostEntity.fromDto(body))
        } catch (e: IOException) {
            throw NetworkError
        } catch (e: Exception) {
            throw UnknownError
        }
    }

    override suspend fun saveWithAttachment(file: File, post: Post) {
        try {
            val media = upload(file)
            val response = Api.retrofitService.save(
                post.copy(
                    attachment = Attachment(
                        url = media.id,
                        type = AttachmentType.IMAGE,
                        description = ""
                    )
                )
            )
            if (!response.isSuccessful) {
                throw ApiError(response.code(), response.message())
            }
            val body = response.body() ?: throw ApiError(response.code(), response.message())
            postDao.insert(PostEntity.fromDto(body))
        } catch (e: IOException) {
            throw NetworkError
        } catch (e: Exception) {
            throw UnknownError
        }
    }

    private suspend fun upload(file: File): Media {
        return Api.retrofitService.upload(
            MultipartBody.Part.createFormData(
                "file",
                file.name,
                file.asRequestBody()
            )
        ).let { requireNotNull(it.body()) }
    }

    override suspend fun removeById(id: Long) {
        val postRemoved = postDao.get(id)
        if (BuildConfig.DEBUG) {
            Log.d("REPOSITORY", "post to be deleted: $postRemoved")
        }
        postDao.removeById(id)
        try {
            val response = Api.retrofitService.removeById(id)
            if (!response.isSuccessful) {
                postDao.insert(postRemoved) // recover previous state
                throw ApiError(response.code(), response.message())
            }
        } catch (e: IOException) {
            postDao.insert(postRemoved) // recover previous state
            throw NetworkError
        } catch (e: Exception) {
            postDao.insert(postRemoved) // recover previous state
            throw UnknownError
        }
    }

    override suspend fun likeById(id: Long) {
        val post = postDao.get(id)
        if (BuildConfig.DEBUG) {
            Log.d("REPOSITORY", "post to be liked: $post")
        }
        postDao.likeById(id)
        try {
            val response = if (!post.likedByMe)
                Api.retrofitService.likeById(id)
            else
                Api.retrofitService.dislikeById(id)
            if (!response.isSuccessful) {
                postDao.likeById(id)    // recover previous state
                throw ApiError(response.code(), response.message())
            }
            if (response.body() == null) {
                postDao.likeById(id)    // recover previous state
                throw ApiError(response.code(), response.message())
            }
        } catch (e: IOException) {
            postDao.likeById(id)    // recover previous state
            throw NetworkError
        } catch (e: Exception) {
            postDao.likeById(id)    // recover previous state
            throw UnknownError
        }
    }

    override suspend fun signIn(login: String, password: String): AuthModel {
        val response = Api.retrofitService.updateUser(login, password)
        if (!response.isSuccessful) {
            throw ApiError(response.code(), response.message())
        }
        return response.body() ?: throw ApiError(response.code(), response.message())
    }

    override suspend fun signUp(login: String, password: String, name: String): AuthModel {
        val response = Api.retrofitService.registerUser(login, password, name)
        if (!response.isSuccessful) {
            throw ApiError(response.code(), response.message())
        }
        return response.body() ?: throw ApiError(response.code(), response.message())
    }
}
