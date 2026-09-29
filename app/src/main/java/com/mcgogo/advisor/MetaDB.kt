package com.mcgogo.advisor

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.io.InputStreamReader

data class Hero(
    val id: String,
    val name: String,
    val cost: Int,
    val traits: List<String>
)

data class Trait(
    val name: String,
    val desc: String,
    val breaks: List<Int>,
    val members: List<String>
)

class MetaDB(
    val heroes: Map<String, Hero>,
    val traits: Map<String, Trait>
) {
    companion object {
        fun load(heroesJson: String, traitsJson: String): MetaDB {
            val gson = Gson()
            val heroListType = object : TypeToken<List<Hero>>() {}.type
            val heroList: List<Hero> = gson.fromJson(heroesJson, heroListType)
            val heroes = heroList.associateBy { it.id }

            val traitMapType = object : TypeToken<Map<String, Trait>>() {}.type
            val traits: Map<String, Trait> = gson.fromJson(traitsJson, traitMapType)

            return MetaDB(heroes, traits)
        }
    }

    fun traitCounts(boardHeroIds: Collection<String>): Map<String, Int> {
        val counts = mutableMapOf<String, Int>()
        for (hid in boardHeroIds) {
            val h = heroes[hid] ?: continue
            for (tid in h.traits) {
                counts[tid] = (counts[tid] ?: 0) + 1
            }
        }
        return counts
    }
}
