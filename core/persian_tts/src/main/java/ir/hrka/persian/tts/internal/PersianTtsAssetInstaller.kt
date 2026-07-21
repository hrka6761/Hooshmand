package ir.hrka.persian.tts.internal

import android.content.Context
import android.util.Log
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

/**
 * Copies bundled Persian TTS assets from the APK asset tree to app-private
 * storage exactly once (per install marker version).
 *
 * Piper / eSpeak-ng require real filesystem paths for the model, tokens, and
 * `dataDir`, so assets under `assets/persian_tts/` are mirrored to
 * `context.filesDir/persian_tts/`.
 *
 * Subsequent calls are cheap when the [READY_MARKER] file and required paths
 * already exist and are non-empty.
 */
internal object PersianTtsAssetInstaller {
    /** Logcat tag for install diagnostics. */
    private const val TAG = "PersianTtsAssets"

    /** Root folder name under both `assets/` and `filesDir`. */
    private const val ASSET_ROOT = "persian_tts"

    /**
     * Presence of this marker (plus valid model/tokens/espeak dirs) means a
     * previous install completed. Bump the suffix when asset layout changes.
     */
    private const val READY_MARKER = ".ready_v1"

    /**
     * Absolute filesystem paths required by sherpa-onnx OfflineTts VITS config.
     *
     * @property modelPath Absolute path to `fa_IR-gyro-medium.onnx`.
     * @property tokensPath Absolute path to Piper `tokens.txt`.
     * @property espeakDataDir Absolute path to the `espeak-ng-data` directory.
     */
    data class InstalledPaths(
        val modelPath: String,
        val tokensPath: String,
        val espeakDataDir: String,
    )

    /**
     * Ensures assets are present on disk and returns absolute paths for the
     * engine.
     *
     * If the ready marker or any required file is missing/empty, deletes the
     * previous tree (if any), copies the full asset tree, then writes the marker.
     *
     * @param context Used for [Context.getAssets] and [Context.getFilesDir].
     * @return Paths to model, tokens, and eSpeak data dir.
     * @throws IOException when assets are missing or cannot be copied.
     */
    @Throws(IOException::class)
    fun ensureInstalled(context: Context): InstalledPaths {
        val root = File(context.filesDir, ASSET_ROOT)
        val marker = File(root, READY_MARKER)
        val modelFile = File(root, "model/fa_IR-gyro-medium.onnx")
        val tokensFile = File(root, "model/tokens.txt")
        val espeakDir = File(root, "espeak-ng-data")

        if (
            marker.isFile &&
            modelFile.isFile &&
            modelFile.length() > 0L &&
            tokensFile.isFile &&
            tokensFile.length() > 0L &&
            espeakDir.isDirectory
        ) {
            return InstalledPaths(
                modelPath = modelFile.absolutePath,
                tokensPath = tokensFile.absolutePath,
                espeakDataDir = espeakDir.absolutePath,
            )
        }

        if (root.exists()) {
            root.deleteRecursively()
        }
        if (!root.mkdirs() && !root.isDirectory) {
            throw IOException("Cannot create Persian TTS asset directory: ${root.absolutePath}")
        }

        copyAssetTree(context, ASSET_ROOT, root)

        if (!modelFile.isFile || modelFile.length() == 0L) {
            throw IOException("Persian TTS model missing after install: ${modelFile.absolutePath}")
        }
        if (!tokensFile.isFile || tokensFile.length() == 0L) {
            throw IOException("Persian TTS tokens missing after install: ${tokensFile.absolutePath}")
        }
        if (!espeakDir.isDirectory) {
            throw IOException("eSpeak data missing after install: ${espeakDir.absolutePath}")
        }

        if (!marker.createNewFile() && !marker.isFile) {
            Log.w(TAG, "Could not write ready marker at ${marker.absolutePath}")
        }

        return InstalledPaths(
            modelPath = modelFile.absolutePath,
            tokensPath = tokensFile.absolutePath,
            espeakDataDir = espeakDir.absolutePath,
        )
    }

    /**
     * Recursively copies [assetPath] from the APK assets into [destinationDir].
     *
     * When [Context.assets.list] returns an empty/null list, [assetPath] is
     * treated as a leaf file and copied to [destinationDir] as a file path.
     *
     * @param context Asset source.
     * @param assetPath Relative path under `assets/` (e.g. `persian_tts/model`).
     * @param destinationDir Target directory (or file path for a leaf asset).
     * @throws IOException on list or copy failure.
     */
    private fun copyAssetTree(
        context: Context,
        assetPath: String,
        destinationDir: File,
    ) {
        val children =
            try {
                context.assets.list(assetPath)
            } catch (e: IOException) {
                throw IOException("Failed to list assets at $assetPath", e)
            }

        if (children.isNullOrEmpty()) {
            // Leaf file.
            val parent = destinationDir.parentFile
            if (parent != null && !parent.exists() && !parent.mkdirs() && !parent.isDirectory) {
                throw IOException("Cannot create parent for $destinationDir")
            }
            copyAssetFile(context, assetPath, destinationDir)
            return
        }

        if (!destinationDir.exists() && !destinationDir.mkdirs() && !destinationDir.isDirectory) {
            throw IOException("Cannot create directory $destinationDir")
        }

        for (child in children) {
            val childAssetPath = if (assetPath.isEmpty()) child else "$assetPath/$child"
            copyAssetTree(context, childAssetPath, File(destinationDir, child))
        }
    }

    /**
     * Copies a single asset file to [destinationFile], overwriting if present.
     *
     * @param context Asset source.
     * @param assetPath Relative asset path of the file.
     * @param destinationFile Absolute destination file.
     * @throws IOException when open/read/write fails.
     */
    private fun copyAssetFile(
        context: Context,
        assetPath: String,
        destinationFile: File,
    ) {
        context.assets.open(assetPath).use { input ->
            FileOutputStream(destinationFile).use { output ->
                val buffer = ByteArray(64 * 1024)
                while (true) {
                    val read = input.read(buffer)
                    if (read <= 0) break
                    output.write(buffer, 0, read)
                }
                output.flush()
            }
        }
    }
}
