package com.hk.vcab

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.HapticFeedbackConstants
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.card.MaterialCardView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class AccountActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var imgProfile: ImageView
    private lateinit var tvName: TextView
    private lateinit var tvEmail: TextView
    private lateinit var cardEditAccount: MaterialCardView
    private lateinit var cardChangePassword: MaterialCardView
    private lateinit var cardNotifications: MaterialCardView
    private lateinit var cardRidePreferences: MaterialCardView
    private lateinit var cardHelp: MaterialCardView
    private lateinit var cardAboutUs: MaterialCardView
    private lateinit var cardPrivacyPolicy: MaterialCardView
    private lateinit var cardTerms: MaterialCardView
    private lateinit var cardShare: MaterialCardView
    private lateinit var cardLogout: MaterialCardView
    private lateinit var bottomNavigation: BottomNavigationView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.account_activity)

        auth = FirebaseAuth.getInstance()
        val db = FirebaseFirestore.getInstance()

        imgProfile = findViewById(R.id.imgProfile)
        tvName = findViewById(R.id.tvName)
        tvEmail = findViewById(R.id.tvEmail)
        cardEditAccount = findViewById(R.id.cardEditAccount)
        cardChangePassword = findViewById(R.id.cardChangePassword)
        cardNotifications = findViewById(R.id.cardNotifications)
        cardRidePreferences = findViewById(R.id.cardRidePreferences)
        cardHelp = findViewById(R.id.cardHelp)
        cardAboutUs = findViewById(R.id.cardAboutUs)
        cardPrivacyPolicy = findViewById(R.id.cardPrivacyPolicy)
        cardTerms = findViewById(R.id.cardTerms)
        cardShare = findViewById(R.id.cardShare)
        cardLogout = findViewById(R.id.cardLogout)
        bottomNavigation = findViewById(R.id.bottomNavigationView)

        // Highlight account tab
        bottomNavigation.selectedItemId = R.id.accountFragment

        // Bottom navigation listener
        bottomNavigation.setOnItemSelectedListener { item ->
            bottomNavigation.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
            when(item.itemId) {
                R.id.homeFragment -> {
                    startActivity(Intent(this, BookingActivity::class.java))
                    finish()
                    true
                }
                R.id.servicesFragment -> {
                    startActivity(Intent(this, MyRidesActivity::class.java))
                    finish()
                    true
                }
                R.id.activityFragment -> {
                    startActivity(Intent(this, MyBookingActivity::class.java))
                    finish()
                    true
                }
                R.id.accountFragment -> true
                else -> false
            }
        }

        val currentUser = auth.currentUser
        if (currentUser != null) {
            val sharedPref = getSharedPreferences("UserPrefs", MODE_PRIVATE)

            // Show cached info immediately if available
            val cachedName = sharedPref.getString("name", null)
            val cachedEmail = sharedPref.getString("email", null)
            if (!cachedName.isNullOrEmpty()) tvName.text = cachedName
            if (!cachedEmail.isNullOrEmpty()) tvEmail.text = cachedEmail

            // Fetch fresh data from Firestore
            db.collection("users").document(currentUser.uid)
                .get()
                .addOnSuccessListener { document ->
                    val name = document?.getString("name") ?: "User"
                    val email = document?.getString("email") ?: currentUser.email ?: ""

                    tvName.text = name
                    tvEmail.text = email

                    // Update SharedPreferences
                    with(sharedPref.edit()) {
                        putString("name", name)
                        putString("email", email)
                        apply()
                    }
                }
                .addOnFailureListener {
                    // Fallback if Firestore fetch fails
                    if (tvName.text.isEmpty()) tvName.text = "User"
                    if (tvEmail.text.isEmpty()) tvEmail.text = currentUser.email ?: ""
                }
        }

        imgProfile.setOnClickListener {
            Toast.makeText(this, "Profile pic clicked", Toast.LENGTH_SHORT).show()
        }

        cardEditAccount.setOnClickListener {
            startActivity(Intent(this, EditAccountActivity::class.java))
            Toast.makeText(this, "Edit Account clicked", Toast.LENGTH_SHORT).show()
        }

        cardChangePassword.setOnClickListener {
            startActivity(Intent(this, ComingSoonActivity::class.java))
        }

        cardNotifications.setOnClickListener {
            startActivity(Intent(this, ComingSoonActivity::class.java))
        }

        cardRidePreferences.setOnClickListener {
            startActivity(Intent(this, ComingSoonActivity::class.java))
        }

        cardHelp.setOnClickListener {
            try {
                val phoneNumber = "917667884004"
                val url = "https://wa.me/$phoneNumber"
                val intent = Intent(Intent.ACTION_VIEW).apply {
                    data = Uri.parse(url)
                    setPackage("com.whatsapp")
                }
                startActivity(intent)
            } catch (e: Exception) {
                Toast.makeText(this, "WhatsApp not installed", Toast.LENGTH_SHORT).show()
            }
        }

        cardAboutUs.setOnClickListener {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://harsh0904k.github.io/vcab-legal/about.html"))
            startActivity(intent)
        }

        cardPrivacyPolicy.setOnClickListener {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://harsh0904k.github.io/vcab-legal/privacy.html"))
            startActivity(intent)
        }

        cardShare.setOnClickListener {
            val appLink = "https://play.google.com/store/apps/details?id=com.hk.vcab&pcampaignid=web_share"
            val shareIntent = Intent().apply {
                action = Intent.ACTION_SEND
                putExtra(Intent.EXTRA_TEXT, "Check out this app: $appLink")
                type = "text/plain"
            }
            startActivity(Intent.createChooser(shareIntent, "Share via"))
        }

        cardLogout.setOnClickListener {
            // Clear local cache
            getSharedPreferences("UserPrefs", MODE_PRIVATE).edit().clear().apply()
            getSharedPreferences("vcab_prefs", MODE_PRIVATE)
                .edit()
                .remove("user_role")
                .putBoolean("is_first_launch", true)
                .apply()

            auth.signOut()

            val intent = Intent(this, HomeActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
        }
    }

    override fun onBackPressed() {
        super.onBackPressed()
        val intent = Intent(this, BookingActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        startActivity(intent)
        finish()
    }
}
