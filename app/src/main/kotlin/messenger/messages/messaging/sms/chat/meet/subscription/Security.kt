package messenger.messages.messaging.sms.chat.meet.subscription

import android.text.TextUtils
import android.util.Base64
import android.util.Log
import java.security.*
import java.security.spec.InvalidKeySpecException
import java.security.spec.X509EncodedKeySpec

internal object Security {
    private const val TAG = "IABUtil/Security"
    private const val KEY_FACTORY_ALGORITHM = "RSA"
    private const val SIGNATURE_ALGORITHM = "SHA1withRSA"
    fun verifyPurchase(
        base64PublicKey: String?,
        signedData: String, signature: String?
    ): Boolean {
        if (TextUtils.isEmpty(signedData) || TextUtils.isEmpty(base64PublicKey) ||
            TextUtils.isEmpty(signature)
        ) {

            Log.e(TAG, "Purchase verification failed: missing data.")
            return false
        }
        val key = generatePublicKey(base64PublicKey)
        return verify(key, signedData, signature)
    }


    fun generatePublicKey(encodedPublicKey: String?): PublicKey {
        return try {
            val decodedKey = Base64.decode(encodedPublicKey, Base64.DEFAULT)
            val keyFactory = KeyFactory.getInstance(KEY_FACTORY_ALGORITHM)
            keyFactory.generatePublic(X509EncodedKeySpec(decodedKey))
        } catch (e: NoSuchAlgorithmException) {
            throw RuntimeException(e)
        } catch (e: InvalidKeySpecException) {
            Log.e(TAG, "Invalid key specification.")
            throw IllegalArgumentException(e)
        } catch (e: IllegalArgumentException) {
            Log.e(TAG, "Base64 decoding failed.")
            throw e
        }
    }

    fun verify(publicKey: PublicKey?, signedData: String, signature: String?): Boolean {
        val sig: Signature
        try {
            sig = Signature.getInstance(SIGNATURE_ALGORITHM)
            sig.initVerify(publicKey)
            sig.update(signedData.toByteArray())
            if (!sig.verify(Base64.decode(signature, Base64.DEFAULT))) {
                Log.e(TAG, "Signature verification failed.")
                return false
            }
            return true
        } catch (e: NoSuchAlgorithmException) {
            Log.e(TAG, "NoSuchAlgorithmException.")
        } catch (e: InvalidKeyException) {
            Log.e(TAG, "Invalid key specification.")
        } catch (e: SignatureException) {
            Log.e(TAG, "Signature exception.")
        } catch (e: IllegalArgumentException) {
            Log.e(TAG, "Base64 decoding failed.")
        }
        return false
    }
}
