package com.github.gumtreediff.matchers.heuristic.gt;

import com.github.gumtreediff.matchers.GumtreeProperties;
import com.github.gumtreediff.matchers.MappingStore;
import com.github.gumtreediff.matchers.Matcher;
import com.github.gumtreediff.tree.Tree;

import java.util.Stack;

public class TokenMatcher implements Matcher {
    public TokenMatcher() {}

    @Override
    public MappingStore match(Tree src, Tree dst, MappingStore mappings) {
        MappingStore remappings = new MappingStore(src, dst);
        Stack<Tree> stack = new Stack<>();
        stack.push(src);
        while (!stack.isEmpty()) {
            Tree t = stack.pop();

            // 式同士マッチしていないという前提
            // TODO: tが既にMappingされていたらどうするの？
            if ((t.getType().toString().equals("SimpleName") || t.getType().toString().equals("MethodInvocation")) && !mappings.isSrcMapped(t)) {
                // もしExpressionの親がMappingされている場合は対応するdstのノードを探す
                // 親がMappingされていない場合はそもそも別のものとして考えて良い...
                if (mappings.isSrcMapped(t.getParent())) {
                    // Mappingされている親の同じ位置にあると仮定
                    var parentInDst = mappings.getDstForSrc(t.getParent());
                    var childIndex = t.getParent().getChildPosition(t);
                    var candidate = parentInDst.getChild(childIndex);
                    if (candidate.getType().toString().equals("ArrayAccess")) {
                        subTreeMatch(t, candidate, mappings, remappings);
                    }
                }
                continue; // これ以降の子孫の確認は飛ばす
            }

            for (int i = t.getChildren().size() - 1; i >= 0; i--) {
                stack.push(t.getChild(i));
            }
        }

        return remappings;
    }

    private void subTreeMatch(Tree src, Tree dst, MappingStore mappings, MappingStore remappings) {
        var srcLeaves = src.getUnMappedLeaves(mappings);
        var dstLeaves = dst.getUnMappedLeaves(mappings);
        for (Tree s : srcLeaves) {
            for (Tree d : dstLeaves) {
                if (s.getType().equals(d.getType()) && s.getLabel().equals(d.getLabel())) {
                    remappings.addMapping(s, d);
                }
            }
        }
    }
}
