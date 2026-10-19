package com.etudiantsdroitmaroc.app.ui.forum

import android.app.AlertDialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.content.res.ColorStateList
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.core.widget.TextViewCompat
import androidx.recyclerview.widget.GridLayoutManager
import com.etudiantsdroitmaroc.app.R
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.etudiantsdroitmaroc.app.data.model.ForumGroup
import com.etudiantsdroitmaroc.app.data.model.Post
import com.etudiantsdroitmaroc.app.data.remote.ChatRepository
import com.etudiantsdroitmaroc.app.data.remote.ForumRepository
import com.etudiantsdroitmaroc.app.data.remote.GroupRepository
import com.etudiantsdroitmaroc.app.databinding.DialogCreateGroupBinding
import com.etudiantsdroitmaroc.app.databinding.DialogNewPostBinding
import com.etudiantsdroitmaroc.app.databinding.FragmentForumBinding
import com.etudiantsdroitmaroc.app.ui.moderation.ReportDialog
import com.etudiantsdroitmaroc.app.utils.ImageUploader
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch

class ForumFragment : Fragment() {

    private var _binding: FragmentForumBinding? = null
    private val binding get() = _binding!!

    private val repository = ForumRepository()
    private val groupRepository = GroupRepository()
    private val chatRepository = ChatRepository()
    private lateinit var adapter: ForumFeedAdapter
    private lateinit var groupAdapter: GroupAdapter
    private var allPosts: List<Post> = emptyList()
    private var allGroups: List<ForumGroup> = emptyList()
    private var friendUids: Set<String>? = null
    private var currentTab = TAB_PUBLIC
    private var postsLoaded = false
    private var groupsLoaded = false
    private var fabOpen = false
    private var blockedUids: Set<String> = emptySet()
    private var pickedGroupIconUri: Uri? = null
    private var groupIconPreviewCallback: ((Uri) -> Unit)? = null
    private var pickedPostImageUri: Uri? = null
    private var postImagePreviewCallback: ((Uri) -> Unit)? = null

    private val pickPostImageLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            pickedPostImageUri = uri
            postImagePreviewCallback?.invoke(uri)
        }
    }

    private val pickGroupIconLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            pickedGroupIconUri = uri
            groupIconPreviewCallback?.invoke(uri)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentForumBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        var wasOffline = false
        com.etudiantsdroitmaroc.app.utils.NetworkStatus.online.observe(viewLifecycleOwner) { on ->
            if (on) { if (wasOffline) { wasOffline = false; loadPosts(); loadGroups() } } else wasOffline = true
        }

        adapter = ForumFeedAdapter(
            requireContext(), emptyList(),
            onLikeClick = { post ->
                viewLifecycleOwner.lifecycleScope.launch {
                    try {
                        repository.toggleLike(post.id)
                        loadPosts()
                    } catch (e: Exception) {
            if (e is kotlinx.coroutines.CancellationException) throw e
                        Toast.makeText(context, com.etudiantsdroitmaroc.app.utils.ErrorUtils.friendly(e), Toast.LENGTH_SHORT).show()
                    }
                }
            },
            onEditClick = { post -> showEditPostDialog(post) },
            onDeleteClick = { post -> confirmDeletePost(post) },
            onReportClick = { post ->
                ReportDialog.show(
                    requireContext(), lifecycleScope,
                    targetType = "post", targetId = post.id,
                    targetOwnerUid = post.authorUid, targetOwnerName = post.authorName
                )
            },
            onBlockClick = { post ->
                ReportDialog.confirmBlock(
                    requireContext(), lifecycleScope, post.authorUid, post.authorName
                ) { loadPosts() }
            }
        )
        binding.rvPosts.layoutManager = LinearLayoutManager(context)
        binding.rvPosts.setHasFixedSize(true)
        binding.rvPosts.adapter = adapter

        setupFabMenu()

        groupAdapter = GroupAdapter(
            emptyList(),
            isMember = { it.memberUids.contains(FirebaseAuth.getInstance().currentUser?.uid) },
            onOpen = { openGroupChat(it) },
            onJoin = { joinGroup(it) }
        )
        binding.rvGroups.layoutManager = GridLayoutManager(context, 2)
        binding.rvGroups.adapter = groupAdapter
        setupTabs()
        loadGroups()

        com.etudiantsdroitmaroc.app.utils.BannerAdHelper.attach(requireContext(), binding.bannerAdContainer)

        binding.etSearch.addTextChangedListener(object : android.text.TextWatcher {
            override fun afterTextChanged(s: android.text.Editable?) {
                applyFilters()
            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })

        loadPosts()
    }

    // ---------- التبويبات الثلاثة ----------

    private fun setupTabs() {
        binding.tabPublic.setOnClickListener { selectTab(TAB_PUBLIC) }
        binding.tabFriends.setOnClickListener { selectTab(TAB_FRIENDS) }
        binding.tabGroups.setOnClickListener { selectTab(TAB_GROUPS) }
        selectTab(TAB_PUBLIC)
    }

    private fun selectTab(tab: Int) {
        currentTab = tab
        styleTab(binding.tabPublic, tab == TAB_PUBLIC)
        styleTab(binding.tabFriends, tab == TAB_FRIENDS)
        styleTab(binding.tabGroups, tab == TAB_GROUPS)
        binding.etSearch.hint = getString(
            if (tab == TAB_GROUPS) R.string.forum_search_groups else R.string.forum_search_posts
        )
        if (tab == TAB_FRIENDS && friendUids == null) loadFriends() else applyFilters()
    }

    private fun styleTab(tab: TextView, selected: Boolean) {
        val ctx = requireContext()
        tab.setBackgroundResource(if (selected) R.drawable.bg_btn_blue_solid else 0)
        val color = ContextCompat.getColor(ctx, if (selected) R.color.white else R.color.text_primary)
        tab.setTextColor(color)
        TextViewCompat.setCompoundDrawableTintList(tab, ColorStateList.valueOf(color))
    }

    private fun loadFriends() {
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                friendUids = repository.getFriendUids()
            } catch (e: Exception) {
                if (e is kotlinx.coroutines.CancellationException) throw e
                friendUids = emptySet()
                Toast.makeText(context, com.etudiantsdroitmaroc.app.utils.ErrorUtils.friendly(e), Toast.LENGTH_SHORT).show()
            }
            applyFilters()
        }
    }

    /** كيطبق التبويب الحالي + البحث على المنشورات أو المجموعات */
    private fun applyFilters() {
        val b = _binding ?: return
        val query = b.etSearch.text?.toString().orEmpty().trim()

        if (currentTab == TAB_GROUPS) {
            val groups = if (query.isEmpty()) allGroups else allGroups.filter {
                it.name.contains(query, ignoreCase = true) || it.description.contains(query, ignoreCase = true)
            }
            groupAdapter.updateData(groups)
            b.rvPosts.visibility = View.GONE
            b.rvGroups.visibility = View.VISIBLE
            showEmpty(groupsLoaded && groups.isEmpty(), R.string.forum_empty_groups)
            return
        }

        var posts = allPosts
        if (currentTab == TAB_FRIENDS) {
            val friends = friendUids.orEmpty()
            posts = posts.filter { it.authorUid in friends }
        }
        if (query.isNotEmpty()) {
            posts = posts.filter {
                it.content.contains(query, ignoreCase = true) ||
                    it.authorName.contains(query, ignoreCase = true)
            }
        }
        adapter.updateData(posts)
        b.rvPosts.visibility = View.VISIBLE
        b.rvGroups.visibility = View.GONE
        showEmpty(
            postsLoaded && posts.isEmpty(),
            if (currentTab == TAB_FRIENDS) R.string.forum_empty_friends else R.string.forum_empty_posts
        )
    }

    private fun showEmpty(show: Boolean, messageRes: Int) {
        val b = _binding ?: return
        b.tvForumEmpty.setText(messageRes)
        b.tvForumEmpty.visibility = if (show) View.VISIBLE else View.GONE
    }

    // ---------- زر الإضافة (إنشاء منشور / إضافة مجموعة) ----------

    private fun setupFabMenu() {
        binding.fabMain.setOnClickListener { toggleFab(!fabOpen) }
        binding.fabScrim.setOnClickListener { toggleFab(false) }
        binding.fabAddPost.setOnClickListener {
            toggleFab(false)
            showNewPostDialog()
        }
        binding.fabAddGroup.setOnClickListener {
            toggleFab(false)
            showCreateGroupDialog()
        }
    }

    private fun toggleFab(open: Boolean) {
        fabOpen = open
        binding.fabMenu.visibility = if (open) View.VISIBLE else View.GONE
        binding.fabScrim.visibility = if (open) View.VISIBLE else View.GONE
        binding.fabMain.animate().rotation(if (open) 45f else 0f).setDuration(150).start()
    }

    private fun loadPosts() {
        viewLifecycleOwner.lifecycleScope.launch {
            com.etudiantsdroitmaroc.app.utils.cacheThenServer(onError = { e ->
                android.widget.Toast.makeText(context, com.etudiantsdroitmaroc.app.utils.ErrorUtils.friendly(e), android.widget.Toast.LENGTH_LONG).show()
            }) {
                blockedUids = chatRepository.getBlockedUids()
                allPosts = repository.getPosts().filter { it.authorUid !in blockedUids }
                postsLoaded = true
                applyFilters()
            }
        }
    }

    private fun showNewPostDialog() {
        pickedPostImageUri = null
        val dialogBinding = DialogNewPostBinding.inflate(LayoutInflater.from(context))
        val dialog = AlertDialog.Builder(requireContext())
            .setView(dialogBinding.root)
            .create()

        bindPostDialogCommon(dialogBinding, dialog)

        postImagePreviewCallback = { uri ->
            dialogBinding.previewContainer.visibility = View.VISIBLE
            Glide.with(this).load(uri).centerCrop().into(dialogBinding.ivPostImagePreview)
        }
        dialog.setOnDismissListener { postImagePreviewCallback = null }
        dialogBinding.btnPickPostImage.setOnClickListener { pickPostImageLauncher.launch("image/*") }
        dialogBinding.btnRemoveImage.setOnClickListener {
            pickedPostImageUri = null
            dialogBinding.previewContainer.visibility = View.GONE
        }

        dialogBinding.btnPublish.setOnClickListener {
            val content = dialogBinding.etPostContent.text?.toString()?.trim().orEmpty()
            if (content.isEmpty() && pickedPostImageUri == null) {
                Toast.makeText(context, "اكتب شي حاجة أو زيد صورة قبل النشر", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            dialogBinding.btnPublish.isEnabled = false
            dialogBinding.btnPublish.setText(R.string.post_dialog_publishing)
            viewLifecycleOwner.lifecycleScope.launch {
                var imageUrl = ""
                val imageUri = pickedPostImageUri
                if (imageUri != null) {
                    val uploadResult = ImageUploader.uploadImage(requireContext(), imageUri)
                    imageUrl = uploadResult.getOrNull() ?: ""
                }
                val result = repository.createPost(content, imageUrl)
                if (result.isSuccess) {
                    dialog.dismiss()
                    loadPosts()
                } else {
                    dialogBinding.btnPublish.isEnabled = true
                    dialogBinding.btnPublish.setText(R.string.post_dialog_publish)
                    Toast.makeText(context, "فشل النشر، تأكد من الانترنت", Toast.LENGTH_SHORT).show()
                }
            }
        }

        showStyledDialog(dialog)
    }

    private fun showEditPostDialog(post: Post) {
        val dialogBinding = DialogNewPostBinding.inflate(LayoutInflater.from(context))
        val dialog = AlertDialog.Builder(requireContext())
            .setView(dialogBinding.root)
            .create()

        bindPostDialogCommon(dialogBinding, dialog)
        dialogBinding.tvDialogTitle.setText(R.string.post_dialog_edit_title)
        dialogBinding.tvDialogSubtitle.setText(R.string.post_dialog_edit_subtitle)
        dialogBinding.imageSection.visibility = View.GONE
        dialogBinding.btnPublish.setText(R.string.post_dialog_save)
        dialogBinding.etPostContent.setText(post.content)

        dialogBinding.btnPublish.setOnClickListener {
            val content = dialogBinding.etPostContent.text?.toString()?.trim().orEmpty()
            if (content.isEmpty()) return@setOnClickListener
            dialogBinding.btnPublish.isEnabled = false
            viewLifecycleOwner.lifecycleScope.launch {
                val result = repository.updatePost(post.id, content)
                if (result.isSuccess) {
                    dialog.dismiss()
                    loadPosts()
                } else {
                    dialogBinding.btnPublish.isEnabled = true
                    Toast.makeText(context, "فشل التعديل", Toast.LENGTH_SHORT).show()
                }
            }
        }
        showStyledDialog(dialog)
    }

    /** أزرار الإغلاق/الإلغاء + عدّاد الحروف (0/1000) المشتركين بين نافذة الإنشاء والتعديل */
    private fun bindPostDialogCommon(dialogBinding: DialogNewPostBinding, dialog: AlertDialog) {
        dialogBinding.btnCloseDialog.setOnClickListener { dialog.dismiss() }
        dialogBinding.btnCancelDialog.setOnClickListener { dialog.dismiss() }
        dialogBinding.etPostContent.addTextChangedListener(object : android.text.TextWatcher {
            override fun afterTextChanged(s: android.text.Editable?) {
                dialogBinding.tvCounter.text = "${s?.length ?: 0}/1000"
            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })
    }

    /** خلفية شفافة (باش تبان الزوايا المدورة) وعرض 94% من الشاشة */
    private fun showStyledDialog(dialog: AlertDialog) {
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        dialog.window?.setSoftInputMode(android.view.WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)
        dialog.show()
        dialog.window?.setLayout(
            (resources.displayMetrics.widthPixels * 0.94f).toInt(),
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
    }

    private fun confirmDeletePost(post: Post) {
        AlertDialog.Builder(requireContext())
            .setTitle("حذف المنشور")
            .setMessage("متأكد بغيتي تحذف هاد المنشور؟")
            .setPositiveButton("حذف") { _, _ ->
                viewLifecycleOwner.lifecycleScope.launch {
                    repository.deletePost(post.id)
                    loadPosts()
                }
            }
            .setNegativeButton("إلغاء", null)
            .show()
    }

    private fun loadGroups() {
        viewLifecycleOwner.lifecycleScope.launch {
            com.etudiantsdroitmaroc.app.utils.cacheThenServer(onError = { e ->
                android.widget.Toast.makeText(context, com.etudiantsdroitmaroc.app.utils.ErrorUtils.friendly(e), android.widget.Toast.LENGTH_SHORT).show()
            }) {
                allGroups = groupRepository.getGroups()
                groupsLoaded = true
                applyFilters()
            }
        }
    }

    private fun joinGroup(group: ForumGroup) {
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                groupRepository.joinGroup(group.id)
                Toast.makeText(context, "انضميتي لمجموعة ${group.name} ✅", Toast.LENGTH_SHORT).show()
                loadGroups()
            } catch (e: Exception) {
            if (e is kotlinx.coroutines.CancellationException) throw e
                Toast.makeText(context, com.etudiantsdroitmaroc.app.utils.ErrorUtils.friendly(e), Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun openGroupChat(group: ForumGroup) {
        val myUid = FirebaseAuth.getInstance().currentUser?.uid
        val intent = Intent(requireContext(), GroupChatActivity::class.java)
        intent.putExtra("groupId", group.id)
        intent.putExtra("groupName", group.name)
        intent.putExtra("groupIcon", group.iconUrl)
        intent.putExtra("isAdmin", group.creatorUid == myUid)
        startActivity(intent)
    }

    private fun showCreateGroupDialog() {
        pickedGroupIconUri = null
        val dialogBinding = DialogCreateGroupBinding.inflate(LayoutInflater.from(context))
        val dialog = AlertDialog.Builder(requireContext())
            .setView(dialogBinding.root)
            .create()

        dialogBinding.btnCloseGroupDialog.setOnClickListener { dialog.dismiss() }
        dialogBinding.btnCancelGroupDialog.setOnClickListener { dialog.dismiss() }

        groupIconPreviewCallback = { uri ->
            dialogBinding.ivGroupPlaceholder.visibility = View.GONE
            dialogBinding.ivGroupIconPreview.visibility = View.VISIBLE
            Glide.with(this).load(uri).centerCrop().into(dialogBinding.ivGroupIconPreview)
        }
        dialog.setOnDismissListener { groupIconPreviewCallback = null }

        dialogBinding.btnPickIcon.setOnClickListener { pickGroupIconLauncher.launch("image/*") }

        dialogBinding.btnCreateGroup.setOnClickListener {
            val name = dialogBinding.etGroupName.text?.toString()?.trim().orEmpty()
            val description = dialogBinding.etGroupDescription.text?.toString()?.trim().orEmpty()
            if (name.isEmpty() || description.isEmpty()) {
                Toast.makeText(context, "دخل اسم ووصف المجموعة", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            dialogBinding.btnCreateGroup.isEnabled = false
            dialogBinding.btnCreateGroup.setText(R.string.group_dialog_creating)
            viewLifecycleOwner.lifecycleScope.launch {
                var iconUrl = ""
                val iconUri = pickedGroupIconUri
                if (iconUri != null) {
                    val result = ImageUploader.uploadImage(requireContext(), iconUri)
                    iconUrl = result.getOrNull() ?: ""
                }
                try {
                    groupRepository.createGroup(name, description, iconUrl)
                    dialog.dismiss()
                    Toast.makeText(context, "تم إنشاء المجموعة ✅", Toast.LENGTH_SHORT).show()
                    loadGroups()
                } catch (e: Exception) {
                    if (e is kotlinx.coroutines.CancellationException) throw e
                    dialogBinding.btnCreateGroup.isEnabled = true
                    dialogBinding.btnCreateGroup.setText(R.string.group_dialog_create)
                    Toast.makeText(context, "فشل إنشاء المجموعة: ${com.etudiantsdroitmaroc.app.utils.ErrorUtils.friendly(e)}", Toast.LENGTH_SHORT).show()
                }
            }
        }

        showStyledDialog(dialog)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        private const val TAB_PUBLIC = 0
        private const val TAB_FRIENDS = 1
        private const val TAB_GROUPS = 2
    }
}
