package extensions

import java.io.BufferedInputStream
import java.io.File
import java.io.FileInputStream

fun File.countLines(): Int {
    val inputStream = BufferedInputStream(FileInputStream(this))
    return inputStream.use {
        val c = ByteArray(1024)
        var readChars: Int = it.read(c)
        if (readChars == -1) {
            // bail out if nothing to read
            return 0
        }

        // make it easy for the optimizer to tune this loop
        var count = 0
        while (readChars == 1024) {
            var i = 0
            while (i < 1024) {
                if (c[i++] == '\n'.code.toByte()) {
                    ++count
                }
            }
            readChars = it.read(c)
        }

        // count remaining characters
        while (readChars != -1) {
            for (i in 0 until readChars) {
                if (c[i] == '\n'.code.toByte()) {
                    ++count
                }
            }
            readChars = it.read(c)
        }
        if (count == 0) 1 else count
    }
}