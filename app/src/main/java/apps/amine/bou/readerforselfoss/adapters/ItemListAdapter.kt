package apps.amine.bou.readerforselfoss.adapters


import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Color
import android.net.Uri
import android.support.constraint.ConstraintLayout
import android.support.design.widget.Snackbar
import android.support.v4.graphics.drawable.RoundedBitmapDrawableFactory
import android.support.v7.widget.RecyclerView
import android.text.Html
import android.text.format.DateUtils
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*

import com.amulyakhare.textdrawable.TextDrawable
import com.amulyakhare.textdrawable.util.ColorGenerator
import com.bumptech.glide.Glide
import com.bumptech.glide.request.target.BitmapImageViewTarget
import com.like.LikeButton
import com.like.OnLikeListener
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.*

import apps.amine.bou.readerforselfoss.R
import apps.amine.bou.readerforselfoss.api.selfoss.Item
import apps.amine.bou.readerforselfoss.api.selfoss.SelfossApi
import apps.amine.bou.readerforselfoss.api.selfoss.SuccessResponse
import apps.amine.bou.readerforselfoss.utils.buildCustomTabsIntent
import apps.amine.bou.readerforselfoss.utils.customtabs.CustomTabActivityHelper
import apps.amine.bou.readerforselfoss.utils.openItemUrl



class ItemListAdapter(private val app: Activity,
                      private val items: ArrayList<Item>,
                      private val api: SelfossApi,
                      private val helper: CustomTabActivityHelper,
                      private val clickBehavior: Boolean,
                      private val internalBrowser: Boolean,
                      private val articleViewer: Boolean) : RecyclerView.Adapter<ItemListAdapter.ViewHolder>() {
    private val generator: ColorGenerator = ColorGenerator.MATERIAL
    private val c: Context = app.applicationContext
    private val bars: ArrayList<Boolean> = ArrayList(Collections.nCopies(items.size + 1, false))

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val v = LayoutInflater.from(c).inflate(R.layout.list_item, parent, false) as ConstraintLayout
        return ViewHolder(v)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val itm = items[position]


        holder.saveBtn.isLiked = itm.starred
        holder.title.text = Html.fromHtml(itm.title)

        var sourceAndDate = itm.sourcetitle
        val d: Long
        try {
            d = SimpleDateFormat("yyyy-MM-dd HH:mm:ss").parse(itm.datetime).time
            sourceAndDate += " " + DateUtils.getRelativeTimeSpanString(
                d,
                Date().time,
                DateUtils.MINUTE_IN_MILLIS,
                DateUtils.FORMAT_ABBREV_RELATIVE
            )
        } catch (e: ParseException) {
            e.printStackTrace()
        }

        holder.sourceTitleAndDate.text = sourceAndDate

        if (itm.getThumbnail(c).isEmpty()) {
            val sizeInInt = 46
            val sizeInDp = TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP, sizeInInt.toFloat(), c.resources
                .displayMetrics).toInt()

            val marginInInt = 16
            val marginInDp = TypedValue.applyDimension(
                    TypedValue.COMPLEX_UNIT_DIP, marginInInt.toFloat(), c.resources
                    .displayMetrics).toInt()

            val params = holder.sourceImage.layoutParams as ViewGroup.MarginLayoutParams
            params.height = sizeInDp
            params.width = sizeInDp
            params.setMargins(marginInDp, 0, 0, 0)
            holder.sourceImage.layoutParams = params

            if (itm.getIcon(c).isEmpty()) {
                val color = generator.getColor(itm.sourcetitle)
                val textDrawable = StringBuilder()
                for (s in itm.sourcetitle.split(" ".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()) {
                    textDrawable.append(s[0])
                }

                val builder = TextDrawable.builder().round()

                val drawable = builder.build(textDrawable.toString(), color)
                holder.sourceImage.setImageDrawable(drawable)
            } else {

                val fHolder = holder
                Glide.with(c).load(itm.getIcon(c)).asBitmap().centerCrop().into(object : BitmapImageViewTarget(holder.sourceImage) {
                    override fun setResource(resource: Bitmap) {
                        val circularBitmapDrawable = RoundedBitmapDrawableFactory.create(c.resources, resource)
                        circularBitmapDrawable.isCircular = true
                        fHolder.sourceImage.setImageDrawable(circularBitmapDrawable)
                    }
                })
            }
        } else {
            Glide.with(c).load(itm.getThumbnail(c)).asBitmap().centerCrop().into(holder.sourceImage)
        }

        if (bars[position]) holder.actionBar.visibility = View.VISIBLE else holder.actionBar.visibility = View.GONE

        holder.saveBtn.isLiked = itm.starred
    }

    override fun getItemCount(): Int = items.size


    private fun doUnmark(i: Item, position: Int) {
        val s = Snackbar
                .make(app.findViewById(R.id.coordLayout), R.string.marked_as_read, Snackbar.LENGTH_LONG)
                .setAction(R.string.undo_string) {
                    items.add(position, i)
                    notifyItemInserted(position)

                    api.unmarkItem(i.id).enqueue(object : Callback<SuccessResponse> {
                        override fun onResponse(call: Call<SuccessResponse>, response: Response<SuccessResponse>) {}

                        override fun onFailure(call: Call<SuccessResponse>, t: Throwable) {
                            items.remove(i)
                            notifyItemRemoved(position)
                            doUnmark(i, position)
                        }
                    })
                }

        val view = s.view
        val tv = view.findViewById(android.support.design.R.id.snackbar_text) as TextView
        tv.setTextColor(Color.WHITE)
        s.show()
    }

    fun removeItemAtIndex(position: Int) {

        val i = items[position]

        items.remove(i)
        notifyItemRemoved(position)

        api.markItem(i.id).enqueue(object : Callback<SuccessResponse> {
            override fun onResponse(call: Call<SuccessResponse>, response: Response<SuccessResponse>) {
                doUnmark(i, position)

            }

            override fun onFailure(call: Call<SuccessResponse>, t: Throwable) {
                Toast.makeText(app, app.getString(R.string.cant_mark_read), Toast.LENGTH_SHORT).show()
                items.add(i)
                notifyItemInserted(position)
            }
        })

    }

    inner class ViewHolder(val mView: ConstraintLayout) : RecyclerView.ViewHolder(mView) {
        lateinit var saveBtn: LikeButton
        lateinit var browserBtn: ImageButton
        lateinit var shareBtn: ImageButton
        lateinit var actionBar: RelativeLayout
        lateinit var sourceImage: ImageView
        lateinit var title: TextView
        lateinit var sourceTitleAndDate: TextView

        init {
            handleClickListeners()
            handleCustomTabActions()
        }

        private fun handleClickListeners() {
            actionBar = mView.findViewById(R.id.actionBar) as RelativeLayout
            sourceImage = mView.findViewById(R.id.itemImage) as ImageView
            title = mView.findViewById(R.id.title) as TextView
            sourceTitleAndDate = mView.findViewById(R.id.sourceTitleAndDate) as TextView
            saveBtn = mView.findViewById(R.id.favButton) as LikeButton
            shareBtn = mView.findViewById(R.id.shareBtn) as ImageButton
            browserBtn = mView.findViewById(R.id.browserBtn) as ImageButton


            saveBtn.setOnLikeListener(object : OnLikeListener {
                override fun liked(likeButton: LikeButton) {
                    val (id) = items[adapterPosition]
                    api.starrItem(id).enqueue(object : Callback<SuccessResponse> {
                        override fun onResponse(call: Call<SuccessResponse>, response: Response<SuccessResponse>) {}

                        override fun onFailure(call: Call<SuccessResponse>, t: Throwable) {
                            saveBtn.isLiked = false
                            Toast.makeText(c, R.string.cant_mark_favortie, Toast.LENGTH_SHORT).show()
                        }
                    })
                }

                override fun unLiked(likeButton: LikeButton) {
                    val (id) = items[adapterPosition]
                    api.unstarrItem(id).enqueue(object : Callback<SuccessResponse> {
                        override fun onResponse(call: Call<SuccessResponse>, response: Response<SuccessResponse>) {}

                        override fun onFailure(call: Call<SuccessResponse>, t: Throwable) {
                            saveBtn.isLiked = true
                            Toast.makeText(c, R.string.cant_unmark_favortie, Toast.LENGTH_SHORT).show()
                        }
                    })
                }
            })

            shareBtn.setOnClickListener {
                val i = items[adapterPosition]
                val sendIntent = Intent()
                sendIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                sendIntent.action = Intent.ACTION_SEND
                sendIntent.putExtra(Intent.EXTRA_TEXT, i.getLinkDecoded())
                sendIntent.type = "text/plain"
                c.startActivity(Intent.createChooser(sendIntent, c.getString(R.string.share)).setFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            }

            browserBtn.setOnClickListener {
                val i = items[adapterPosition]
                val intent = Intent(Intent.ACTION_VIEW)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                intent.data = Uri.parse(i.getLinkDecoded())
                c.startActivity(intent)
            }
        }


        private fun handleCustomTabActions() {
            val customTabsIntent = buildCustomTabsIntent(c)
            helper.bindCustomTabsService(app)


            if (!clickBehavior) {
                mView.setOnClickListener {
                    openItemUrl(items[adapterPosition],
                        customTabsIntent,
                        internalBrowser,
                        articleViewer,
                        app,
                        c)
                }
                mView.setOnLongClickListener {
                    actionBarShowHide()
                    true
                }
            } else {
                mView.setOnClickListener { actionBarShowHide() }
                mView.setOnLongClickListener {
                    openItemUrl(items[adapterPosition],
                        customTabsIntent,
                        internalBrowser,
                        articleViewer,
                        app,
                        c)
                    true
                }
            }
        }

        private fun actionBarShowHide() {
            bars[adapterPosition] = true
            if (actionBar.visibility == View.GONE) actionBar.visibility = View.VISIBLE else actionBar.visibility = View.GONE
        }
    }
}
