package com.github.gumtreediff.matchers.heuristic.gt;

import com.github.gumtreediff.matchers.*;
import com.github.gumtreediff.tree.Tree;
import com.github.gumtreediff.utils.Pair;

import java.util.*;

public class TokenMatcher implements Matcher {
    public TokenMatcher() {}

    @Override
    public MappingStore match(Tree src, Tree dst, MappingStore mappings) {
        MappingStore remappingsForMove = new MappingStore(src, dst);
        Stack<Tree> stack = new Stack<>();
        stack.push(src);
        while (!stack.isEmpty()) {
            Tree t = stack.pop();

            // 一番最初に出てきたExpressionを発見
            if (JdtNodes.smallNodes.contains(t.getType().toString())) {
                // TODO: candidateが被る場合があるため一対一マッチする必要あり（0011.html, 0012.html参照）
                Tree candidate = null;
                // 同じ型じゃないとマッチしていないため、遡ってマッチする
                Tree treeInDst = null;
                treeInDst = mappings.getDstForSrc(t);
                if (mappings.isSrcMapped(t.getParent())) {
                    var parentInDst = mappings.getDstForSrc(t.getParent());
                    var childrenInDst = parentInDst.getChildren();
                    var max = -1D;
                    for (Tree c : childrenInDst) {
                        var sim = SimilarityMetrics.chawatheSimilarity(t, c, mappings);
                        if (sim > max && JdtNodes.smallNodes.contains(c.getType().toString())) {
                            max = sim;
                            candidate = c;
                        }
                    }
                }
                if (candidate != null && JdtNodes.smallNodes.contains(t.getType().toString())) {
//                    rematchTokens(t, candidate, mappings);
                    doNotMove(t, candidate, mappings, remappingsForMove);
                }
                continue; // これ以降の子孫の確認はとばす
            }

            for (int i = t.getChildren().size() - 1; i >= 0; i--) {
                stack.push(t.getChild(i));
            }
        }

        return remappingsForMove;
    }

    private void doNotMove(Tree src, Tree dst, MappingStore mappings, MappingStore remappings) {
        Stack<Tree> stack = new Stack<>();
        for (int i = 0; i < src.getChildren().size(); i++) {
            stack.push(src.getChildren().get(i));
        }
        while (!stack.isEmpty()) {
            Tree t = stack.pop();
            if (mappings.isSrcMapped(t)) {
                var treeInDst = mappings.getDstForSrc(t);
                if (dst.getDescendants().contains(treeInDst) || dst.equals(treeInDst)) {
                    remappings.addMapping(t, treeInDst);
                }
            }
            for (int i = t.getChildren().size() - 1; i >= 0; i--) {
                stack.push(t.getChild(i));
            }
        }
    }

    /*
    <キーアイデア>
    Top-downにおける閾値の制限とBottom-Upにおける親の型の不一致にによってマッチングされていないトークンをマッチングしたい。
    Recoveryでは検出されないトークンをマッチすることが主目的。
    （ソースコード中の見た目は同じでも構文木では型が異なる場合がある）
    また、完全に一致しているトークン（型も値も同じ場合）以外はもう一度マッチングを行う。
    nits) 他のチャンクに移動している完全一致トークンも、同一チャンク内で完全一致が見つかればマッチングし直したい。
    つまり、構造だけで一致しているトークン（型は同じだが値が異なる場合）は同じチャンク内に値が完全に一致しているトークンがあるのなら方を優先して再度マッチングさせたい。
    注意：全てのチャンクに対して実施する際に元の一致関係を大幅に影響が出るのは避けたい。例えば、元々全部が完全一致しているチャンクに対しても完全一致として出したい。（getLeavesFromSrcで完全マッチは無視してるから大丈夫か...?）
     */
    private void rematchTokens(Tree src, Tree dst, MappingStore mappings) {
        var srcLeaves = getLeavesFromSrc(src, mappings);
        var dstLeaves = getLeavesFromDst(dst, mappings);

        Map<String, List<Tree>> srcKV = new HashMap<>();
        Map<String, List<Tree>> dstKV = new HashMap<>();
        for (Tree t : srcLeaves) {
            var key = t.getType().toString() + t.getLabel();
            if (!srcKV.containsKey(key)) {
                srcKV.put(key, new ArrayList<>());
            }
            srcKV.get(key).add(t);
        }
        for (Tree t : dstLeaves) {
            var key = t.getType().toString() + t.getLabel();
            if (!dstKV.containsKey(key)) {
                dstKV.put(key, new ArrayList<>());
            }
            dstKV.get(key).add(t);
        }

        var pairs = rematchManyTokens(srcKV, dstKV, mappings);
        for (var pair : pairs) {
            if (mappings.isSrcMapped(pair.first)) {
                var dstTree = mappings.getDstForSrc(pair.first);
                mappings.removeMapping(pair.second, dstTree);
            }
            if (mappings.isDstMapped(pair.second)) {
                var srcTree = mappings.getSrcForDst(pair.second);
                mappings.removeMapping(srcTree, pair.first);
            }
            mappings.addMapping(pair.first, pair.second);
        }
    }

    private List<Pair<Tree, Tree>> rematchManyTokens(Map<String, List<Tree>> srcKV, Map<String, List<Tree>> dstKV, MappingStore mappings) {
        List<Pair<Tree, Tree>> treePairs = new ArrayList<>();
        for (String key : srcKV.keySet()) {
            if (!dstKV.containsKey(key)) {
                continue;
            }
            var srcList = srcKV.get(key);
            var dstList = dstKV.get(key);
            int[][] scores = new int[srcList.size()][dstList.size()];
            for (int i = 0; i < srcList.size(); i++) {
                var srcElt = srcList.get(i);
                for (int j = 0; j < dstList.size(); j++) {
                    var dstElt = dstList.get(j);
                    if (mappings.isSrcMapped(srcElt.getParent()) && mappings.getDstForSrc(srcElt.getParent()).equals(dstElt.getParent())) {
                        scores[i][j] += 2;
                    }
                    if (mappings.isSrcMapped(srcElt) && mappings.getDstForSrc(srcElt).equals(dstElt)) {
                        scores[i][j]++;
                    }
                }
            }
            var indexPairs = getUniquePairsInDescendingOrder(scores);
            for (var pair : indexPairs) {
                treePairs.add(new Pair<>(srcList.get(pair[0]), dstList.get(pair[1])));
            }
        }
        return treePairs;
    }

    public static List<int[]> getUniquePairsInDescendingOrder(int[][] array) {
        List<int[]> elements = new ArrayList<>();
        for (int i = 0; i < array.length; i++) {
            for (int j = 0; j < array[i].length; j++) {
                // 格納形式: { value, row, col }
                elements.add(new int[] { array[i][j], i, j });
            }
        }

        Collections.sort(elements, new Comparator<int[]>() {
            @Override
            public int compare(int[] o1, int[] o2) {
                // 降順にしたいので o2[0] - o1[0] を使う
                return Integer.compare(o2[0], o1[0]);
            }
        });

        Set<Integer> usedRows = new HashSet<>();
        Set<Integer> usedCols = new HashSet<>();

        List<int[]> result = new ArrayList<>();
        for (int[] elem : elements) {
            int val = elem[0];
            int row = elem[1];
            int col = elem[2];

            if (usedRows.contains(row) || usedCols.contains(col)) {
                continue;
            }

            usedRows.add(row);
            usedCols.add(col);

            result.add(new int[] { row, col });
        }

        return result;
    }

    // 値も同じトークンは完全に一致していることにして、rematchの対象から外す
    private List<Tree> getLeavesFromSrc(Tree src, MappingStore mappings) {
        List<Tree> leafs = new ArrayList<>();

        Stack<Tree> stack = new Stack<>();
        for (int i = 0; i < src.getChildren().size(); i++) {
            stack.push(src.getChildren().get(i));
        }
        while (!stack.isEmpty()) {
            Tree t = stack.pop();
            if (t.isLeaf()) {
                // 葉が既にMappingされており、値も同じ場合は飛ばす
                if (mappings.isSrcMapped(t)) {
                    var treeInDst = mappings.getDstForSrc(t);
                    if (treeInDst.getLabel().equals(src.getLabel())) {
                        continue;
                    }
                }
                leafs.add(t);
            }

            for (int i = t.getChildren().size() - 1; i >= 0; i--) {
                stack.push(t.getChild(i));
            }
        }

        return leafs;
    }

    private List<Tree> getLeavesFromDst(Tree dst, MappingStore mappings) {
        List<Tree> leafs = new ArrayList<>();

        Stack<Tree> stack = new Stack<>();
        for (int i = 0; i < dst.getChildren().size(); i++) {
            stack.push(dst.getChildren().get(i));
        }
        while (!stack.isEmpty()) {
            Tree t = stack.pop();
            if (t.isLeaf()) {
                // 葉が既にMappingされており、値も同じ場合は飛ばす
                if (mappings.isSrcMapped(t)) {
                    var treeInDst = mappings.getDstForSrc(t);
                    if (treeInDst.getLabel().equals(dst.getLabel())) {
                        continue;
                    }
                }
                leafs.add(t);
            }

            for (int i = t.getChildren().size() - 1; i >= 0; i--) {
                stack.push(t.getChild(i));
            }
        }

        return leafs;
    }

    private void subTreeMatch(Tree src, Tree dst, MappingStore mappings, MappingStore remappings) {
        var srcLeaves = getUnMappedLeavesFromSrc(src, dst, mappings, remappings);
        var dstLeaves = getUnMappedLeavesFromDst(dst, mappings);
        for (Tree s : srcLeaves) {
            for (Tree d : dstLeaves) {
                if (s.getType().equals(d.getType()) && s.getLabel().equals(d.getLabel()) && !mappings.isDstMapped(d)) {
                    mappings.addMapping(s, d);
                    remappings.addMapping(s, d);
                    break;
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
