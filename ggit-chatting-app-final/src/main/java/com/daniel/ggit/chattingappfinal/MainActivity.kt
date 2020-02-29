package com.daniel.ggit.chattingappfinal

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.*
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
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


    // this is where we link our recycler view data to our UI
    // then we will use this to populate the recycler view
    class MessageViewHolder(v: View?) : RecyclerView.ViewHolder(v!!) {
        var messageTextView: TextView = itemView.findViewById(R.id.messageTextView)
        var messageImageView: ImageView = itemView.findViewById(R.id.messageImageView) as ImageView
        var messengerTextView: TextView = itemView.findViewById(R.id.messengerTextView)
        var messengerImageView: CircleImageView = itemView.findViewById(R.id.messengerImageView)
    }

    val MESSAGES_CHILD = "messages"
    val ANONYMOUS = "anonymous"

    private var mUsername: String? = null
    private var mPhotoUrl: String? = null

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

        // Set default username is anonymous.
        mUsername = ANONYMOUS
        // Initialize Firebase Auth
        mFirebaseAuth = FirebaseAuth.getInstance()
        mFirebaseUser = mFirebaseAuth!!.currentUser
        validateFirebaseUser()

        // Initialize ProgressBar and RecyclerView.
        mProgressBar = findViewById(R.id.progressBar)
        mMessageRecyclerView = findViewById(R.id.messageRecyclerView)
        // we want to show the recycler view as linear layout
        // text messages show under each other
        mLinearLayoutManager = LinearLayoutManager(this)
        // keep showing the end
        mLinearLayoutManager!!.stackFromEnd = true
        mMessageRecyclerView!!.layoutManager = mLinearLayoutManager

        // connect to firebase database
        mFirebaseDatabaseReference = FirebaseDatabase.getInstance().reference

        // get the data and parse it using our model
        val parser: SnapshotParser<FriendlyMessage?> =
            SnapshotParser { dataSnapshot ->

                // parse the data taken using our model
                val friendlyMessage = dataSnapshot.getValue(FriendlyMessage::class.java)
                // set the messages id from the spanchot key (generated from fireabase)
                if (friendlyMessage != null) {
                    friendlyMessage.id = dataSnapshot.key
                }

                friendlyMessage!!
            }

        // initialze the messages
        val messagesRef: DatabaseReference = mFirebaseDatabaseReference!!.child(MESSAGES_CHILD)
        // create the query using the messages
        val options: FirebaseRecyclerOptions<FriendlyMessage> =
            FirebaseRecyclerOptions.Builder<FriendlyMessage>()
                .setQuery(messagesRef, parser)
                .build()

        // set the query and save and infate the result with the item_message
        mFirebaseAdapter = object :
            FirebaseRecyclerAdapter<FriendlyMessage, MessageViewHolder>(options) {
            override fun onCreateViewHolder(viewGroup: ViewGroup, i: Int): MessageViewHolder {
                val inflater = LayoutInflater.from(viewGroup.context)
                return MessageViewHolder(
                    inflater.inflate(
                        R.layout.item_message,
                        viewGroup,
                        false
                    )
                )
            }

            // bind each item to the corresponding item in the xml
            override fun onBindViewHolder(
                viewHolder: MessageViewHolder,
                position: Int,
                friendlyMessage: FriendlyMessage
            ) {
                // clear the progress bar
                mProgressBar!!.visibility = ProgressBar.INVISIBLE
                // set the message text from the db into the text in the layout
                viewHolder.messageTextView.text = friendlyMessage.text
                // set the visibilty to visible (cause initialy its hidden and we showed the
                // loading circle
                viewHolder.messageTextView.visibility = TextView.VISIBLE
                // the viewHolder.ITEM is from the MessageViewHolder from the top
                viewHolder.messageImageView.visibility = ImageView.GONE
                // thats what we called MVC model, view, controller
                viewHolder.messengerTextView.text = friendlyMessage.name
                // glide is used to set the user pic into the pic
                Glide.with(this@MainActivity)
                    .load(friendlyMessage.photoUrl)
                    .into(viewHolder.messengerImageView)
            }
        }


        // set the firebase adapter
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

        // set the recycler view adapter from the firebase adapter
        mMessageRecyclerView?.adapter = mFirebaseAdapter

        // link the edit text from the xml
        mMessageEditText = findViewById(R.id.messageEditText)
        // optional set the send button to enabled when the user starts typing
        mMessageEditText!!.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(charSequence: CharSequence, i: Int, i1: Int, i2: Int) {}

            // theres 3 states before and after text changing,
            // we only wants when the user starts typing
            override fun onTextChanged(charSequence: CharSequence, i: Int, i1: Int, i2: Int) {
                // it will set the button to enabled if the characters are not empty or space
                mSendButton!!.isEnabled = charSequence.toString().trim { it <= ' ' }.isNotEmpty()
            }

            override fun afterTextChanged(editable: Editable) {}
        })


        // link the sendButton from the xml
        mSendButton = findViewById(R.id.sendButton)
        // create listener to that button
        mSendButton!!.setOnClickListener {
            // if username and photoUrl not empty
            val friendlyMessage = mUsername?.let {
                mPhotoUrl?.let { it1 ->
                    // create the message using our model
                    FriendlyMessage(
                        mMessageEditText!!.text.toString(),
                        it,
                        it1
                    )
                }
            }

            // send the message to the firebase database
            mFirebaseDatabaseReference!!.child(MESSAGES_CHILD)
                .push().setValue(friendlyMessage)

            // clear the edit text after sending
            mMessageEditText!!.setText("")
        }
    }

    private fun validateFirebaseUser() {
        // Not signed in, launch the Sign In activity
        if (mFirebaseUser == null) {
            startActivity(Intent(this, SplashActivity::class.java))
            finish()
            return
        } else {
            // get the user display name and save it into variable mUsername
            mUsername = mFirebaseUser!!.displayName
            // if theres profile pic
            if (mFirebaseUser!!.photoUrl != null) {
                mPhotoUrl = mFirebaseUser!!.photoUrl.toString()
            }
        }
    }


    // on pause stop the firebase adapter listener
    override fun onPause() {
        mFirebaseAdapter!!.stopListening()
        super.onPause()
    }

    // if the user re enter the app it will resume the lifecyle
    // restart listening
    override fun onResume() {
        super.onResume()
        mFirebaseAdapter!!.startListening()
    }

    // inflate the menu in the top bar
    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        val inflater = menuInflater
        inflater.inflate(R.menu.main_menu, menu)
        return true
    }

    // if the user selected the signout option > signout
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

