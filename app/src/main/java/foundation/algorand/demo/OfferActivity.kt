package foundation.algorand.demo

import android.content.Intent
import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import foundation.algorand.auth.connect.AuthMessage
import foundation.algorand.auth.connect.SignalClient
import foundation.algorand.demo.databinding.ActivityOfferBinding
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import org.json.JSONObject


/**
 * Offer Activity
 *
 * This activity is an example of displaying an offer to the users.
 * It demonstrates the context of Android<->Android communication.
 */
class OfferActivity : AppCompatActivity() {
    companion object {
        const val TAG = "OfferActivity"
    }
    private val viewModel: OfferViewModel by viewModels()
    private lateinit var binding: ActivityOfferBinding

    private val cookieJar = Cookies()

    // Third Party APIs
    private var httpClient = OkHttpClient.Builder()
        .cookieJar(cookieJar)
        .build()

    private lateinit var signalClient: SignalClient

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val url = "https://liquid-auth.onrender.com"
        val requestId = SignalClient.generateRequestId()
        signalClient = SignalClient(url, this@OfferActivity, httpClient)
        binding = ActivityOfferBinding.inflate(layoutInflater)

        binding.lifecycleOwner = this
        binding.viewModel = viewModel
        binding.switchButton.setOnClickListener {
            val myIntent = Intent(this, MainActivity::class.java)
            myIntent.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
            startActivity(myIntent)
        }
        binding.qrCodeImageView.setImageBitmap(signalClient.qrCode(requestId, BitmapFactory.decodeResource(getResources(), R.mipmap.ic_launcher_round)))
        viewModel.setMessage(AuthMessage(url, requestId))
        lifecycleScope.launch {
            val dc = signalClient.peer(requestId, "offer")
            Log.d(TAG, "Data Channel: $dc")
            signalClient.handleDataChannel(dc!!, {
                // TODO: AVM Provider Handler
                Log.e(TAG, "onMessage($it)")
                runOnUiThread {
                    Toast.makeText(this@OfferActivity, it, Toast.LENGTH_LONG).show()
                }
                try {
                    val message = JSONObject(it)
                    val wallet = message.get("address").toString()
                    viewModel.setAddress(wallet)
                } catch (e: Exception) {
                    Log.e(TAG, "Error: $e")
                }
            },{
                Log.d(TAG, "onStateChange($it)")
            })
        }
        setContentView(binding.root)
    }
}
