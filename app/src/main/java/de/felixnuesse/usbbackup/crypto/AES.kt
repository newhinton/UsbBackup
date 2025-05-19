package de.felixnuesse.usbbackup.crypto

import java.io.File
import java.io.OutputStream
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.SecretKey
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec


/**
 * Source: https://www.baeldung.com/kotlin/advanced-encryption-standard
 */
class AES {

    companion object {
        private var KEY_ITERATIONS = 50000
        private var KEY_SZE = 256

        private fun getSalt(): ByteArray {
            val salt = ByteArray(100)
            val random = SecureRandom()
            random.nextBytes(salt)
            return salt
        }
    }

    // todo: is this secure?
    private fun getSecretKeyFromPW(password: CharArray, salt: ByteArray): SecretKey {
        val pbeKeySpec = PBEKeySpec(password, salt, KEY_ITERATIONS, KEY_SZE)
        val pbeKey = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(pbeKeySpec)
        return SecretKeySpec(pbeKey.encoded, "AES")
    }

    fun aesEncrypt(data: File, targetFile: OutputStream, password: CharArray) {
        val salt = getSalt()
        val secretKey = getSecretKeyFromPW(password, salt)


        val cipher = Cipher.getInstance("AES/CBC/PKCS5Padding")

        // Use a secure IV in production
        val ivParameterSpec = IvParameterSpec(ByteArray(16))
        cipher.init(Cipher.ENCRYPT_MODE, secretKey, ivParameterSpec)



        // make the first 100 bytes the salt
        targetFile.write(salt)

        val buffer = ByteArray(16384) // 16KB buffer
        var fileStream = data.inputStream()
        while (fileStream.available() != 0) {
            fileStream.read(buffer)
            targetFile.write(cipher.update(buffer))
        }

        targetFile.write(cipher.doFinal())
    }
}