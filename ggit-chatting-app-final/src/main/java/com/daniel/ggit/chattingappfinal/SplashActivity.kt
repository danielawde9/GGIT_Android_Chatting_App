package com.daniel.ggit.chattingappfinal

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.firebase.ui.auth.AuthUI
import com.firebase.ui.auth.IdpResponse
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser

class SplashActivity : AppCompatActivity() {
    // initilize the firebase authentication system
    private var auth: FirebaseAuth? = null
    // initilize the user
    private var currentUser: FirebaseUser? = null
    // Sign In Number
    private val RC_SIGN_IN = 123

    override fun onCreate(savedInstanceState: Bundle?) {
        // super function that needs to be called
        super.onCreate(savedInstanceState)

        // get the user authentication profile
        auth = FirebaseAuth.getInstance()
        // get the current user saved on the device
        currentUser = auth!!.currentUser

        // if the user exist
        if (auth!!.currentUser != null) {
            // already signed in go to MainActivity
            startActivity(Intent(this, MainActivity::class.java))
        }
        // user is not logged in
        else {
            // go to function create signInIntent
            createSignInIntent()
        }
    }

    private fun createSignInIntent() {
        // [START auth_fui_create_intent]
        // Choose authentication providers
        val providers = arrayListOf(
            AuthUI.IdpConfig.EmailBuilder().build(),
            AuthUI.IdpConfig.GoogleBuilder().build()
        )
        // Create and launch sign-in intent
        startActivityForResult(
            AuthUI.getInstance()
                .createSignInIntentBuilder()
                .setAvailableProviders(providers)
                .build(),
            RC_SIGN_IN
        )
        // [END auth_fui_create_intent]
    }

    // [START auth_fui_result]
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == RC_SIGN_IN) {
            val response = IdpResponse.fromResultIntent(data)

            if (resultCode == Activity.RESULT_OK) {
                // Successfully signed in go to Main
                startActivity(Intent(this, MainActivity::class.java))
            } else {
                // Sign in failed. If response is null the user canceled the
                // sign-in flow using the back button. Otherwise check
                // response.getError().getErrorCode() and handle the error.
                // ...
            }
        }
    }
    // [END auth_fui_result]


}
