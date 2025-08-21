package com.pedigree.layout;

import com.pedigree.model.Family;
import com.pedigree.model.Individual;
import com.pedigree.render.NodeMetrics;
import com.pedigree.storage.ProjectRepository;

import java.util.*;

/**
 * Computes a basic layered layout for a pedigree graph.
 * - Individuals with no parents are placed on the top layer.
 * - Spouses are aligned adjacently on the same layer; a family node is placed between them.
 * - Children are placed on the next layer centered under their family.
 * This is a simplified version of the layered DAG algorithm and can be refined further.
 */
public class PedigreeLayoutEngine {

    private final NodeMetrics metrics;
    private final double hGap;
    private final double vGap;

    public PedigreeLayoutEngine() {
        this(new NodeMetrics(), 40.0, 80.0);
    }

    public PedigreeLayoutEngine(NodeMetrics metrics, double horizontalGap, double verticalGap) {
        this.metrics = metrics;
        this.hGap = horizontalGap;
        this.vGap = verticalGap;
    }

    public LayoutResult computeLayout(ProjectRepository.ProjectData data) {
        LayoutResult result = new LayoutResult();
        if (data == null) return result;

        Map<String, Individual> individuals = new LinkedHashMap<>();
        Map<String, Family> families = new LinkedHashMap<>();
        for (Individual i : data.individuals) individuals.put(i.getId(), i);
        for (Family f : data.families) families.put(f.getId(), f);

        // Build child -> families mapping and find individuals with parents
        Set<String> individualsWithParents = new HashSet<>();
        Map<String, List<String>> familyChildren = new HashMap<>();
        Map<String, List<String>> personFamilies = new HashMap<>(); // person -> families where spouse
        for (Family f : families.values()) {
            familyChildren.put(f.getId(), new ArrayList<>(f.getChildrenIds()));
            if (f.getHusbandId() != null) personFamilies.computeIfAbsent(f.getHusbandId(), k -> new ArrayList<>()).add(f.getId());
            if (f.getWifeId() != null) personFamilies.computeIfAbsent(f.getWifeId(), k -> new ArrayList<>()).add(f.getId());
            for (String cid : f.getChildrenIds()) individualsWithParents.add(cid);
        }

        // Roots: individuals without parents
        List<String> roots = new ArrayList<>();
        for (String id : individuals.keySet()) {
            if (!individualsWithParents.contains(id)) roots.add(id);
        }
        if (roots.isEmpty()) roots.addAll(individuals.keySet()); // fallback

        // Assign layers: BFS from roots; spouses stay on same layer
        Map<String, Integer> layerByNode = new HashMap<>(); // includes individuals and families
        Deque<String> queue = new ArrayDeque<>(roots);
        for (String r : roots) layerByNode.put(r, 0);

        while (!queue.isEmpty()) {
            String pid = queue.removeFirst();
            int layer = layerByNode.getOrDefault(pid, 0);

            // Families where this person is a spouse
            for (String famId : personFamilies.getOrDefault(pid, List.of())) {
                Family fam = families.get(famId);
                if (!layerByNode.containsKey(famId)) {
                    layerByNode.put(famId, layer); // place family on same layer as spouses
                }
                // Ensure spouse stays on same layer
                if (fam != null) {
                    if (fam.getHusbandId() != null && !layerByNode.containsKey(fam.getHusbandId())) {
                        layerByNode.put(fam.getHusbandId(), layer);
                        queue.addLast(fam.getHusbandId());
                    }
                    if (fam.getWifeId() != null && !layerByNode.containsKey(fam.getWifeId())) {
                        layerByNode.put(fam.getWifeId(), layer);
                        queue.addLast(fam.getWifeId());
                    }
                    // Place children at next layer
                    for (String cid : familyChildren.getOrDefault(famId, List.of())) {
                        if (!layerByNode.containsKey(cid)) {
                            layerByNode.put(cid, layer + 1);
                            queue.addLast(cid);
                        }
                    }
                }
            }
        }

        // Group nodes by layer
        Map<Integer, List<String>> layers = new TreeMap<>();
        for (Map.Entry<String, Integer> e : layerByNode.entrySet()) {
            layers.computeIfAbsent(e.getValue(), k -> new ArrayList<>()).add(e.getKey());
        }

        // Place nodes horizontally within each layer
        for (Map.Entry<Integer, List<String>> entry : layers.entrySet()) {
            int layer = entry.getKey();
            List<String> nodeIds = entry.getValue();

            // Separate individuals and families in this layer for ordering
            List<String> fams = new ArrayList<>();
            Set<String> placed = new HashSet<>();
            for (String id : nodeIds) {
                if (families.containsKey(id)) fams.add(id);
            }

            double y = layer * (metrics.getHeight("any") + vGap); // uniform height assumption
            double cursorX = 0.0;

            // Place families with spouses together
            for (String famId : fams) {
                Family f = families.get(famId);
                String a = f.getHusbandId();
                String b = f.getWifeId();

                // Husband
                if (a != null && individuals.containsKey(a)) {
                    double wA = metrics.getWidth(a);
                    result.setPosition(a, cursorX, y);
                    placed.add(a);
                    cursorX += wA + hGap;
                }
                // Family node centered between spouses if both present, else just placed at current cursor
                double famW = metrics.getWidth(famId);
                double famX;
                if (a != null && b != null) {
                    double wA = metrics.getWidth(a);
                    double wB = metrics.getWidth(b);
                    double aX = result.getPosition(a).getX();
                    double bX = aX + wA + hGap; // where wife will be placed
                    famX = (aX + wA + bX) / 2.0 - famW / 2.0;
                } else {
                    famX = cursorX;
                }
                result.setPosition(famId, famX, y);
                placed.add(famId);

                // Wife
                if (b != null && individuals.containsKey(b)) {
                    double wB = metrics.getWidth(b);
                    double bX = Math.max(cursorX, result.getPosition(famId).getX() + famW / 2.0 + hGap / 2.0);
                    result.setPosition(b, bX, y);
                    placed.add(b);
                    cursorX = bX + wB + hGap;
                }
            }

            // Place remaining individuals not part of families on this layer
            for (String id : nodeIds) {
                if (placed.contains(id)) continue;
                if (individuals.containsKey(id)) {
                    double w = metrics.getWidth(id);
                    result.setPosition(id, cursorX, y);
                    cursorX += w + hGap;
                    placed.add(id);
                }
            }
        }

        // Place children under each family on the next layer, centered
        for (Family f : families.values()) {
            String famId = f.getId();
            Integer famLayer = layerByNode.get(famId);
            if (famLayer == null) continue;
            double famX = result.getPosition(famId) != null ? result.getPosition(famId).getX() : 0.0;
            double famW = metrics.getWidth(famId);
            double midX = famX + famW / 2.0;

            List<String> children = familyChildren.getOrDefault(famId, List.of());
            if (children.isEmpty()) continue;

            int childLayer = famLayer + 1;
            double y = childLayer * (metrics.getHeight("any") + vGap);

            // Compute total width and start x so that the group is centered under family
            double totalW = 0.0;
            for (String cid : children) totalW += metrics.getWidth(cid);
            double totalGaps = hGap * (children.size() - 1);
            double startX = midX - (totalW + totalGaps) / 2.0;

            double x = startX;
            for (String cid : children) {
                if (!individuals.containsKey(cid)) continue;
                result.setPosition(cid, x, y);
                x += metrics.getWidth(cid) + hGap;
            }
        }

        return result;
    }
}
