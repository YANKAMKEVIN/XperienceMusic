package com.xperiencemusic.shared.platform

import android.content.Context
import android.net.Uri
import com.xperiencemusic.shared.domain.repository.TempFileWriter
import java.io.File

class AndroidTempFileWriter(private val context: Context) : TempFileWriter {
    override fun write(fileName: String, bytes: ByteArray): String {
        val directory = File(context.cacheDir, "audio").apply { mkdirs() }
        val file = File(directory, fileName).apply { writeBytes(bytes) }
        return Uri.fromFile(file).toString()
    }
}
