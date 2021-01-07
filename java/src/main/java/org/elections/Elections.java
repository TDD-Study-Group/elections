package org.elections;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class Elections {
    List<String> candidates = new ArrayList<>();
    List<String> officialCandidates = new ArrayList<>();
    ArrayList<Integer> votesWithoutDistricts = new ArrayList<>();
    Map<String, ArrayList<Integer>> votesWithDistricts;
    private Map<String, List<String>> electorsPerDistrict;
    private boolean withDistrict;

    public Elections(Map<String, List<String>> electorsPerDistrict, boolean withDistrict) {
        this.electorsPerDistrict = electorsPerDistrict;
        this.withDistrict = withDistrict;

        votesWithDistricts = new HashMap<>();
        votesWithDistricts.put("District 1", new ArrayList<>());
        votesWithDistricts.put("District 2", new ArrayList<>());
        votesWithDistricts.put("District 3", new ArrayList<>());
    }

    public void addCandidate(String candidate) {
        addUnofficialCandidate(candidate);
        makeOfficial(candidate);
    }

    private void makeOfficial(String candidate) {
        officialCandidates.add(candidate);
    }

    public void voteFor(String elector, String candidate, String electorDistrict) {
        if (withDistrict) {
            if (votesWithDistricts.containsKey(electorDistrict)) {
                addUnofficialCandidate(candidate);
                incrementVoteFor(candidate, votesWithDistricts.get(electorDistrict));
            }
        } else {
            addUnofficialCandidate(candidate);
            incrementVoteFor(candidate, votesWithoutDistricts);
        }
    }

    private void addUnofficialCandidate(String candidate) {
        if (!candidates.contains(candidate)) {
            candidates.add(candidate);
            votesWithoutDistricts.add(0);
            votesWithDistricts.forEach((district, votes) -> votes.add(0));
        }
    }

    private void incrementVoteFor(String candidate, ArrayList<Integer> votes) {
        int index = candidates.indexOf(candidate);
        votes.set(index, votes.get(index) + 1);
    }

    public Map<String, String> results() {
        Map<String, String> results = new HashMap<>();
        Integer nbVotes = 0;
        Integer nullVotes = 0;
        Integer blankVotes = 0;

        if (withDistrict) {
            nbVotes = countVotesWithDistrict(nbVotes);
            int nbValidVotes = countValidVotesWithDistrict();

            Map<String, Integer> officialCandidatesResult = new HashMap<>();
            for (int i = 0; i < officialCandidates.size(); i++) {
                officialCandidatesResult.put(candidates.get(i), 0);
            }
            for (Map.Entry<String, ArrayList<Integer>> entry : votesWithDistricts.entrySet()) {
                ArrayList<Float> districtResult = new ArrayList<>();
                ArrayList<Integer> districtVotes = entry.getValue();
                for (int i = 0; i < districtVotes.size(); i++) {
                    String candidate = candidates.get(i);
                    Integer candidateVotesInDistrict = districtVotes.get(i);
                    if (officialCandidates.contains(candidate)) {
                        float candidateResult = 0;
                        if (nbValidVotes != 0)
                            candidateResult = ((float) candidateVotesInDistrict * 100) / nbValidVotes;
                        districtResult.add(candidateResult);
                    } else if (candidate.isEmpty()) {
                        blankVotes += candidateVotesInDistrict;
                    } else {
                        nullVotes += candidateVotesInDistrict;
                    }
                }
                String districtWinner = districtWinner(districtResult);
                officialCandidatesResult.put(districtWinner,
                        officialCandidatesResult.get(districtWinner) + 1);
            }
            for (int i = 0; i < officialCandidatesResult.size(); i++) {
                String candidate = candidates.get(i);
                Float ratioCandidate = ((float) officialCandidatesResult.get(candidate)) / officialCandidatesResult.size() * 100;
                results.put(candidate, format(ratioCandidate));
            }
        } else {

            nbVotes = votesWithoutDistricts.stream().reduce(0, Integer::sum);
            int nbValidVotes = countValidVotes();

            for (int i = 0; i < votesWithoutDistricts.size(); i++) {
                Integer candidateVotes = votesWithoutDistricts.get(i);
                String candidate = candidates.get(i);
                if (officialCandidates.contains(candidate)) {
                    float candidateResult = 0f;
                    if (nbValidVotes != 0)
                        candidateResult = ((float) candidateVotes * 100) / nbValidVotes;
                    results.put(candidate, format(candidateResult));
                } else if (candidate.isEmpty()) {
                    blankVotes += candidateVotes;
                } else {
                    nullVotes += candidateVotes;
                }
            }
        }

        float blankResult = ((float) blankVotes * 100) / nbVotes;
        results.put("Blank", format(blankResult));

        float nullResult = ((float) nullVotes * 100) / nbVotes;
        results.put("Null", format(nullResult));

        int nbElectors = electorsPerDistrict.values().stream().map(List::size).reduce(0, Integer::sum);
        float abstentionResult = 100 - ((float) nbVotes * 100 / nbElectors);
        results.put("Abstention", format(abstentionResult));

        return results;
    }

    private String districtWinner(ArrayList<Float> districtResult) {
        int districtWinnerIndex = 0;
        for (int i = 1; i < districtResult.size(); i++) {
            if (districtResult.get(districtWinnerIndex) < districtResult.get(i))
                districtWinnerIndex = i;
        }
        return candidates.get(districtWinnerIndex);
    }

    private int countValidVotesWithDistrict() {
        int nbValidVotes = 0;
        for (int i = 0; i < officialCandidates.size(); i++) {
            int index = candidates.indexOf(officialCandidates.get(i));
            for (Map.Entry<String, ArrayList<Integer>> entry : votesWithDistricts.entrySet()) {
                ArrayList<Integer> districtVotes = entry.getValue();
                nbValidVotes += districtVotes.get(index);
            }
        }
        return nbValidVotes;
    }

    private Integer countVotesWithDistrict(Integer nbVotes) {
        for (Map.Entry<String, ArrayList<Integer>> entry : votesWithDistricts.entrySet()) {
            ArrayList<Integer> districtVotes = entry.getValue();
            nbVotes += districtVotes.stream().reduce(0, Integer::sum);
        }
        return nbVotes;
    }

    private String format(Float result) {
        return String.format(Locale.FRENCH, "%.2f%%", result);
    }

    private int countValidVotes() {
        int nbValidVotes = 0;
        for (String officialCandidate : officialCandidates) {
            int index = candidates.indexOf(officialCandidate);
            nbValidVotes += votesWithoutDistricts.get(index);
        }
        return nbValidVotes;
    }
}
