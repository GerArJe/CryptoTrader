package com.platzi.android.firestore.network

import java.lang.Exception

interface RealTimeDataListener<T> {

    fun onDataChange(updateData: T)

    fun onError(exeption: Exception)
}