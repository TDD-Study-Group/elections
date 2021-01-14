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
        votesWithDistricts.forEach { (_, votes) -> votes.add(0) }
    }

    fun voteFor(elector: String?, candidate: String, electorDistrict: String) {
        when {
            withDistrict -> if (districtExists(electorDistrict)) {
                votesWithDistricts[electorDistrict]!!.incrementVotesFor(candidate)
            }
            else -> votesWithoutDistricts.incrementVotesFor(candidate)
        }
    }

    private fun districtExists(electorDistrict: String) = electorDistrict in votesWithDistricts.keys

    private fun ArrayList<Int>.incrementVotesFor(candidate: String) {
        if (candidate !in candidates) {
            addUnofficialCandidate(candidate)
        }
        val index = candidates.indexOf(candidate)
        this[index] = this[index] + 1
    }

    fun results(): Map<String, String> {
        val results: MutableMap<String, String> = HashMap()
        var nullVotes = 0
        var blankVotes = 0


        val nbValidVotes = officialCandidates.map { candidate -> candidates.indexOf(candidate) }
            .sumBy { candidateIndex ->
                when {
                    withDistrict -> votesWithDistricts.values.sumBy { districtVotes -> districtVotes[candidateIndex] }
                    else -> votesWithoutDistricts[candidateIndex]
                }
            }

        if (withDistrict) {

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
                results[candidates[i]] = format(ratioCandidate)
            }
        } else {

            for (i in votesWithoutDistricts.indices) {
                val candidatResult = votesWithoutDistricts[i].toFloat() * 100 / nbValidVotes
                val candidate = candidates[i]
                if (officialCandidates.contains(candidate)) {
                    results[candidate] = format(candidatResult)
                } else {
                    if (candidates[i].isEmpty()) {
                        blankVotes += votesWithoutDistricts[i]
                    } else {
                        nullVotes += votesWithoutDistricts[i]
                    }
                }
            }
        }

        val nbVotes = when {
            withDistrict -> votesWithDistricts.values.sumBy(ArrayList<Int>::sum)
            else -> votesWithoutDistricts.sum()
        }
        val blankResult = blankVotes.toFloat() * 100 / nbVotes
        results["Blank"] = format(blankResult)
        val nullResult = nullVotes.toFloat() * 100 / nbVotes
        results["Null"] = format(nullResult)
        val nbElectors = votersByDistrict.values.map { it.size }.sum()
        val df = DecimalFormat()
        df.maximumFractionDigits = 2
        val abstentionResult = 100 - nbVotes.toFloat() * 100 / nbElectors
        results["Abstention"] = format(abstentionResult)
        return results
    }

    private fun format(ratioCandidate: Float) = String.format(Locale.FRENCH, "%.2f%%", ratioCandidate)
}
