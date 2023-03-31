package ru.netology.nmedia.viewmodel

import android.app.Application
import androidx.lifecycle.*
import ru.netology.nmedia.dto.Post
import ru.netology.nmedia.model.FeedModel
import ru.netology.nmedia.model.FeedResponse
import ru.netology.nmedia.repository.*
import ru.netology.nmedia.util.SingleLiveEvent

private val empty = Post(
    id = 0,
    content = "",
    author = "",
    authorAvatar = "",
    likedByMe = false,
    likes = 0,
    published = ""
)

class PostViewModel(application: Application) : AndroidViewModel(application) {
    // упрощённый вариант
    private val repository: PostRepository = PostRepositoryImpl()
    private val _data = MutableLiveData(FeedModel())
    val data: LiveData<FeedModel>
        get() = _data
    val edited = MutableLiveData(empty)
    private val _postCreated = SingleLiveEvent<Unit>()
    val postCreated: LiveData<Unit>
        get() = _postCreated

    init {
        loadPosts()
    }

    private fun reload(isInitial: Boolean) {
        _data.value = FeedModel(loading = isInitial, refreshing = !isInitial)
        repository.getAll(object : PostRepository.ResponseCallback<List<Post>> {
            override fun onSuccess(result: List<Post>) {
                _data.postValue(FeedModel(posts = result, empty = result.isEmpty()))
            }

            override fun onError(code: Int, message: String) {
                _data.postValue(FeedModel(error = true, response = FeedResponse(code, message)))
            }

            override fun onFailure(e: Exception) {
                _data.postValue(FeedModel(error = true))
            }
        })
    }

    fun loadPosts() {
        reload(true)
    }

    fun refreshPosts() {
        reload(false)
    }

    fun save() {
        edited.value?.let {
            repository.save(it, object : PostRepository.ResponseCallback<Post> {
                override fun onSuccess(result: Post) {
                    // TODO compare it against result?
                    _postCreated.postValue(Unit)
                }

                override fun onError(code: Int, message: String) {
                    _data.postValue(FeedModel(response = FeedResponse(code, message)))
                }

                override fun onFailure(e: Exception) {
                    TODO("IDK what to do here")
                }
            })
        }
        edited.value = empty
    }

    fun edit(post: Post) {
        edited.value = post
    }

    fun changeContent(content: String) {
        val text = content.trim()
        if (edited.value?.content == text) {
            return
        }
        edited.value = edited.value?.copy(content = text)
    }

    fun likeById(id: Long) {
        // Оптимистичная модель
        val old = _data.value?.posts.orEmpty()
        _data.value = _data.value?.copy(posts = _data.value?.posts.orEmpty().map {
            if (it.id != id) it else it.copy(
                likedByMe = !it.likedByMe,
                likes = it.likes + if (!it.likedByMe) 1 else -1
            )
        })

        val liker = object : PostRepository.ResponseCallback<Post> {
            override fun onSuccess(result: Post) {
                // Nothing to do because of optimistic model.
                // It is not much significant to lose possibly updated likes counter,
                // blinking icon due to query delay is worse.
            }

            override fun onError(code: Int, message: String) {
                _data.postValue(FeedModel(posts = old, response = FeedResponse(code, message)))
            }

            override fun onFailure(e: Exception) {
                _data.postValue(_data.value?.copy(posts = old))
            }
        }

        if (old.firstOrNull { it.id == id } !!.likedByMe) {
            repository.dislikeById(id, liker)
        } else {
            repository.likeById(id, liker)
        }
    }

    fun removeById(id: Long) {
        // Оптимистичная модель
        val old = _data.value?.posts.orEmpty()
        _data.value = _data.value?.copy(posts = _data.value?.posts.orEmpty()
            .filter { it.id != id }
        )
        repository.removeById(id, object : PostRepository.ResponseCallback<Unit> {
            override fun onSuccess(result: Unit) {
                // Nothing to do because of optimistic model.
            }

            override fun onError(code: Int, message: String) {
                _data.postValue(FeedModel(posts = old, response = FeedResponse(code, message)))
            }

            override fun onFailure(e: Exception) {
                _data.postValue(_data.value?.copy(posts = old))
            }
        })
    }
}
