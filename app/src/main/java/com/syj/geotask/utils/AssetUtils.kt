package com.syj.geotask.utils

import android.content.Context
import java.io.File
import java.io.FileOutputStream

object AssetUtils {

    fun copyAssetToInternal(context: Context, assetName: String): String {
        val outFile = File(context.filesDir, assetName)

        if (!outFile.exists()) {
            context.assets.open(assetName).use { input ->
                FileOutputStream(outFile).use { output ->
                    input.copyTo(output)
                }
            }
        }

        return outFile.absolutePath
    }
}
