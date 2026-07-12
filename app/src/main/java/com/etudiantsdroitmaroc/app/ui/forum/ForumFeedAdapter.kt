package com.etudiantsdroitmaroc.app.ui.forum

import android.content.Context
import android.text.format.DateUtils
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.etudiantsdroitmaroc.app.data.model.Post
import com.etudiantsdroitmaroc.app.databinding.ItemNativeAdBinding
import com.etudiantsdroitmaroc.app.databinding.ItemPostBinding
import com.etudiantsdroitmaroc.app.utils.AdIds
import com.google.android.gms.ads.AdLoader
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.nativead.NativeAd

private const val TYPE_POST = 0
private const val TYPE_AD = 1
private const val AD_INTERVAL = 5 // إعلان واحد كل 5 منشورات

/**
 * كيدمج المنشورات مع إعلان Native Advanced كل 5 منشورات، بحال فيسبوك/إنستغرام.
 */
class ForumFeedAdapter(
    private val context: Context,
    private var posts: List<Post>,
    private val onLikeClick: (Post) -> Unit,
    private val onEditClick: (Post) -> Unit,
    private val onDeleteClick: (Post) -> Unit
) :
    RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private val loadedAds = mutableMapOf<Int, NativeAd>()

    inner class PostVH(val binding: ItemPostBinding) : RecyclerView.ViewHolder(binding.root)
    inner class AdVH(val binding: ItemNativeAdBinding) : RecyclerView.ViewHolder(binding.root)

    override fun getItemViewType(position: Int): Int {
        return if (position > 0 && position % (AD_INTERVAL + 1) == AD_INTERVAL) TYPE_AD else TYPE_POST
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return if (viewType == TYPE_AD) {
            AdVH(ItemNativeAdBinding.inflate(LayoutInflater.from(parent.context), parent, false))
        } else {
            PostVH(ItemPostBinding.inflate(LayoutInflater.from(parent.context), parent, false))
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        if (holder is AdVH) {
            bindAd(holder, position)
            return
        }

        val post = posts[postIndexFor(position)]
        val binding = (holder as PostVH).binding
        binding.tvAuthorName.text = post.authorName
        binding.tvContent.text = post.content
        val relativeTime = DateUtils.getRelativeTimeSpanString(post.createdAt)
        binding.tvMeta.text = "$relativeTime · ${post.commentsCount} تعليق"
        if (post.authorPhotoUrl.isNotEmpty()) {
            Glide.with(holder.itemView).load(post.authorPhotoUrl).into(binding.ivAuthorPhoto)
        }
        binding.tvMeta.setOnClickListener {
            val intent = android.content.Intent(context, CommentsActivity::class.java)
            intent.putExtra("postId", post.id)
            context.startActivity(intent)
        }
        binding.tvLikesCount.text = post.likesCount.toString()
        binding.btnLike.setOnClickListener {
            onLikeClick(post)
        }

        val myUid = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid
        if (post.authorUid == myUid) {
            binding.btnPostMenu.visibility = android.view.View.VISIBLE
            binding.btnPostMenu.setOnClickListener { anchor ->
                val popup = android.widget.PopupMenu(context, anchor)
                popup.menu.add("تعديل")
                popup.menu.add("حذف")
                popup.setOnMenuItemClickListener { item ->
                    when (item.title) {
                        "تعديل" -> onEditClick(post)
                        "حذف" -> onDeleteClick(post)
                    }
                    true
                }
                popup.show()
            }
        } else {
            binding.btnPostMenu.visibility = android.view.View.GONE
        }
    }

    /** كيرجع الـ index الحقيقي فـ لائحة المنشورات، محسوب بلا الأسطر ديال الإعلانات */
    private fun postIndexFor(position: Int): Int {
        val adsBefore = (position / (AD_INTERVAL + 1))
        return position - adsBefore
    }

    private fun bindAd(holder: AdVH, position: Int) {
        val existing = loadedAds[position]
        if (existing != null) {
            populateNativeAdView(existing, holder)
            return
        }

        val adLoader = AdLoader.Builder(context, AdIds.NATIVE_ADVANCED)
            .forNativeAd { nativeAd ->
                loadedAds[position] = nativeAd
                populateNativeAdView(nativeAd, holder)
            }
            .build()
        adLoader.loadAd(AdRequest.Builder().build())
    }

    private fun populateNativeAdView(nativeAd: NativeAd, holder: AdVH) {
        val adView = holder.binding.root
        adView.headlineView = holder.binding.adHeadline
        adView.bodyView = holder.binding.adBody
        adView.callToActionView = holder.binding.adCallToAction
        adView.iconView = holder.binding.adAppIcon

        holder.binding.adHeadline.text = nativeAd.headline
        holder.binding.adBody.text = nativeAd.body
        holder.binding.adCallToAction.text = nativeAd.callToAction
        nativeAd.icon?.let { holder.binding.adAppIcon.setImageDrawable(it.drawable) }

        adView.setNativeAd(nativeAd)
    }

    override fun getItemCount(): Int {
        if (posts.isEmpty()) return 0
        val adsCount = posts.size / AD_INTERVAL
        return posts.size + adsCount
    }

    fun updateData(newPosts: List<Post>) {
        posts = newPosts
        loadedAds.values.forEach { it.destroy() }
        loadedAds.clear()
        notifyDataSetChanged()
    }
}
