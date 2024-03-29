package com.platzi.android.firestore.ui.activity

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.firestore.FirebaseFirestore
import com.platzi.android.firestore.R
import com.platzi.android.firestore.adapter.CryptosAdapter
import com.platzi.android.firestore.adapter.CryptosAdapterListener
import com.platzi.android.firestore.model.Crypto
import com.platzi.android.firestore.model.User
import com.platzi.android.firestore.network.Callback
import com.platzi.android.firestore.network.FirestoreService
import com.platzi.android.firestore.network.RealTimeDataListener
import com.squareup.picasso.Picasso
import kotlinx.android.synthetic.main.activity_trader.*
import java.lang.Exception

class TraderActivity : AppCompatActivity(), CryptosAdapterListener {

    lateinit var firestoreService: FirestoreService

    private val cryptosAdapter = CryptosAdapter(this)

    private var username: String? = null

    private var user: User? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_trader)
        firestoreService = FirestoreService(FirebaseFirestore.getInstance())

        username = intent.extras!![USERNAME_KEY]!!.toString()
        usernameTextView.text = username

        configureRecyclerView()
        loadCryptos()

        fab.setOnClickListener { view ->
            Snackbar.make(view, getString(R.string.generating_new_cryptos), Snackbar.LENGTH_SHORT)
                .setAction("Info", null).show()
            generateCryptoCurrenciesRandom()
        }

    }

    private fun generateCryptoCurrenciesRandom() {
        for(crypto in cryptosAdapter.cryptosList) {
            val amount = (1..10).random()
            crypto.available += amount  
            firestoreService.updateCrypto(crypto)
        }
    }

    private fun loadCryptos() {
        firestoreService.getCryptos(object : Callback<List<Crypto>>{
            override fun onSuccess(cryptoList: List<Crypto>?) {
                firestoreService.findUserById(username!!, object: Callback<User>{
                    override fun onSuccess(result: User?) {
                        user = result
                        if (user!!.cryptosList == null){
                            val userCryptoList = mutableListOf<Crypto>()

                            for (cryto in cryptoList!!){
                                val cryptoUser = Crypto()
                                cryptoUser.name = cryto.name
                                cryptoUser.available = cryto.available
                                cryptoUser.imageUrl = cryto.imageUrl
                                userCryptoList.add(cryptoUser)
                            }
                            user!!.cryptosList = userCryptoList
                            firestoreService.updateUser(user!!, null)
                        }
                        loadUserCryptos()
                        addRealTimeDatabaseListener(user!!, cryptoList!!)
                    }

                    override fun onFailed(exception: Exception) {
                        showGeneralServerErrorMessage()
                    }

                })

                this@TraderActivity.runOnUiThread {
                    cryptosAdapter.cryptosList = cryptoList!!
                    cryptosAdapter.notifyDataSetChanged()
                }
            }

            override fun onFailed(exception: Exception) {
                Log.e("TraderActivity", "error loading cryptos", exception)
                showGeneralServerErrorMessage()
            }

        })
    }

    private fun addRealTimeDatabaseListener(user: User, cryptoList: List<Crypto>) {

        firestoreService.listenForUpdates(user, object : RealTimeDataListener<User>{
            override fun onDataChange(updateData: User) {
                this@TraderActivity.user = updateData
                loadUserCryptos()
            }

            override fun onError(exeption: Exception) {
                showGeneralServerErrorMessage()
            }

        })

        firestoreService.listenForUpdates(cryptoList, object : RealTimeDataListener<Crypto>{
            override fun onDataChange(updatedData: Crypto) {
                var pos = 0
                for (crypto in cryptosAdapter.cryptosList){
                    if (crypto.name.equals(updatedData.name)){
                        crypto.available = updatedData.available
                        cryptosAdapter.notifyItemChanged(pos)
                    }
                    pos++
                }
            }

            override fun onError(exeption: Exception) {
                showGeneralServerErrorMessage()
            }

        })
    }

    private fun loadUserCryptos() {
        runOnUiThread {
            if (user != null && user!!.cryptosList != null){
                infoPanel.removeAllViews()
                for (crypto in user!!.cryptosList!!){
                    addUserCryptoInRow(crypto)
                }
            }
        }
    }

    private fun addUserCryptoInRow(crypto: Crypto) {
        val view = LayoutInflater.from(this).inflate(R.layout.coin_info, infoPanel, false)
        view.findViewById<TextView>(R.id.coinLabel).text =
            getString(R.string.coin_info, crypto.name, crypto.available.toString())
        Picasso.get().load(crypto.imageUrl).into(view.findViewById<ImageView>(R.id.coinIcon))
        infoPanel.addView(view)
    }

    private fun configureRecyclerView() {
        recyclerView.setHasFixedSize(true)
        val layoutManager = LinearLayoutManager(this)
        recyclerView.layoutManager = layoutManager
        recyclerView.adapter = cryptosAdapter
    }

    fun showGeneralServerErrorMessage() {
        Snackbar.make(fab, getString(R.string.error_while_connecting_to_the_server), Snackbar.LENGTH_LONG)
            .setAction("Info", null).show()
    }

    override fun onBuyCryptoClicked(crypto: Crypto) {
        if(crypto.available > 0){
            for (userCrypto in user!!.cryptosList!!){
                if (userCrypto.name == crypto.name){
                    userCrypto.available += 1
                    break
                }
            }

            crypto.available--

            firestoreService.updateUser(user!!, null)
            firestoreService.updateCrypto(crypto)
        }
    }

}