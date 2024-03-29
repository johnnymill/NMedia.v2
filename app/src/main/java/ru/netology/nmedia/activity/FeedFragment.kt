package ru.netology.nmedia.activity

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.core.view.MenuProvider
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.snackbar.Snackbar
import ru.netology.nmedia.R
import ru.netology.nmedia.activity.ImageFragment.Companion.textArg
import ru.netology.nmedia.adapter.OnInteractionListener
import ru.netology.nmedia.adapter.PostsAdapter
import ru.netology.nmedia.auth.AppAuth
import ru.netology.nmedia.databinding.FragmentFeedBinding
import ru.netology.nmedia.dto.Post
import ru.netology.nmedia.model.FeedModelActing
import ru.netology.nmedia.viewmodel.AuthViewModel
import ru.netology.nmedia.viewmodel.PostViewModel

class FeedFragment : Fragment() {

    private val viewModel: PostViewModel by activityViewModels()
    private val authViewModel: AuthViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val binding = FragmentFeedBinding.inflate(inflater, container, false)

        val adapter = PostsAdapter(object : OnInteractionListener {
            override fun onEdit(post: Post) {
                viewModel.edit(post)
            }

            override fun onLike(post: Post) {
                if (AppAuth.getInstance().isUserValid()) {
                    viewModel.likeById(post.id)
                } else {
                    findNavController().navigate(R.id.action_feedFragment_to_authSigninFragment)
                }
            }

            override fun onRemove(post: Post) {
                viewModel.removeById(post.id)
            }

            override fun onShare(post: Post) {
                val intent = Intent().apply {
                    action = Intent.ACTION_SEND
                    putExtra(Intent.EXTRA_TEXT, post.content)
                    type = "text/plain"
                }

                val shareIntent =
                    Intent.createChooser(intent, getString(R.string.chooser_share_post))
                startActivity(shareIntent)
            }

            override fun onViewImage(post: Post) {
                findNavController().navigate(
                    R.id.action_feedFragment_to_imageFragment,
                    Bundle().apply {
                        textArg = post.attachment!!.url
                    })
            }
        })

        var currentAuthMenuProvider: MenuProvider? = null
        authViewModel.authLiveData.observe(viewLifecycleOwner) { authModel ->
            currentAuthMenuProvider?.let(requireActivity()::removeMenuProvider)
            requireActivity().addMenuProvider(object : MenuProvider {
                override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
                    menuInflater.inflate(R.menu.options_auth, menu)
                    menu.setGroupVisible(R.id.authorized, authViewModel.isAuthorized)
                    menu.setGroupVisible(R.id.unauthorized, !authViewModel.isAuthorized)
                }

                override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
                    return when (menuItem.itemId) {
                        R.id.signIn -> {
                            findNavController().navigate(R.id.action_feedFragment_to_authSigninFragment)
                            true
                        }

                        R.id.signUp -> {
                            findNavController().navigate(R.id.action_feedFragment_to_authSignupFragment)
                            true
                        }

                        R.id.signOut -> {
                            val newFragment = AuthSignOutFragment()
                            newFragment.show(parentFragmentManager, "dialog")
                            true
                        }

                        else -> false
                    }
                }

            }.also { currentAuthMenuProvider = it }, viewLifecycleOwner)
        }

        binding.list.adapter = adapter
        viewModel.state.observe(viewLifecycleOwner) { state ->
            binding.progress.isVisible = state.acting == FeedModelActing.LOADING
            binding.swiperefresh.isRefreshing = state.acting == FeedModelActing.REFRESHING

            if (state.error) {
                val message = if (state.response.code == 0) {
                    getString(R.string.error_loading)
                } else {
                    getString(
                        R.string.error_response,
                        state.response.message.toString(),
                        state.response.code
                    )
                }
                val actResId = when (state.acting) {
                    FeedModelActing.IDLE -> android.R.string.ok
                    else -> R.string.retry_acting
                }
                Snackbar.make(
                    binding.root,
                    message,
                    Snackbar.LENGTH_LONG
                ).setAction(actResId) {
                    when (state.acting) {
                        FeedModelActing.LOADING -> viewModel.loadPosts()
                        FeedModelActing.REFRESHING -> viewModel.refreshPosts()
                        FeedModelActing.REMOVING -> viewModel.removeById(state.postId)
                        FeedModelActing.LIKING -> viewModel.likeById(state.postId)
                        FeedModelActing.IDLE -> return@setAction
                    }
                }.show()
            }
        }

        viewModel.data.observe(viewLifecycleOwner) { data ->
            adapter.submitList(data.posts)
            binding.emptyText.isVisible = data.empty
        }

        viewModel.newerCount.observe(viewLifecycleOwner) {
            if (it == 0) {
                binding.fabNewer.hide()
            } else {
                binding.fabNewer.text = getString(R.string.new_posts, it)
                binding.fabNewer.show()
            }
        }

        binding.swiperefresh.setOnRefreshListener {
            viewModel.refreshPosts()
        }

        binding.fab.setOnClickListener {
            if (AppAuth.getInstance().isUserValid()) {
                findNavController().navigate(R.id.action_feedFragment_to_newPostFragment)
            } else {
                findNavController().navigate(R.id.action_feedFragment_to_authSigninFragment)
            }
        }

        binding.fabNewer.setOnClickListener {
            viewModel.loadNewPosts()
            binding.fabNewer.hide()
        }

        adapter.registerAdapterDataObserver(object : RecyclerView.AdapterDataObserver() {
            override fun onItemRangeInserted(positionStart: Int, itemCount: Int) {
                if (positionStart == 0) {
                    binding.list.smoothScrollToPosition(0)
                }
            }
        })

        return binding.root
    }
}
