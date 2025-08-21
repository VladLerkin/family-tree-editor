package com.pedigree.search;

import com.pedigree.model.Individual;
import com.pedigree.storage.ProjectRepository;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

public class QuickSearchService {
    private final ProjectRepository.ProjectData data;
    private final Map<String, Set<String>> prefixToIds = new HashMap<>();

    public QuickSearchService(ProjectRepository.ProjectData data) {
        this.data = Objects.requireNonNull(data, "data");
        rebuildIndex();
    }

    public void rebuildIndex() {
        prefixToIds.clear();
        for (Individual individual : data.individuals) {
            indexIndividual(individual);
        }
    }

    public void addIndividual(Individual individual) {
        data.individuals.add(individual);
        indexIndividual(individual);
    }

    public void updateIndividual(Individual individual) {
        removeIndividual(individual.getId());
        data.individuals.add(individual);
        indexIndividual(individual);
    }

    public void removeIndividual(String individualId) {
        data.individuals.removeIf(i -> i.getId().equals(individualId));
        for (Set<String> ids : prefixToIds.values()) {
            ids.remove(individualId);
        }
    }

    public List<Individual> findByName(String query) {
        if (query == null || query.isBlank()) {
            return Collections.emptyList();
        }
        String q = normalize(query);

        Set<String> resultIds = prefixToIds.getOrDefault(q, Collections.emptySet());

        List<Individual> prefixMatches = data.individuals.stream()
                .filter(i -> resultIds.contains(i.getId()))
                .collect(Collectors.toList());

        List<Individual> substringMatches = data.individuals.stream()
                .filter(i -> !resultIds.contains(i.getId()))
                .filter(i -> containsNormalized(i.getFirstName(), q) || containsNormalized(i.getLastName(), q))
                .collect(Collectors.toList());

        List<Individual> combined = new ArrayList<>(prefixMatches.size() + substringMatches.size());
        combined.addAll(prefixMatches);
        combined.addAll(substringMatches);

        combined.sort(Comparator
                .comparing(Individual::getLastName, Comparator.nullsLast(String::compareToIgnoreCase))
                .thenComparing(Individual::getFirstName, Comparator.nullsLast(String::compareToIgnoreCase)));
        return combined;
    }

    private void indexIndividual(Individual individual) {
        for (String token : tokensFor(individual)) {
            for (int i = 1; i <= token.length(); i++) {
                String prefix = token.substring(0, i);
                prefixToIds.computeIfAbsent(prefix, k -> new HashSet<>()).add(individual.getId());
            }
        }
    }

    private static List<String> tokensFor(Individual i) {
        List<String> tokens = new ArrayList<>();
        if (i.getFirstName() != null) {
            tokens.add(normalize(i.getFirstName()));
        }
        if (i.getLastName() != null) {
            tokens.add(normalize(i.getLastName()));
        }
        return tokens;
    }

    private static boolean containsNormalized(String value, String needle) {
        if (value == null) { return false; }
        return normalize(value).contains(needle);
    }

    private static String normalize(String s) {
        return s.toLowerCase(Locale.ROOT).trim();
    }
}



