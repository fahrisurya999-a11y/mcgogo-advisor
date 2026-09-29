package com.mcgogo.advisor

data class Advice(
    val hero: Hero,
    val verdict: String, // BUY, SAVE, PASS
    val score: Double,
    val reasons: List<String>
)

data class Recommendation(
    val stage: Int,
    val targetComp: String,
    val advices: List<Advice>,
    val notes: List<String>
)

class Advisor(private val db: MetaDB) {

    fun recommend(
        boardHeroIds: List<String>,
        shopHeroIds: List<String>,
        roundNo: Int = 4,
        gold: Int = 30,
        hp: Int = 60
    ): Recommendation {
        val counts = db.traitCounts(boardHeroIds)
        val stage = when {
            roundNo <= 3 -> 1
            roundNo <= 5 -> 2
            else -> 3
        }

        val advices = mutableListOf<Advice>()
        for (hid in shopHeroIds) {
            val hero = db.heroes[hid] ?: continue
            var traitScore = 0.3
            val reasons = mutableListOf<String>()

            // Check trait proximity
            for (tid in hero.traits) {
                val trait = db.traits[tid] ?: continue
                val current = counts[tid] ?: 0
                val nextBreak = trait.breaks.firstOrNull { it > current }
                if (nextBreak != null) {
                    val gap = nextBreak - current
                    if (gap == 1) {
                        traitScore = maxOf(traitScore, 1.2)
                        reasons.add("${trait.name} butuh 1 hero lagi ($current -> $nextBreak)")
                    } else if (gap == 2) {
                        traitScore = maxOf(traitScore, 0.7)
                    }
                }
            }

            val costScore = when (stage) {
                1 -> if (hero.cost <= 2) 1.0 else 0.4
                2 -> if (hero.cost in 2..4) 0.9 else 0.6
                else -> if (hero.cost >= 4) 1.1 else 0.4
            }

            val totalScore = 2.2 * traitScore + 1.0 * costScore
            val verdict = when {
                gold < hero.cost -> "SAVE"
                totalScore >= 2.6 -> "BUY"
                totalScore >= 1.8 -> "SAVE"
                else -> "PASS"
            }

            advices.add(Advice(hero, verdict, totalScore, reasons))
        }

        advices.sortByDescending { it.score }

        // Find primary target comp
        var targetComp = "Bebas / Adaptif"
        var minGap = 99
        for ((tid, count) in counts) {
            val trait = db.traits[tid] ?: continue
            val nextBreak = trait.breaks.firstOrNull { it > count } ?: continue
            val gap = nextBreak - count
            if (gap < minGap) {
                minGap = gap
                targetComp = "${trait.name} (kurang $gap)"
            }
        }

        return Recommendation(stage, targetComp, advices, listOf("Bunga gold: simpan >= 20G"))
    }
}
