/*
 * This file is part of GumTree.
 *
 * GumTree is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * GumTree is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with GumTree.  If not, see <http://www.gnu.org/licenses/>.
 *
 * Copyright 2011-2016 Jean-Rémy Falleri <jr.falleri@gmail.com>
 * Copyright 2011-2016 Floréal Morandat <florealm@gmail.com>
 */

package com.github.gumtreediff.matchers.heuristic.gt;

import java.util.*;

import com.github.gumtreediff.matchers.*;
import com.github.gumtreediff.tree.Tree;
import com.github.gumtreediff.tree.TreeUtils;
import com.github.gumtreediff.tree.Type;
import com.github.gumtreediff.utils.SequenceAlgorithms;

public class SimpleBottomUpMatcher implements Matcher {
    private static final double DEFAULT_SIM_THRESHOLD = Double.NaN;

    protected double simThreshold = DEFAULT_SIM_THRESHOLD;

    public SimpleBottomUpMatcher() {
    }

    @Override
    public void configure(GumtreeProperties properties) {
        simThreshold = properties.tryConfigure(ConfigurationOptions.bu_minsim, simThreshold);
    }

    @Override
    public MappingStore match(Tree src, Tree dst, MappingStore mappings) {
        for (Tree t : src.postOrder()) {
            if (t.isRoot()) {
                mappings.addMapping(t, dst);
                lastChanceMatch(mappings, t, dst);
                break;
            }
            else if (!(mappings.isSrcMapped(t) || t.isLeaf())) {
                List<Tree> candidates = getDstCandidates(mappings, t);
                Tree best = null;
                var max = -1D;
                var tSize = t.getDescendants().size();

                for (var candidate : candidates) {
                    var threshold = Double.isNaN(simThreshold)
                            ? 1D / (1D + Math.log(candidate.getDescendants().size() + tSize))
                            : simThreshold;
                    var sim = SimilarityMetrics.chawatheSimilarity(t, candidate, mappings);
                    if (sim > max && sim >= threshold) {
                        max = sim;
                        best = candidate;
                    }
                }

                if (best != null) {
                    lastChanceMatch(mappings, t, best);
                    mappings.addMapping(t, best);
                }
            }
            else if (mappings.isSrcMapped(t) && mappings.hasUnmappedSrcChildren(t)
                       && mappings.hasUnmappedDstChildren(mappings.getDstForSrc(t)))
                lastChanceMatch(mappings, t, mappings.getDstForSrc(t));
        }
        return mappings;
    }

    protected List<Tree> getDstCandidates(MappingStore mappings, Tree src) {
        List<Tree> seeds = new ArrayList<>();
        for (Tree c : src.getDescendants()) {
            Tree m = mappings.getDstForSrc(c);
            if (m != null)
                seeds.add(m);
        }
        List<Tree> candidates = new ArrayList<>();
        Set<Tree> visited = new HashSet<>();
        for (var seed : seeds) {
            while (seed.getParent() != null) {
                var parent = seed.getParent();
                if (visited.contains(parent))
                    break;
                visited.add(parent);
                if (parent.getType() == src.getType() && !mappings.isDstMapped(parent) && !parent.isRoot())
                    candidates.add(parent);
                seed = parent;
            }
        }

        return candidates;
    }

    protected void lastChanceMatch(MappingStore mappings, Tree src, Tree dst) {
        lcsEqualMatching(mappings, src, dst);
        lcsStructureMatching(mappings, src, dst);
        histogramMatching(mappings, src, dst);
    }

    protected void lcsEqualMatching(MappingStore mappings, Tree src, Tree dst) {
        List<Tree> unmappedSrcChildren = new ArrayList<>();
        for (Tree c : src.getChildren())
            if (!mappings.isSrcMapped(c))
                unmappedSrcChildren.add(c);

        List<Tree> unmappedDstChildren = new ArrayList<>();
        for (Tree c : dst.getChildren())
            if (!mappings.isDstMapped(c))
                unmappedDstChildren.add(c);

        List<int[]> lcs = SequenceAlgorithms.longestCommonSubsequenceWithIsomorphism(
                unmappedSrcChildren, unmappedDstChildren);
        for (int[] x : lcs) {
            var t1 = unmappedSrcChildren.get(x[0]);
            var t2 = unmappedDstChildren.get(x[1]);
            if (mappings.areSrcsUnmapped(TreeUtils.preOrder(t1)) && mappings.areDstsUnmapped(
                    TreeUtils.preOrder(t2)))
                mappings.addMappingRecursively(t1, t2);
        }
    }

    protected void lcsStructureMatching(MappingStore mappings, Tree src, Tree dst) {
        List<Tree> unmappedSrcChildren = new ArrayList<>();
        for (Tree c : src.getChildren())
            if (!mappings.isSrcMapped(c))
                unmappedSrcChildren.add(c);

        List<Tree> unmappedDstChildren = new ArrayList<>();
        for (Tree c : dst.getChildren())
            if (!mappings.isDstMapped(c))
                unmappedDstChildren.add(c);

        List<int[]> lcs = SequenceAlgorithms.longestCommonSubsequenceWithIsostructure(
                unmappedSrcChildren, unmappedDstChildren);
        for (int[] x : lcs) {
            var t1 = unmappedSrcChildren.get(x[0]);
            var t2 = unmappedDstChildren.get(x[1]);
            if (mappings.areSrcsUnmapped(
                    TreeUtils.preOrder(t1)) && mappings.areDstsUnmapped(TreeUtils.preOrder(t2)))
                mappings.addMappingRecursively(t1, t2);
        }
    }

    protected void histogramMatching(MappingStore mappings, Tree src, Tree dst) {
        Map<Type, List<Tree>> srcHistogram = new HashMap<>();
        for (var c :  src.getChildren()) {
            if (mappings.isSrcMapped(c))
                continue;
            srcHistogram.putIfAbsent(c.getType(), new ArrayList<>());
            srcHistogram.get(c.getType()).add(c);
        }

        Map<Type, List<Tree>> dstHistogram = new HashMap<>();
        for (var c : dst.getChildren()) {
            if (mappings.isDstMapped(c))
                continue;
            dstHistogram.putIfAbsent(c.getType(), new ArrayList<>());
            dstHistogram.get(c.getType()).add(c);
        }

        for (Type t : srcHistogram.keySet()) {
            if (dstHistogram.containsKey(t) && srcHistogram.get(t).size() == 1 && dstHistogram.get(t).size() == 1) {
                var srcChild = srcHistogram.get(t).get(0);
                var dstChild = dstHistogram.get(t).get(0);
                mappings.addMapping(srcChild, dstChild);
                lastChanceMatch(mappings, srcChild, dstChild);
            }
        }
    }
}
