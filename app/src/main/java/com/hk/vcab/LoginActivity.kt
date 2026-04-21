package com.hk.vcab

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.airbnb.lottie.LottieAnimationView
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.tasks.Task
import com.google.android.material.button.MaterialButton
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.messaging.FirebaseMessaging

class LoginActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var lottieProgress: LottieAnimationView
    private lateinit var loginFormLayout: LinearLayout
    private lateinit var googleSignInClient: GoogleSignInClient
    private val db = FirebaseFirestore.getInstance()

    private val RC_SIGN_IN = 1001

    // 🔑 Hardcoded admin emails
    private val adminEmails = listOf(
        "vcabadmin1@gmail.com",
        "vcabadmin2@gmail.com"
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        auth = FirebaseAuth.getInstance()

        val emailEditText = findViewById<EditText>(R.id.emailLoginEditText)
        val passwordEditText = findViewById<EditText>(R.id.passwordLoginEditText)
        val loginButton = findViewById<Button>(R.id.loginButton)
        val registerLink = findViewById<TextView>(R.id.registerLink)
        val googleSignInButton = findViewById<MaterialButton>(R.id.btnGoogleSignIn)

        lottieProgress = findViewById(R.id.lottieProgressLogin)
        loginFormLayout = findViewById(R.id.loginFormLayout)

        lottieProgress.visibility = View.GONE
        loginFormLayout.visibility = View.VISIBLE

        // Configure Google Sign-In
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .build()

        googleSignInClient = GoogleSignIn.getClient(this, gso)

        val forgotPasswordLink = findViewById<TextView>(R.id.forgotPasswordLink)
        forgotPasswordLink.setOnClickListener {
            val email = emailEditText.text.toString().trim()
            if (email.isEmpty()) {
                Toast.makeText(this, "Please enter your email", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                Toast.makeText(this, "Invalid email format", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            auth.sendPasswordResetEmail(email)
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        Toast.makeText(
                            this,
                            "Password reset link sent to $email , CHECK SPAM MAIL TOO",
                            Toast.LENGTH_LONG
                        ).show()
                    } else {
                        Toast.makeText(
                            this,
                            "Error: ${task.exception?.message}",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
        }

        loginButton.setOnClickListener {
            hideKeyboard()

            val email = emailEditText.text.toString().trim()
            val password = passwordEditText.text.toString().trim()

            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Please enter both email and password", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                Toast.makeText(this, "Invalid email format", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            showLoading()

            auth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener { task ->
                    hideLoading()
                    if (task.isSuccessful) {
                        val userId = auth.currentUser?.uid ?: return@addOnCompleteListener

                        // Save FCM token for this user
                        FirebaseMessaging.getInstance().token.addOnSuccessListener { token ->
                            db.collection("users").document(userId)
                                .update("fcmToken", token)
                        }

                        val userEmail = auth.currentUser?.email ?: ""
                        Toast.makeText(this, "Login successful", Toast.LENGTH_SHORT).show()

                        // 🔑 Admin check here
                        if (adminEmails.contains(userEmail)) {
                            startActivity(Intent(this, AdminDashboardActivity::class.java))
                            finish()
                        } else {
                            goToDashboard(userEmail)
                        }
                    } else {
                        loginFormLayout.visibility = View.VISIBLE
                        Toast.makeText(this, "Login failed: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                    }
                }
        }

        registerLink.setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
        }

        googleSignInButton.setOnClickListener {
            // Force user to select account every time
            googleSignInClient.signOut().addOnCompleteListener {
                signInWithGoogle()
            }
        }
    }

    private fun signInWithGoogle() {
        showLoading()
        val signInIntent = googleSignInClient.signInIntent
        startActivityForResult(signInIntent, RC_SIGN_IN)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == RC_SIGN_IN) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(data)
            handleSignInResult(task)
        }
    }

    private fun handleSignInResult(completedTask: Task<GoogleSignInAccount>) {
        try {
            val account = completedTask.getResult(ApiException::class.java)!!
            // Check college email before authentication
            if (account.email?.endsWith("@vitbhopal.ac.in") == true || adminEmails.contains(account.email)) {
                firebaseAuthWithGoogle(account.idToken!!, account.email, account.displayName)
            } else {
                hideLoading()
                loginFormLayout.visibility = View.VISIBLE
                Toast.makeText(this, "Please use your college email", Toast.LENGTH_LONG).show()
                auth.signOut()
            }
        } catch (e: ApiException) {
            hideLoading()
            loginFormLayout.visibility = View.VISIBLE
            Toast.makeText(this, "Google sign in failed: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun firebaseAuthWithGoogle(idToken: String, email: String?, name: String?) {
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        auth.signInWithCredential(credential)
            .addOnCompleteListener(this) { task ->
                hideLoading()
                if (task.isSuccessful) {
                    val user = auth.currentUser ?: return@addOnCompleteListener
                    val userId = user.uid

                    // Save FCM token for Google Sign-In user
                    FirebaseMessaging.getInstance().token.addOnSuccessListener { token ->
                        db.collection("users").document(userId)
                            .update("fcmToken", token)
                    }

                    // 🔑 If Admin → skip everything else
                    if (adminEmails.contains(email)) {
                        startActivity(Intent(this, AdminDashboardActivity::class.java))
                        finish()
                        return@addOnCompleteListener
                    }

                    // Normal Google users → check if Firestore user exists
                    db.collection("users").document(userId).get()
                        .addOnSuccessListener { document ->
                            if (document.exists() && document.getString("name") != null) {
                                checkDriverAndGoToDashboard(email ?: "")
                            } else {
                                val intent = Intent(this, RegisterActivity::class.java)
                                intent.putExtra("prefill_email", email)
                                intent.putExtra("prefill_name", name)
                                startActivity(intent)
                                finish()
                            }
                        }
                        .addOnFailureListener {
                            Toast.makeText(this, "Error checking user data", Toast.LENGTH_SHORT).show()
                        }
                } else {
                    loginFormLayout.visibility = View.VISIBLE
                    Toast.makeText(this, "Google authentication failed.", Toast.LENGTH_SHORT).show()
                }
            }
    }

    private fun checkDriverAndGoToDashboard(userEmail: String) {
        db.collection("Drivers")
            .whereEqualTo("email", userEmail)
            .get()
            .addOnSuccessListener { result ->
                val intent = if (!result.isEmpty) {
                    Intent(this, DriverDashboardActivity::class.java)
                } else {
                    Intent(this, BookingActivity::class.java)
                }
                startActivity(intent)
                finish()
            }
            .addOnFailureListener {
                startActivity(Intent(this, BookingActivity::class.java))
                finish()
            }
    }

    private fun goToDashboard(userEmail: String) {
        checkDriverAndGoToDashboard(userEmail)
    }

    private fun showLoading() {
        loginFormLayout.visibility = View.GONE
        lottieProgress.visibility = View.VISIBLE
        lottieProgress.playAnimation()
    }

    private fun hideLoading() {
        lottieProgress.cancelAnimation()
        lottieProgress.visibility = View.GONE
    }

    private fun hideKeyboard() {
        val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(currentFocus?.windowToken, 0)
    }

    override fun onBackPressed() {
        super.onBackPressed()
        val intent = Intent(this, DashboardActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
        startActivity(intent)
        overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right)
        finish()
    }
}
