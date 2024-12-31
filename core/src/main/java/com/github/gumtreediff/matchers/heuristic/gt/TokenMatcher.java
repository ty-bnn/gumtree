package com.github.gumtreediff.matchers.heuristic.gt;

import com.github.gumtreediff.matchers.*;
import com.github.gumtreediff.tree.Tree;

import java.util.*;

public class TokenMatcher implements Matcher {
    public TokenMatcher() {}

    @Override
    public MappingStore match(Tree src, Tree dst, MappingStore mappings) {
        MappingStore remappings = new MappingStore(src, dst);
        Stack<Tree> stack = new Stack<>();
        stack.push(src);
        while (!stack.isEmpty()) {
            Tree t = stack.pop();

            // 一番最初に出てきたExpressionを発見
            if (JdtNodes.expressions.contains(t.getType().toString())) {
                Tree candidate = null;
                if (mappings.isSrcMapped(t.getParent())) {
                    var parentInDst = mappings.getDstForSrc(t.getParent());
                    var childrenInDst = parentInDst.getChildren();
                    var max = -1D;
                    for (Tree c : childrenInDst) {
                        var sim = SimilarityMetrics.chawatheSimilarity(t, c, mappings);
                        if (sim > max && JdtNodes.expressions.contains(c.getType().toString())) {
                            max = sim;
                            candidate = c;
                        }
                    }
                }
                if (candidate != null && JdtNodes.expressions.contains(t.getType().toString())) {
                    subTreeMatch(t, candidate, mappings, remappings);
                }
                continue; // これ以降の子孫の確認はとばす
            }

            for (int i = t.getChildren().size() - 1; i >= 0; i--) {
                stack.push(t.getChild(i));
            }
        }

        return remappings;
    }

    private void subTreeMatch(Tree src, Tree dst, MappingStore mappings, MappingStore remappings) {
        var srcLeaves = getUnMappedLeavesFromSrc(src, dst, mappings, remappings);
        var dstLeaves = getUnMappedLeavesFromDst(dst, mappings);
        for (Tree s : srcLeaves) {
            for (Tree d : dstLeaves) {
                if (s.getType().equals(d.getType()) && s.getLabel().equals(d.getLabel())) {
                    mappings.addMapping(s, d);
                    remappings.addMapping(s, d);
                }
            }
        }
    }

    private List<Tree> getUnMappedLeavesFromSrc(Tree src, Tree dst, MappingStore mappings, MappingStore remappings) {
        List<Tree> leaves = new ArrayList<>();
        Stack<Tree> stack = new Stack<>();
        stack.push(src);
        while (!stack.isEmpty()) {
            Tree t = stack.pop();
            if (JdtNodes.bigNodes.contains(t.getLabel())) {
                continue;
            }
            if (mappings.isSrcMapped(t)) {
                var treeInDst = mappings.getDstForSrc(t);
                if (dst.getParent().getDescendants().contains(treeInDst)) {
                    remappings.addMapping(t, treeInDst);
                }
            } else if (t.isLeaf()) {
                leaves.add(t);
            }
            for (int i = t.getChildren().size() - 1; i >= 0; i--) {
                stack.push(t.getChild(i));
            }
        }
        return leaves;
    }

    private List<Tree> getUnMappedLeavesFromDst(Tree smallTree, MappingStore mappings) {
        List<Tree> leaves = new ArrayList<>();
        Stack<Tree> stack = new Stack<>();
        stack.push(smallTree);
        while (!stack.isEmpty()) {
            Tree t = stack.pop();
            if (JdtNodes.bigNodes.contains(t.getLabel())) {
                continue;
            }
            if (t.isLeaf() && !mappings.isDstMapped(t)) {
                leaves.add(t);
            }
            for (int i = t.getChildren().size() - 1; i >= 0; i--) {
                stack.push(t.getChild(i));
            }
        }
        return leaves;
    }
}
