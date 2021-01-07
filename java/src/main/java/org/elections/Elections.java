package org.elections;

import java.text.DecimalFormat;
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
            for (Map.Entry<String, ArrayList<Integer>> entry : votesWithDistricts.entrySet()) {
                ArrayList<Integer> districtVotes = entry.getValue();
                nbVotes += districtVotes.stream().reduce(0, Integer::sum);
            }

            int nbValidVotes = 0;
            for (int i = 0; i < officialCandidates.size(); i++) {
                int index = candidates.indexOf(officialCandidates.get(i));
                for (Map.Entry<String, ArrayList<Integer>> entry : votesWithDistricts.entrySet()) {
                    ArrayList<Integer> districtVotes = entry.getValue();
                    nbValidVotes += districtVotes.get(index);
                }
            }

            Map<String, Integer> officialCandidatesResult = new HashMap<>();
            for (int i = 0; i < officialCandidates.size(); i++) {
                officialCandidatesResult.put(candidates.get(i), 0);
            }
            for (Map.Entry<String, ArrayList<Integer>> entry : votesWithDistricts.entrySet()) {
                ArrayList<Float> districtResult = new ArrayList<>();
                ArrayList<Integer> districtVotes = entry.getValue();
                for (int i = 0; i < districtVotes.size(); i++) {
                    float candidateResult = 0;
                    if (nbValidVotes != 0)
                        candidateResult = ((float) districtVotes.get(i) * 100) / nbValidVotes;
                    String candidate = candidates.get(i);
                    if (officialCandidates.contains(candidate)) {
                        districtResult.add(candidateResult);
                    } else {
                        if (candidates.get(i).isEmpty()) {
                            blankVotes += districtVotes.get(i);
                        } else {
                            nullVotes += districtVotes.get(i);
                        }
                    }
                }
                int districtWinnerIndex = 0;
                for (int i = 1; i < districtResult.size(); i++) {
                    if (districtResult.get(districtWinnerIndex) < districtResult.get(i))
                        districtWinnerIndex = i;
                }
                officialCandidatesResult.put(candidates.get(districtWinnerIndex), officialCandidatesResult.get(candidates.get(districtWinnerIndex)) + 1);
            }
            for (int i = 0; i < officialCandidatesResult.size(); i++) {
                Float ratioCandidate = ((float) officialCandidatesResult.get(candidates.get(i))) / officialCandidatesResult.size() * 100;
                results.put(candidates.get(i), String.format(Locale.FRENCH, "%.2f%%", ratioCandidate));
            }
        } else {

            nbVotes = votesWithoutDistricts.stream().reduce(0, Integer::sum);
            int nbValidVotes = countValidVotes();

            for (int i = 0; i < votesWithoutDistricts.size(); i++) {
                Float candidateResult = ((float) votesWithoutDistricts.get(i) * 100) / nbValidVotes;
                String candidate = candidates.get(i);
                if (officialCandidates.contains(candidate)) {
                    results.put(candidate, String.format(Locale.FRENCH, "%.2f%%", candidateResult));
                } else {
                    if (candidate.isEmpty()) {
                        blankVotes += votesWithoutDistricts.get(i);
                    } else {
                        nullVotes += votesWithoutDistricts.get(i);
                    }
                }
            }
        }

        float blankResult = ((float) blankVotes * 100) / nbVotes;
        results.put("Blank", String.format(Locale.FRENCH, "%.2f%%", blankResult));

        float nullResult = ((float) nullVotes * 100) / nbVotes;
        results.put("Null", String.format(Locale.FRENCH, "%.2f%%", nullResult));

        int nbElectors = electorsPerDistrict.values().stream().map(List::size).reduce(0, Integer::sum);
        DecimalFormat df = new DecimalFormat();
        df.setMaximumFractionDigits(2);
        float abstentionResult = 100 - ((float) nbVotes * 100 / nbElectors);
        results.put("Abstention", String.format(Locale.FRENCH, "%.2f%%", abstentionResult));

        return results;
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