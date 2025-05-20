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
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.Calendar


fun main(args: Array<String>) = AesTool().main(args)

class AesTool : CliktCommand() {

    override val printHelpOnEmptyArgs = true
    override fun help(context: Context) = """
        |Apply AES on <source> and write to <target>. 
        |If no parameter is supplied, the default operation is to encrypt the file.
        |
        |The expected file should have the first 128 Bytes salted, and the next 16 Bytes the IV.
        |""".trimMargin()
    override fun helpEpilog(context: Context) = "Copyright Felix Nüsse@${Calendar.getInstance().get(Calendar.YEAR)}"

    init {
        versionOption("1.0.0")
    }

    private val encrypt: Boolean by option("-e", "--encrypt").flag(default = false).help("Encrypt file instead of decrypting")
    val password by option().prompt("Password", requireConfirmation = false, hideInput = true).help("The password used for the cryptographic operation")

    private val source by argument().file()
    private val target by argument().file()

    override fun run() {

        var targetFile = if(target.isDirectory) {
            File(target, "decrypted.zip")
        } else {
            target
        }

        if(encrypt) {
            Crypto().aesEncrypt(FileInputStream(source), FileOutputStream(targetFile), password.toCharArray())
        } else {
            Crypto().aesDecrypt(FileInputStream(source), FileOutputStream(targetFile), password.toCharArray())
        }
    }

}
