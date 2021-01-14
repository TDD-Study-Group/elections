package org.elections

import java.text.DecimalFormat
import java.util.*
import kotlin.collections.ArrayList
import kotlin.collections.HashMap

class Elections(private val votersByDistrict: Map<String, List<String>>, private val withDistrict: Boolean) {

    var candidates: MutableList<String> = ArrayList()
    var officialCandidates: MutableList<String> = ArrayList()
    var votesWithoutDistricts = ArrayList<Int>()
    private val votesWithDistricts: Map<String, ArrayList<Int>> = mapOf(
        "District 1" to ArrayList(),
        "District 2" to ArrayList(),
        "District 3" to ArrayList()
    )

    fun addCandidate(candidate: String) {
        officialCandidates.add(candidate)
        addUnofficialCandidate(candidate)
    }

    private fun addUnofficialCandidate(candidate: String) {
        candidates.add(candidate)
        votesWithoutDistricts.add(0)
        votesWithDistricts["District 1"]!!.add(0)
        votesWithDistricts["District 2"]!!.add(0)
        votesWithDistricts["District 3"]!!.add(0)
    }

    fun voteFor(elector: String?, candidate: String, electorDistrict: String) {
        if (withDistrict) {
            if (votesWithDistricts.containsKey(electorDistrict)) {
                val districtVotes = votesWithDistricts[electorDistrict]!!
                if (candidate !in candidates) {
                    candidates.add(candidate)
                    votesWithDistricts.forEach { (_, votes) -> votes.add(0) }
                }
                districtVotes.incrementVote(candidate)
            }
        } else {
            val votesWithoutDistrictsTemp = votesWithoutDistricts
            if (candidate !in candidates) {
                candidates.add(candidate)
                votesWithoutDistrictsTemp.add(0)
            }
            votesWithoutDistrictsTemp.incrementVote(candidate)
        }
    }

    private fun ArrayList<Int>.incrementVote(candidate: String) {
        val index = candidates.indexOf(candidate)
        this[index] = this[index] + 1
    }

    fun results(): Map<String, String> {
        val results: MutableMap<String, String> = HashMap()
        var nbVotes = 0
        var nullVotes = 0
        var blankVotes = 0
        var nbValidVotes = 0
        if (!withDistrict) {
            nbVotes = votesWithoutDistricts.sum()
            for (i in officialCandidates.indices) {
                val index = candidates.indexOf(officialCandidates[i])
                nbValidVotes += votesWithoutDistricts[index]
            }
            for (i in votesWithoutDistricts.indices) {
                val candidatResult = votesWithoutDistricts[i].toFloat() * 100 / nbValidVotes
                val candidate = candidates[i]
                if (officialCandidates.contains(candidate)) {
                    results[candidate] = String.format(Locale.FRENCH, "%.2f%%", candidatResult)
                } else {
                    if (candidates[i].isEmpty()) {
                        blankVotes += votesWithoutDistricts[i]
                    } else {
                        nullVotes += votesWithoutDistricts[i]
                    }
                }
            }
        } else {
            for (districtVotes in votesWithDistricts.values) {
                nbVotes += districtVotes.sum()
            }
            for (i in officialCandidates.indices) {
                val index = candidates.indexOf(officialCandidates[i])
                for (districtVotes in votesWithDistricts.values) {
                    nbValidVotes += districtVotes[index]
                }
            }
            val officialCandidatesResult: MutableMap<String, Int> = HashMap()
            for (i in officialCandidates.indices) {
                officialCandidatesResult[candidates[i]] = 0
            }
            for (districtVotes in votesWithDistricts.values) {
                val districtResult = ArrayList<Float>()
                for (i in districtVotes.indices) {
                    var candidateResult = 0f
                    if (nbValidVotes != 0) candidateResult = districtVotes[i].toFloat() * 100 / nbValidVotes
                    val candidate = candidates[i]
                    if (officialCandidates.contains(candidate)) {
                        districtResult.add(candidateResult)
                    } else {
                        if (candidates[i].isEmpty()) {
                            blankVotes += districtVotes[i]
                        } else {
                            nullVotes += districtVotes[i]
                        }
                    }
                }
                var districtWinnerIndex = 0
                for (i in 1 until districtResult.size) {
                    if (districtResult[districtWinnerIndex] < districtResult[i]) districtWinnerIndex = i
                }
                officialCandidatesResult[candidates[districtWinnerIndex]] =
                    officialCandidatesResult[candidates[districtWinnerIndex]]!! + 1
            }
            for (i in 0 until officialCandidatesResult.size) {
                val ratioCandidate =
                    officialCandidatesResult[candidates[i]]!!.toFloat() / officialCandidatesResult.size * 100
                results[candidates[i]] = String.format(Locale.FRENCH, "%.2f%%", ratioCandidate)
            }
        }
        val blankResult = blankVotes.toFloat() * 100 / nbVotes
        results["Blank"] = String.format(Locale.FRENCH, "%.2f%%", blankResult)
        val nullResult = nullVotes.toFloat() * 100 / nbVotes
        results["Null"] = String.format(Locale.FRENCH, "%.2f%%", nullResult)
        val nbElectors = votersByDistrict.values.map { it.size }.sum()
        val df = DecimalFormat()
        df.maximumFractionDigits = 2
        val abstentionResult = 100 - nbVotes.toFloat() * 100 / nbElectors
        results["Abstention"] = String.format(Locale.FRENCH, "%.2f%%", abstentionResult)
        return results
    }
}
