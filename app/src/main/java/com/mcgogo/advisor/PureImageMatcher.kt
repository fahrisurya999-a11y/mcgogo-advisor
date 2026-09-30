package com.mcgogo.advisor

import android.graphics.Bitmap
import android.graphics.Color
import kotlin.math.abs
import kotlin.math.sqrt

data class ScannedCard(
    val slotIndex: Int,
    val heroId: String?,
    val heroName: String,
    val cost: Int?,
    val score: Double
)

class PureImageMatcher(
    private val db: MetaDB,
    private val heroBitmaps: Map<String, Bitmap>
) {
    // Relative bounding box for shop area in 16:9 / 20:9 horizontal orientation
    // Shop band is in the bottom 70% to 98% of the screen
    private val shopBandY0 = 0.70f
    private val shopBandY1 = 0.98f
    private val shopX0 = 0.09f
    private val shopX1 = 0.91f
    private val slotsCount = 5

    // Pre-calculate normalized color histograms / average color vectors for fast matching without native OpenCV
    private val templateSignatures: Map<String, FloatArray> = heroBitmaps.mapValues { (_, bmp) ->
        extractSignature(bmp)
    }

    companion object {
        private const val SAMPLE_SIZE = 32

        fun extractSignature(src: Bitmap): FloatArray {
            val scaled = if (src.width != SAMPLE_SIZE || src.height != SAMPLE_SIZE) {
                Bitmap.createScaledBitmap(src, SAMPLE_SIZE, SAMPLE_SIZE, true)
            } else {
                src
            }

            // Extract 32x32 color features (RGB normalized + brightness)
            val sig = FloatArray(SAMPLE_SIZE * SAMPLE_SIZE * 3)
            var idx = 0
            val pixels = IntArray(SAMPLE_SIZE * SAMPLE_SIZE)
            scaled.getPixels(pixels, 0, SAMPLE_SIZE, 0, 0, SAMPLE_SIZE, SAMPLE_SIZE)

            for (p in pixels) {
                val r = Color.red(p) / 255.0f
                val g = Color.green(p) / 255.0f
                val b = Color.blue(p) / 255.0f
                sig[idx++] = r
                sig[idx++] = g
                sig[idx++] = b
            }
            return sig
        }

        fun compareSignatures(sig1: FloatArray, sig2: FloatArray): Double {
            var dot = 0.0
            var norm1 = 0.0
            var norm2 = 0.0
            val len = minOf(sig1.size, sig2.size)
            for (i in 0 until len) {
                val v1 = sig1[i]
                val v2 = sig2[i]
                dot += (v1 * v2)
                norm1 += (v1 * v1)
                norm2 += (v2 * v2)
            }
            if (norm1 <= 0.0 || norm2 <= 0.0) return 0.0
            return dot / (sqrt(norm1) * sqrt(norm2))
        }
    }

    fun scanShop(screenBitmap: Bitmap): List<ScannedCard> {
        val width = screenBitmap.width
        val height = screenBitmap.height

        val bandY0 = (height * shopBandY0).toInt()
        val bandY1 = (height * shopBandY1).toInt()
        val bandH = bandY1 - bandY0
        val cy = bandY0 + bandH / 2

        val spanX = shopX1 - shopX0
        val slotW = (width / (slotsCount + 0.6f)).toInt()
        val artSide = minOf(slotW, bandH) * 8 / 10

        val results = mutableListOf<ScannedCard>()

        for (i in 0 until slotsCount) {
            val cx = (width * shopX0 + spanX * width * (i + 1) / (slotsCount + 1)).toInt()
            val left = maxOf(0, cx - artSide / 2)
            val top = maxOf(0, cy - artSide / 2)
            val w = minOf(width - left, artSide)
            val h = minOf(height - top, artSide)

            if (w < 16 || h < 16) {
                results.add(ScannedCard(i, null, "Unknown", null, 0.0))
                continue
            }

            val crop = Bitmap.createBitmap(screenBitmap, left, top, w, h)
            val cropSig = extractSignature(crop)

            // Match against hero templates
            var bestId: String? = null
            var bestScore = 0.0
            var runnerUp = 0.0

            for ((hid, sig) in templateSignatures) {
                val score = compareSignatures(cropSig, sig)
                if (score > bestScore) {
                    runnerUp = bestScore
                    bestScore = score
                    bestId = hid
                } else if (score > runnerUp) {
                    runnerUp = score
                }
            }

            // Margin filter for accuracy (winner must be distinctly higher than runner-up)
            val isReliable = bestScore >= 0.55 && (bestScore - runnerUp) >= 0.008
            val finalId = if (isReliable) bestId else null
            val hero = (finalId ?: bestId)?.let { db.heroes[it] }

            results.add(
                ScannedCard(
                    slotIndex = i,
                    heroId = finalId,
                    heroName = if (finalId != null) (hero?.name ?: "-") else "? " + (hero?.name ?: "-"),
                    cost = hero?.cost,
                    score = bestScore
                )
            )
        }

        return results
    }
}
