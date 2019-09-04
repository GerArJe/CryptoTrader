package com.platzi.android.firestore.network

import com.google.firebase.firestore.FirebaseFirestore
import com.platzi.android.firestore.model.Crypto
import com.platzi.android.firestore.model.User

const val CRYTO_COLLECTION_NAME = "cryptos"
const val USER_COLLECTION_NAME = "users"

class FirestoreService (val firebaseFirestore: FirebaseFirestore){
    fun setDocument(data: Any, collectionName: String, id: String, callback: Callback<Void>){
        firebaseFirestore.collection(collectionName)
            .document(id).set(data)
            .addOnSuccessListener { callback.onSuccess(null) }
            .addOnFailureListener { exception ->  callback.onFailed(exception) }
    }

    fun updateUser(user : User, callback: Callback<User>?){
        firebaseFirestore.collection(USER_COLLECTION_NAME).document(user.username)
            .update("cryptosList", user.cryptosList)
            .addOnSuccessListener { result ->
                if (callback != null){
                    callback.onSuccess(user)
                }
            }
            .addOnFailureListener { exeption -> callback!!.onFailed(exeption)}
    }

    fun updateCrypto(crypto: Crypto){
        firebaseFirestore.collection(CRYTO_COLLECTION_NAME).document(crypto.getDocumentId())
            .update("available", crypto.available)
    }
}