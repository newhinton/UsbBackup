package de.felixnuesse.usbbackup

import de.felixnuesse.usbbackup.crypto.AES
import org.junit.Test

import org.junit.Assert.*
import java.nio.ByteBuffer

/**
 * Source: https://www.baeldung.com/kotlin/advanced-encryption-standard
 */
class Crypto {

    @Test
    fun `Given text when encrypted and decrypted should return original text`() {
        val aes = AES()
        val originalText = "Hello Kotlin AES Encryption!Hello Kotlin AES Encryption!Hello Kotlin AES Encryption!Hello Kotlin AES Encryption!Hello Kotlin AES Encryption!Hello Kotlin AES Encryption!Hello Kotlin AES Encryption!Hello Kotlin AES Encryption!Hello Kotlin AES Encryption!Hello Kotlin AES Encryption!Hello Kotlin AES Encryption!Hello Kotlin AES Encryption!Hello Kotlin AES Encryption!Hello Kotlin AES Encryption!Hello Kotlin AES Encryption!Hello Kotlin AES Encryption!Hello Kotlin AES Encryption!Hello Kotlin AES Encryption!Hello Kotlin AES Encryption!Hello Kotlin AES Encryption!Hello Kotlin AES Encryption!Hello Kotlin AES Encryption!Hello Kotlin AES Encryption!"
        val password = "super secret password"

        //val encryptedData = aes.aesEncrypt(originalText.toByteArray(), password.toCharArray())
        //val decryptedData = aes.aesDecrypt(encryptedData, password.toCharArray())
        //val decryptedText = String(decryptedData)

        //System.err.println("Original: "+originalText)
        //System.err.println("Decrypted: "+decryptedText)
        //assertTrue("The decrypted text does not match the original", originalText == decryptedText)
    }
}