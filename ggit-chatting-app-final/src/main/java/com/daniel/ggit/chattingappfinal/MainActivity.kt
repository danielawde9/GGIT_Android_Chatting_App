package com.daniel.ggit.chattingappfinal

import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.*
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.preference.PreferenceManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.firebase.ui.auth.AuthUI
import com.firebase.ui.database.FirebaseRecyclerAdapter
import com.firebase.ui.database.FirebaseRecyclerOptions
import com.firebase.ui.database.SnapshotParser
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import de.hdodenhof.circleimageview.CircleImageView

class MainActivity : AppCompatActivity() {


    class MessageViewHolder(v: View?) : RecyclerView.ViewHolder(v!!) {
        var messageTextView: TextView = itemView.findViewById(R.id.messageTextView)
        var messageImageView: ImageView = itemView.findViewById(R.id.messageImageView) as ImageView
        var messengerTextView: TextView = itemView.findViewById(R.id.messengerTextView)
        var messengerImageView: CircleImageView = itemView.findViewById(R.id.messengerImageView)

    }

//    private val TAG = "MainActivity"
    val MESSAGES_CHILD = "messages"
//    private val REQUEST_INVITE = 1
//    private val REQUEST_IMAGE = 2
//    private val LOADING_IMAGE_URL = "https://www.google.com/images/spin-32.gif"
//    val DEFAULT_MSG_LENGTH_LIMIT = 10
    val ANONYMOUS = "anonymous"
//    private val MESSAGE_SENT_EVENT = "message_sent"
    private var mUsername: String? = null
    private var mPhotoUrl: String? = null
    private var mSharedPreferences: SharedPreferences? = null
//    private val MESSAGE_URL = "http://friendlychat.firebase.google.com/message/"

    private var mSendButton: Button? = null
    private var mMessageRecyclerView: RecyclerView? = null
    private var mLinearLayoutManager: LinearLayoutManager? = null
    private var mProgressBar: ProgressBar? = null
    private var mMessageEditText: EditText? = null

    // Firebase instance variables
    private var mFirebaseAuth: FirebaseAuth? = null
    private var mFirebaseUser: FirebaseUser? = null
    private var mFirebaseDatabaseReference: DatabaseReference? = null
    private var mFirebaseAdapter: FirebaseRecyclerAdapter<FriendlyMessage, MessageViewHolder>? =
        null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        mSharedPreferences = PreferenceManager.getDefaultSharedPreferences(this)
        // Set default username is anonymous.
        mUsername = ANONYMOUS
        // Initialize Firebase Auth
        mFirebaseAuth = FirebaseAuth.getInstance()
        mFirebaseUser = mFirebaseAuth!!.currentUser
        if (mFirebaseUser == null) { // Not signed in, launch the Sign In activity
            startActivity(Intent(this, SplashActivity::class.java))
            finish()
            return
        } else {
            mUsername = mFirebaseUser!!.displayName
            if (mFirebaseUser!!.photoUrl != null) {
                mPhotoUrl = mFirebaseUser!!.photoUrl.toString()
            }
        }

        // Initialize ProgressBar and RecyclerView.
        mProgressBar = findViewById(R.id.progressBar)
        mMessageRecyclerView = findViewById(R.id.messageRecyclerView)
        mLinearLayoutManager = LinearLayoutManager(this)
        mLinearLayoutManager!!.stackFromEnd = true
        mMessageRecyclerView!!.layoutManager = mLinearLayoutManager
        mFirebaseDatabaseReference = FirebaseDatabase.getInstance().reference
        val parser: SnapshotParser<FriendlyMessage?> =
            SnapshotParser { dataSnapshot ->
                val friendlyMessage = dataSnapshot.getValue(
                    FriendlyMessage::class.java
                )
                if (friendlyMessage != null) {
                    friendlyMessage.id = dataSnapshot.key
                }
                friendlyMessage!!
            }


        val messagesRef: DatabaseReference = mFirebaseDatabaseReference!!.child(MESSAGES_CHILD)
        val options: FirebaseRecyclerOptions<FriendlyMessage> =
            FirebaseRecyclerOptions.Builder<FriendlyMessage>()
                .setQuery(messagesRef, parser)
                .build()

        mFirebaseAdapter = object :
            FirebaseRecyclerAdapter<FriendlyMessage, MessageViewHolder>(
                options
            ) {
            override fun onCreateViewHolder(
                viewGroup: ViewGroup,
                i: Int
            ): MessageViewHolder {
                val inflater = LayoutInflater.from(viewGroup.context)
                return MessageViewHolder(
                    inflater.inflate(
                        R.layout.item_message,
                        viewGroup,
                        false
                    )
                )
            }

            override fun onBindViewHolder(
                viewHolder: MessageViewHolder,
                position: Int,
                friendlyMessage: FriendlyMessage
            ) {
                mProgressBar!!.visibility = ProgressBar.INVISIBLE
                if (friendlyMessage.text != null) {
                    viewHolder.messageTextView.text = friendlyMessage.text
                    viewHolder.messageTextView.visibility = TextView.VISIBLE
                    viewHolder.messageImageView.visibility = ImageView.GONE
                }
                viewHolder.messengerTextView.text = friendlyMessage.name
                if (friendlyMessage.photoUrl == null) {
                    viewHolder.messengerImageView.setImageDrawable(
                        ContextCompat.getDrawable(
                            this@MainActivity,
                            R.drawable.ic_account_circle_black
                        )
                    )
                } else {
                    Glide.with(this@MainActivity)
                        .load(friendlyMessage.photoUrl)
                        .into(viewHolder.messengerImageView)
                }
            }
        }


        mFirebaseAdapter!!.registerAdapterDataObserver(object : RecyclerView.AdapterDataObserver() {
            override fun onItemRangeInserted(positionStart: Int, itemCount: Int) {
                super.onItemRangeInserted(positionStart, itemCount)
                val friendlyMessageCount: Int = mFirebaseAdapter!!.itemCount
                val lastVisiblePosition: Int =
                    mLinearLayoutManager!!.findLastCompletelyVisibleItemPosition()
                // If the recycler view is initially being loaded or the
                // user is at the bottom of the list, scroll to the bottom
                // of the list to show the newly added message.
                if (lastVisiblePosition == -1 ||
                    positionStart >= friendlyMessageCount - 1 &&
                    lastVisiblePosition == positionStart - 1
                ) {
                    mMessageRecyclerView!!.scrollToPosition(positionStart)
                }
            }
        })

        mMessageRecyclerView?.adapter = mFirebaseAdapter
        mMessageEditText = findViewById(R.id.messageEditText)
        mMessageEditText!!.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(
                charSequence: CharSequence,
                i: Int,
                i1: Int,
                i2: Int
            ) {
            }

            override fun onTextChanged(
                charSequence: CharSequence,
                i: Int,
                i1: Int,
                i2: Int
            ) {
                mSendButton!!.isEnabled = charSequence.toString().trim { it <= ' ' }.isNotEmpty()
            }

            override fun afterTextChanged(editable: Editable) {}
        })
        mSendButton = findViewById(R.id.sendButton)
        mSendButton!!.setOnClickListener {
            val friendlyMessage = mUsername?.let {
                mPhotoUrl?.let { it1 ->
                    FriendlyMessage(
                        mMessageEditText!!.text.toString(),
                        it,
                        it1
                    )
                }
            }
            mFirebaseDatabaseReference!!.child(MESSAGES_CHILD)
                .push().setValue(friendlyMessage)
            mMessageEditText!!.setText("")
        }
    }


    override fun onPause() {
        mFirebaseAdapter!!.stopListening()
        super.onPause()
    }

    override fun onResume() {
        super.onResume()
        mFirebaseAdapter!!.startListening()
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        val inflater = menuInflater
        inflater.inflate(R.menu.main_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.sign_out_menu -> {
                signOut()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun signOut() {
        // [START auth_fui_signout]
        AuthUI.getInstance()
            .signOut(this)
            .addOnCompleteListener {
                finishAffinity()
            }
        // [END auth_fui_signout]
    }



}
