package de.felixnuesse.desktop

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.Context
import com.github.ajalt.clikt.core.main
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.help
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.prompt
import com.github.ajalt.clikt.parameters.options.versionOption
import com.github.ajalt.clikt.parameters.types.file
import de.felixnuesse.crypto.Crypto
import java.io.FileInputStream
import java.io.FileOutputStream


fun main(args: Array<String>) = AesCrypt().main(args)


const val helpString="Apply AES on <source> and write to <target>. If no parameter is supplied, the default operation is to encrypt the file."

class AesCrypt : CliktCommand() {

    override val printHelpOnEmptyArgs = false
    override fun help(context: Context) = helpString


    init {
        versionOption("1.0.0")
    }

    companion object {
        const val KEY_ALL = "all"
    }

    private val encrypt: Boolean by option("-e", "--encrypt").flag(default = false).help("Encrypt file instead of decrypting")
    val password: String by option().prompt("Password").help("The password used for the cryptographic operation")


    private val source by argument().file()
    private val target by argument().file()


    override fun run() {

        if(encrypt) {
            Crypto().aesEncrypt(FileInputStream(source), FileOutputStream(target), password.toCharArray())
        } else {
            Crypto().aesDecrypt(FileInputStream(source), FileOutputStream(target), password.toCharArray())
        }
    }

}
