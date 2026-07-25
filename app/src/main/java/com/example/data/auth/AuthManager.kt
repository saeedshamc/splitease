package com.example.data.auth

import android.content.Context
import android.util.Log
import com.example.data.model.User
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.UserProfileChangeRequest
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.tasks.await

class AuthManager(private val context: Context) {
    private var firebaseAuth: FirebaseAuth? = null
    
    private val _currentUser = MutableStateFlow<User?>(null)
    val currentUser: StateFlow<User?> = _currentUser.asStateFlow()

    init {
        try {
            if (FirebaseApp.getApps(context).isNotEmpty()) {
                firebaseAuth = FirebaseAuth.getInstance()
                updateUserState(firebaseAuth?.currentUser)
            } else {
                Log.w("AuthManager", "FirebaseApp not initialized. Using offline guest fallback.")
                loginAsGuest("سحر (کاربر مهمان) 👤")
            }
        } catch (e: Exception) {
            Log.e("AuthManager", "Error initializing FirebaseAuth", e)
            loginAsGuest("سحر (کاربر مهمان) 👤")
        }
    }

    private fun updateUserState(fbUser: FirebaseUser?) {
        if (fbUser != null) {
            _currentUser.value = User(
                uid = fbUser.uid,
                email = fbUser.email ?: "",
                displayName = fbUser.displayName ?: fbUser.email?.substringBefore("@") ?: "User",
                photoUrl = fbUser.photoUrl?.toString(),
                isOnlineAuth = true
            )
        } else {
            _currentUser.value = null
        }
    }
    
    suspend fun signUp(email: String, pass: String, displayName: String): Result<User> {
        return try {
            val auth = firebaseAuth ?: return Result.failure(Exception("Firebase Auth not available offline. You can use guest/offline mode."))
            val res = auth.createUserWithEmailAndPassword(email, pass).await()
            res.user?.let { u ->
                try {
                    val profileUpdates = UserProfileChangeRequest.Builder()
                        .setDisplayName(displayName)
                        .build()
                    u.updateProfile(profileUpdates).await()
                } catch (e: Exception) {
                    Log.w("AuthManager", "Profile update failed", e)
                }
                val user = User(
                    uid = u.uid,
                    email = u.email ?: email,
                    displayName = displayName,
                    isOnlineAuth = true
                )
                _currentUser.value = user
                Result.success(user)
            } ?: Result.failure(Exception("User creation failed"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun login(email: String, pass: String): Result<User> {
        return try {
            val auth = firebaseAuth ?: return Result.failure(Exception("Firebase Auth not available offline. You can use guest/offline mode."))
            val res = auth.signInWithEmailAndPassword(email, pass).await()
            res.user?.let { u ->
                updateUserState(u)
                _currentUser.value?.let { Result.success(it) } ?: Result.failure(Exception("Login failed"))
            } ?: Result.failure(Exception("Login failed"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun logout() {
        try {
            firebaseAuth?.signOut()
        } catch (e: Exception) {
            Log.e("AuthManager", "Sign out error", e)
        }
        _currentUser.value = null
    }
    
    fun loginAsGuest(name: String = "کاربر مهمان (آفلاین) 👤") {
        _currentUser.value = User(
            uid = "local_guest_" + System.currentTimeMillis(),
            email = "guest@splitease.local",
            displayName = name,
            isOnlineAuth = false
        )
    }
}
