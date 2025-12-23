package com.example.vibekey

// --- IMPORTS (DO NOT REMOVE) ---
import android.inputmethodservice.InputMethodService
import android.view.View
import android.view.LayoutInflater
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo  // <--- Fixes 'EditorInfo' error
import android.widget.Button
import android.widget.FrameLayout
import android.widget.TextView
import android.widget.LinearLayout
import android.content.ClipboardManager
import android.content.Context
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.chip.Chip // <--- Fixes 'Chip' error
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.IOException
import android.os.Handler
import android.os.Looper
// -------------------------------

class VibeInputService : InputMethodService() {

    private lateinit var mainLayout: FrameLayout
    private lateinit var dashboardView: View
    private lateinit var carouselView: View
    private lateinit var customInputView: View
    
    private var currentContext: String = "Unknown"
    private val client = OkHttpClient()

    override fun onCreateInputView(): View {
        mainLayout = FrameLayout(this)
        
        val inflater = LayoutInflater.from(this)
        // Ensure you have created these XML files in res/layout!
        dashboardView = inflater.inflate(R.layout.layout_dashboard, null)
        carouselView = inflater.inflate(R.layout.layout_carousel, null)
        customInputView = inflater.inflate(R.layout.layout_custom_input, null)

        setupDashboard()
        showDashboard()
        
        return mainLayout
    }

    // This fixes the "overrides nothing" error by adding the exact signature
    override fun onStartInputView(info: EditorInfo?, restarting: Boolean) {
        super.onStartInputView(info, restarting)
        
        val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        if (clipboard.hasPrimaryClip()) {
            val text = clipboard.primaryClip?.getItemAt(0)?.text.toString()
            if (text.isNotEmpty()) {
                updateReplyingHeader(text)
            }
        }
    }

    private fun showDashboard() {
        mainLayout.removeAllViews()
        mainLayout.addView(dashboardView)
    }

    private fun showCarousel(replies: List<String>) {
        mainLayout.removeAllViews()
        mainLayout.addView(carouselView)

        val recyclerView = carouselView.findViewById<RecyclerView>(R.id.replies_recycler)
        // Fix: Explicitly use 'this' context
        recyclerView.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        
        recyclerView.adapter = RepliesAdapter(replies) { selectedReply ->
            commitText(selectedReply)
        }
        
        val btnBack = carouselView.findViewById<Button>(R.id.btn_back)
        btnBack.setOnClickListener { showDashboard() }
    }

    private fun setupDashboard() {
        // Safe check for null IDs to prevent crashes if XML is mismatched
        val btnRoast = dashboardView.findViewById<Button>(R.id.btn_roast) ?: return
        val btnPro = dashboardView.findViewById<Button>(R.id.btn_professional) ?: return
        val btnFlirty = dashboardView.findViewById<Button>(R.id.btn_flirty) ?: return
        // Note: Make sure IDs in XML match these names exactly (btn_roast, etc.)

        btnRoast.setOnClickListener { generateReply("roast") }
        btnPro.setOnClickListener { generateReply("professional") }
        btnFlirty.setOnClickListener { generateReply("flirty") }
    }

    private fun updateReplyingHeader(text: String) {
        val header = dashboardView.findViewById<TextView>(R.id.header_text) ?: return
        val snippet = if (text.length > 20) text.substring(0, 20) + "..." else text
        header.text = "Replying to: \"$snippet\""
    }

    private fun generateReply(vibe: String) {
        val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clipText = clipboard.primaryClip?.getItemAt(0)?.text.toString()

        if (clipText.isEmpty()) return

        val json = JSONObject()
        json.put("clipboard_text", clipText)
        json.put("context_summary", "Generic") 
        json.put("vibe", vibe)

        val mediaType = "application/json; charset=utf-8".toMediaType()
        val body = json.toString().toRequestBody(mediaType)
        
        // Ensure this is 10.0.2.2 for Emulator
        val request = Request.Builder()
            .url("http://10.0.2.2:8000/generate_reply") 
            .post(body)
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                e.printStackTrace()
            }

            override fun onResponse(call: Call, response: Response) {
                response.body?.string()?.let { responseString ->
                    try {
                        val jsonResponse = JSONObject(responseString)
                        val repliesArray = jsonResponse.getJSONArray("replies")
                        val repliesList = ArrayList<String>()
                        
                        // Check if the array contains Strings or Objects
                        if (repliesArray.length() > 0) {
                             // Assuming simple list of strings for MVP
                             // If your backend returns objects like {"text": "hi"}, change this parsing
                             for (i in 0 until repliesArray.length()) {
                                 repliesList.add(repliesArray.getString(i))
                             }
                        }

                        Handler(Looper.getMainLooper()).post {
                            showCarousel(repliesList)
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }
        })
    }

    private fun commitText(text: String) {
        val ic = currentInputConnection
        ic?.commitText(text, 1)
    }
}

// --- FIXED ADAPTER CLASS ---
// This explicit definition fixes the "not abstract" error
class RepliesAdapter(
    private val replies: List<String>,
    private val onClick: (String) -> Unit
) : RecyclerView.Adapter<RepliesAdapter.ReplyViewHolder>() {

    // View Holder Class
    class ReplyViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val textView: TextView = view.findViewById(android.R.id.text1)
    }

    // Fixed onCreateViewHolder signature
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ReplyViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(android.R.layout.simple_list_item_1, parent, false)
        view.setBackgroundColor(android.graphics.Color.WHITE)
        return ReplyViewHolder(view)
    }

    override fun onBindViewHolder(holder: ReplyViewHolder, position: Int) {
        holder.textView.text = replies[position]
        holder.itemView.setOnClickListener { onClick(replies[position]) }
    }

    override fun getItemCount() = replies.size
}