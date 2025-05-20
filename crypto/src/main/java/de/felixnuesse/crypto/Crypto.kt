package de.felixnuesse.crypto

import java.io.InputStream
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
class Crypto {

    companion object {
        private var KEY_ITERATIONS = 50000
        private var KEY_SZE = 256

        private var BUFFER_SIZE = 16384 //16k

        private fun getSalt(): ByteArray {
            val salt = ByteArray(128)
            val random = SecureRandom()
            random.nextBytes(salt)
            return salt
        }


        private fun getIV(): ByteArray {
            val salt = ByteArray(16)
            val random = SecureRandom()
            random.nextBytes(salt)
            return salt
        }

        private fun getCipher(): Cipher {
            return Cipher.getInstance("AES/CBC/PKCS5Padding")
        }
    }

    // todo: is this secure?
    private fun getSecretKeyFromPW(password: CharArray, salt: ByteArray): SecretKey {
        val pbeKeySpec = PBEKeySpec(password, salt, KEY_ITERATIONS, KEY_SZE)
        val pbeKey = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(pbeKeySpec)
        return SecretKeySpec(pbeKey.encoded, "AES")
    }

    fun aesEncrypt(data: InputStream, targetFile: OutputStream, password: CharArray) {

        val cipher = getCipher()

        val salt = getSalt()
        val iv = getIV()

        val secretKey = getSecretKeyFromPW(password, salt)
        cipher.init(Cipher.ENCRYPT_MODE, secretKey, IvParameterSpec(iv))


        // make the first 100 bytes the salt, and the next 16 the iv
        targetFile.write(salt)
        targetFile.write(iv)

        processData(cipher, data, targetFile)
    }


    fun aesDecrypt(data: InputStream, targetFile: OutputStream, password: CharArray) {

        val cipher = getCipher()

        val salt = ByteArray(128)
        val iv = ByteArray(16)
        data.read(salt) // read consumes
        data.read(iv)

        val secretKey = getSecretKeyFromPW(password, salt)
        cipher.init(Cipher.DECRYPT_MODE, secretKey, IvParameterSpec(iv))

        processData(cipher, data, targetFile)
    }


    private fun processData(cipher: Cipher, data: InputStream, targetFile: OutputStream) {

        var buffer = ByteArray(BUFFER_SIZE)
        while (data.available() != 0) {
            if(data.available() < BUFFER_SIZE) {
                buffer = ByteArray(data.available())
            }
            data.read(buffer)
            targetFile.write(cipher.update(buffer))
        }

        targetFile.write(cipher.doFinal())

        targetFile.close()
        data.close()
    }
}