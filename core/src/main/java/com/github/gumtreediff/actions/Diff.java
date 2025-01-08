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
 * Copyright 2022 Jean-Rémy Falleri <jr.falleri@gmail.com>
 * Copyright 2022 Raquel Pau <raquelpau@gmail.com>
 */

package com.github.gumtreediff.actions;

import com.github.gumtreediff.actions.model.Action;
import com.github.gumtreediff.actions.model.Move;
import com.github.gumtreediff.actions.model.Rematch;
import com.github.gumtreediff.gen.TreeGenerators;
import com.github.gumtreediff.matchers.*;
import com.github.gumtreediff.tree.Tree;
import com.github.gumtreediff.tree.TreeContext;
import org.eclipse.jdt.core.dom.*;

import java.io.IOException;
import java.io.Reader;
import java.util.*;

/**
 * Class to facilitate the computation of diffs between ASTs.
 */
public class Diff {
    /**
     * The source AST in its context.
     */
    public final TreeContext src;

    /**
     * The destination AST in its context.
     */
    public final TreeContext dst;

    /**
     * The mappings between the two ASTs.
     */
    public final MappingStore mappings;

    /**
     * The edit script between the two ASTs.
     */
    public final EditScript editScript;

    /**
     * Instantiate a diff object with the provided source and destination
     * ASTs, the provided mappings, and the provided editScript.
     */
    public Diff(TreeContext src, TreeContext dst,
                MappingStore mappings, EditScript editScript) {
        this.src = src;
        this.dst = dst;
        this.mappings = mappings;
        this.editScript = editScript;
    }

    /**
     * Compute and return a diff.
     * @param srcFile The path to the source file.
     * @param dstFile The path to the destination file.
     * @param treeGenerator The id of the tree generator to use.
     * @param matcher The id of the the matcher to use.
     * @param properties The set of options.
     * @throws IOException an IO exception is raised in case of IO problems related to the source
     *     or destination file.
     */
    public static Diff compute(String srcFile, String dstFile, String treeGenerator,
                               String matcher, GumtreeProperties properties) throws IOException {
        TreeContext src = TreeGenerators.getInstance().getTree(srcFile, treeGenerator);
        TreeContext dst = TreeGenerators.getInstance().getTree(dstFile, treeGenerator);

        return compute(src, dst, treeGenerator, matcher, properties);
    }

    private static Diff compute(TreeContext src, TreeContext dst, String treeGenerator,
                               String matcher, GumtreeProperties properties) throws IOException {
        Matcher m = Matchers.getInstance().getMatcherWithFallback(matcher);
        m.configure(properties);
        MappingStore mappings = m.match(src.getRoot(), dst.getRoot());
//        Matcher rm = new TokenMatcher();
//        var remappings = rm.match(src.getRoot(), dst.getRoot(), mappings);
        EditScript editScript = new SimplifiedChawatheScriptGenerator().computeActions(mappings);
//        removeUnnecessaryMove(editScript, remappings);
        removeMove(editScript, mappings);
        return new Diff(src, dst, mappings, editScript);
    }

    private static void removeMove(EditScript editScript, MappingStore mappings) {
        List<Move> removeMoves = new ArrayList<>();
        for (var action : editScript) {
            if (action instanceof Move) {
                var srcTree = action.getNode();
                var dstTree = mappings.getDstForSrc(srcTree);
                var nodeType = getNodeTypeAsChild(srcTree);
                // if文の場合はelseStatementとなる箇所に着目する
                if (srcTree.getNodeProperty() == null) {
                    continue;
                }
                Stack<Tree> srcPath = null;
                Stack<Tree> dstPath = null;
                if (srcTree.getNodeProperty().getId().equals("elseStatement") || dstTree.getNodeProperty().getId().equals("elseStatement")) {
                    srcPath = getPathsToIfNode(srcTree);
                    dstPath = getPathsToIfNode(dstTree);
                    // 対応するTopのif文が同じ場合
                } else if (nodeType == Expression.class || nodeType == Type.class || nodeType == Pattern.class || nodeType == Name.class) {
                    // Expressionを上に辿っていった経路を取得
                    srcPath = getPathsToMoveNode(srcTree);
                    dstPath = getPathsToMoveNode(dstTree);
                }

                if (srcPath != null && dstPath != null) {
                    /*
                     親が一致している場合は経路の比較を行う
                     親が一致していない場合は無視する
                     */
                    if (mappings.isSrcMapped(srcPath.peek()) &&
                            mappings.getDstForSrc(srcPath.peek()).equals(dstPath.peek())) {
                        if (comparePaths(srcPath, dstPath, mappings)) {
                            removeMoves.add((Move) action);
                        }
                    }
                }
            }
        }
        for (var move : removeMoves) {
            editScript.remove(move);
        }
        // 削除対象の移動のノードが他の移動に含まれている場合は無視する
        List<Move> moveActions = new ArrayList<>();
        for (var action : editScript) {
            if (action instanceof Move) {
                moveActions.add((Move) action);
            }
        }
        for (var rm : removeMoves) {
            if (isInOtherMove(rm, moveActions)) {
                continue;
            }
            editScript.add(new Rematch(rm.getNode(), mappings.getDstForSrc(rm.getNode())));
        }
    }

    private static boolean isInOtherMove(Move rm, List<Move> moveActions) {
        for (var ma : moveActions) {
            if (ma.getNode().getDescendants().contains(rm.getNode())) {
                return true;
            }
        }
        return false;
    }

    private static java.lang.Class getNodeTypeAsChild(Tree t) {
        var prop = t.getNodeProperty();
        if (prop instanceof ChildPropertyDescriptor cpd) {
            return cpd.getChildType();
        } else if (prop instanceof ChildListPropertyDescriptor clpd) {
            return clpd.getElementType();
        } else if (prop instanceof SimplePropertyDescriptor spd) {
            return spd.getValueType();
        }
        return null;
    }

    private static Stack<Tree> getPathsToIfNode(Tree t) {
        Stack<Tree> path = new Stack<>();
        path.push(t);
        t = t.getParent();
        while (t != null && t.getType().toString().equals("IfStatement")) {
            path.push(t);
            t = t.getParent();
        }
        path.push(t);
        return path;
    }

    private static Stack<Tree> getPathsToMoveNode(Tree t) {
        Stack<Tree> path = new Stack<>();
        path.push(t);
        var nodeType = getNodeTypeAsChild(t);
        while (t != null && getNodeTypeAsChild(t) == nodeType) {
            t = t.getParent();
            path.push(t);
        }
        return path;
    }

    private static boolean comparePaths(Stack<Tree> srcPath, Stack<Tree> dstPath, MappingStore mappings) {
        while(1 < srcPath.size()) {
            Tree srcTree = srcPath.pop();
            // マッピングがない場合は削除されるためため気にしない
            if (!mappings.isSrcMapped(srcTree)) {
                continue;
            }
            // マッピング対象が相手の経路に存在しない場合は上と同じく削除予定
            var dstTree = mappings.getDstForSrc(srcTree);
            if (!dstPath.contains(dstTree)) {
                continue;
            }
            // マッピングしている場合は次のノードが同一子ノードへの道を辿っているかを判定する
            var nextTreeInSrc = srcPath.peek();
            var nextTreeInDst = dstPath.get(dstPath.indexOf(dstTree) - 1);
            // InfixExpressionの場合はOperandの位置を気にしない（InfixExpressionの場合は子ノードの名前に種類があるため）
            if (!srcTree.getType().toString().equals("InfixExpression") &&
                    !nextTreeInSrc.getNodeProperty().getId().equals(nextTreeInDst.getNodeProperty().getId())) {
                return false;
            }
        }
        return true;
    }

    private static void removeUnnecessaryMove(EditScript editScript, MappingStore remappings) throws IOException {
        List<Tree> moveTrees = new ArrayList<>();
        List<Move> removes = new ArrayList<>();
        for (Action a : editScript) {
            if (a instanceof Move) {
                if (remappings.isSrcMapped(a.getNode())) {
                    removes.add((Move) a);
                } else {
                    moveTrees.add(a.getNode());
                }
            }
        }
        for (Move m : removes) {
            // 削除対象のMoveが他のMoveに含まれている場合、見た目に変化がないので何もしない
            if (!treeIncludedOtherMove(moveTrees, m.getNode())) {
                editScript.remove(m);
                editScript.add(new Rematch(m.getNode(), remappings.getDstForSrc(m.getNode())));
            }
        }
    }

    private static boolean treeIncludedOtherMove(List<Tree> moveTrees, Tree target) {
        for (Tree t : moveTrees) {
            if (t.getDescendants().contains(target)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Compute and return a diff.
     * @param srcReader The reader to the source file.
     * @param dstReader The reader to the destination file.
     * @param treeGenerator The id of the tree generator to use.
     * @param matcher The id of the the matcher to use.
     * @param properties The set of options.
     * @throws IOException an IO exception is raised in case of IO problems related to the source
     *     or destination file.
     */
    public static Diff compute(Reader srcReader, Reader dstReader, String treeGenerator,
                               String matcher, GumtreeProperties properties) throws IOException {
        TreeContext src = TreeGenerators.getInstance().getTree(srcReader, treeGenerator);
        TreeContext dst = TreeGenerators.getInstance().getTree(dstReader, treeGenerator);
        return compute(src, dst, treeGenerator, matcher, properties);
    }

    /**
     * Compute and return a diff.
     * @param srcFile The path to the source file.
     * @param dstFile The path to the destination file.
     * @param command The executable command in the form: command $FILE.
     * @param matcher The id of the the matcher to use.
     * @param properties The set of options.
     * @throws IOException an IO exception is raised in case of IO problems related to the source
     *     or destination file.
     */
    public static Diff computeWithCommand(String srcFile, String dstFile, String command,
                               String matcher, GumtreeProperties properties) throws IOException {
        TreeContext src = TreeGenerators.getInstance().getTreeFromCommand(srcFile, command);
        TreeContext dst = TreeGenerators.getInstance().getTreeFromCommand(dstFile, command);
        Matcher m = Matchers.getInstance().getMatcherWithFallback(matcher);
        m.configure(properties);
        MappingStore mappings = m.match(src.getRoot(), dst.getRoot());
        EditScript editScript = new SimplifiedChawatheScriptGenerator().computeActions(mappings);
        return new Diff(src, dst, mappings, editScript);
    }

    /**
     * Compute and return a diff.
     * @param srcFile The path to the source file.
     * @param dstFile The path to the destination file.
     * @param treeGenerator The id of the tree generator to use.
     * @param matcher The id of the the matcher to use.
     * @throws IOException an IO exception is raised in case of IO problems related to the source
     *     or destination file.
     */
    public static Diff compute(String srcFile, String dstFile,
                               String treeGenerator, String matcher) throws IOException {
        return compute(srcFile, dstFile, treeGenerator, matcher, new GumtreeProperties());
    }

    /**
     * Compute and return a diff, using the default matcher and tree generators automatically
     * retrieved according to the file extensions.
     * @param srcFile The path to the source file.
     * @param dstFile The path to the destination file.
     * @throws IOException an IO exception is raised in case of IO problems related to the source
     *     or destination file.
     */
    public static Diff compute(String srcFile, String dstFile) throws IOException {
        return compute(srcFile, dstFile, null, null);
    }

    /**
     * Compute and return a all node classifier that indicates which node have
     * been added/deleted/updated/moved.
     */
    public TreeClassifier createAllNodeClassifier() {
        return new AllNodesClassifier(this);
    }

    /**
     * Compute and return a root node classifier that indicates which node have
     * been added/deleted/updated/moved. Only the root note is marked when a whole
     * subtree has been subject to a same operation.
     */
    public TreeClassifier createRootNodesClassifier() {
        return new OnlyRootsClassifier(this);
    }
}
