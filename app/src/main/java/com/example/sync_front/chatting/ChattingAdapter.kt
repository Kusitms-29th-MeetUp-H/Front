import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.sync_front.R
import com.example.sync_front.api_server.RoomMessageElementResponseDto
import com.example.sync_front.databinding.ItemChattingMeBinding
import com.example.sync_front.databinding.ItemChattingOtherBinding
import java.text.SimpleDateFormat
import java.util.*

class MyChattingViewHolder(val binding: ItemChattingMeBinding) : RecyclerView.ViewHolder(binding.root)
class OtherChattingViewHolder(val binding: ItemChattingOtherBinding) : RecyclerView.ViewHolder(binding.root)

class ChattingAdapter(private var itemList: MutableList<RoomMessageElementResponseDto>, private val myName: String? = null):
    RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val layoutInflater = LayoutInflater.from(parent.context)
        if (viewType == 0) {
            return MyChattingViewHolder(ItemChattingMeBinding.inflate(layoutInflater))
        } else {
            return OtherChattingViewHolder(ItemChattingOtherBinding.inflate(layoutInflater))
        }
    }

    override fun getItemViewType(position: Int): Int {
        val data = itemList[position]
        return if (data?.user?.name == myName) {
            0 // 로그인한 사용자의 이름과 일치하는 경우 (내 채팅)
        } else {
            1 // 로그인한 사용자의 이름과 다른 경우 (다른 사람 채팅)
        }
    }

    override fun getItemCount(): Int {
        return itemList.size
    }

    fun updateData(newList: RoomMessageElementResponseDto) {
        itemList.add(newList)
        notifyDataSetChanged()
    }

    override fun onBindViewHolder(holder:RecyclerView.ViewHolder, position: Int) {
        val data = itemList[position]

        if (holder is MyChattingViewHolder && data != null) {
            holder.binding.run {
                chattingMessage.text = data.content
                messageTime.text = formatTime(data.time)
                chattingMessage.gravity = Gravity.RIGHT
            }

            val image = data.image

            if (image != "null") {
                Log.d("my log", "이미지 존재?1")
                holder.binding.image.visibility = View.VISIBLE
                Glide.with(holder.itemView.context)
                    .load(data.image)
                    .into(holder.binding.image)
            } else {
                holder.binding.image.visibility = View.GONE
            }

        } else if (holder is OtherChattingViewHolder && data != null) {
            holder.binding.run {
                chattingMessage.gravity = Gravity.LEFT
                chattingUser.text = data.user.name
                chattingMessage.text = data.content
                messageTime.text = formatTime(data.time)

                val image = data.image

                if (image != "null") {
                    Log.d("my log", "이미지 존재?2")
                    holder.binding.image.visibility = View.VISIBLE
                    Glide.with(holder.itemView.context)
                        .load(data.image)
                        .into(holder.binding.image)
                } else {
                    holder.binding.image.visibility = View.GONE
                }

                if (!data.user.profile.isNullOrEmpty()) {
                    Glide.with(holder.itemView.context)
                        .load(data.user.profile)
                        .placeholder(R.drawable.img_profile_default)
                        .error(R.drawable.img_profile_default)
                        .into(holder.binding.profile)
                } else {
                    profile.setImageResource(R.drawable.img_profile_default)
                }

                if (data.user.isOwner) {
                    owner.visibility = View.VISIBLE
                } else {
                    owner.visibility = View.GONE
                }
            }
        }
    }

    private fun formatTime(time: String): String {
        val inputFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS", Locale.getDefault())
        val outputFormat = SimpleDateFormat("HH:mm", Locale.getDefault())

        val date = inputFormat.parse(time)
        return outputFormat.format(date)
    }

    fun setData(list: List<RoomMessageElementResponseDto>) {
        itemList.clear()
        itemList.addAll(list)
        notifyDataSetChanged()
    }
}