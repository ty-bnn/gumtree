package com.github.gumtreediff.matchers.heuristic.gt;

import com.github.gumtreediff.matchers.GumtreeProperties;
import com.github.gumtreediff.matchers.MappingStore;
import com.github.gumtreediff.matchers.Matcher;
import com.github.gumtreediff.matchers.SimilarityMetrics;
import com.github.gumtreediff.tree.Tree;

import java.util.*;

public class TokenMatcher implements Matcher {
    public TokenMatcher() {}
    final private Set<String> expressions = Set.of(
            "Annotation", "ArrayAccess", "ArrayCreation", "ArrayInitializer", "Assignment", "BooleanLiteral",
            "CaseDefaultExpression", "CastExpression", "CharacterLiteral", "ClassInstanceCreation", "ConditionalExpression",
            "FieldAccess", "InfixExpression", "InstanceofExpression", "LambdaExpression", "MethodInvocation", "MethodReference",
            "ModuleQualifiedName", "QualifiedName", "SimpleName", "NullLiteral", "NumberLiteral", "ParenthesizedExpression",
            "EitherOrMultiPattern", "GuardedPattern", "NullPattern", "RecordPattern", "TypePattern",
            "PatternInstanceofExpression", "PostfixExpression", "PrefixExpression", "StringLiteral", "SuperFieldAccess",
            "SuperMethodInvocation", "SwitchExpression", "TextBlock", "ThisExpression", "TypeLiteral", "VariableDeclarationExpression"
    );

    final private Set<String> bigNodes = Set.of(
            "CompilationUnit", "ImportDeclaration", "AnnotationTypeDeclaration", "EnumDeclaration",
            "RecordDeclaration", "TypeDeclaration", "AnnotationTypeMemberDeclaration", "EnumConstantDeclaration",
            "FieldDeclaration", "Initializer", "MethodDeclaration", "ModuleDeclaration",
            "PackageDeclaration", "ModulePackageAccess", "ProvidesDirective", "RequiresDirective", "UsesDirective",
            "ExportsDirective", "OpensDirective", "Block", "AssertStatement", "BreakStatement", "ConstructorInvocation",
            "ContinueStatement", "DoStatement", "EmptyStatement", "EnhancedForStatement", "ExpressionStatement",
            "ForStatement", "IfStatement", "LabeledStatement", "ReturnStatement", "SuperConstructorInvocation",
            "SwitchCase", "SwitchStatement", "SynchronizedStatement", "ThrowStatement", "TryStatement",
            "TypeDeclarationStatement", "VariableDeclarationStatement", "WhileStatement", "YieldStatement"
    );

    @Override
    public MappingStore match(Tree src, Tree dst, MappingStore mappings) {
        MappingStore remappings = new MappingStore(src, dst);
        Stack<Tree> stack = new Stack<>();
        stack.push(src);
        while (!stack.isEmpty()) {
            Tree t = stack.pop();

            if (this.expressions.contains(t.getType().toString())) {
                Tree candidate = null;
                if (mappings.isSrcMapped(t)) {
                    candidate = mappings.getDstForSrc(t);
                } else if (mappings.isSrcMapped(t.getParent())) {
                    var parentInDst = mappings.getDstForSrc(t.getParent());
                    var childrenInDst = parentInDst.getChildren();
                    var max = -1D;
                    for (Tree c : childrenInDst) {
                        var sim = SimilarityMetrics.chawatheSimilarity(t, c, mappings);
                        if (sim > max && expressions.contains(c.getType().toString())) {
                            max = sim;
                            candidate = c;
                        }
                    }
                }
                if (candidate != null && this.expressions.contains(t.getType().toString())) {
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
        var srcLeaves = getUnMappedLeavesFromSrc(src, mappings);
        var dstLeaves = getUnMappedLeavesFromDst(dst, mappings);
        for (Tree s : srcLeaves) {
            for (Tree d : dstLeaves) {
                if (s.getType().equals(d.getType()) && s.getLabel().equals(d.getLabel())) {
                    remappings.addMapping(s, d);
                }
            }
        }
    }

    private List<Tree> getUnMappedLeavesFromSrc(Tree src, MappingStore mappings) {
        List<Tree> leaves = new ArrayList<>();
        Stack<Tree> stack = new Stack<>();
        stack.push(src);
        while (!stack.isEmpty()) {
            Tree t = stack.pop();
            if (bigNodes.contains(t.getLabel())) {
                continue;
            }
            if (t.isLeaf() && !mappings.isSrcMapped(t)) {
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
            if (bigNodes.contains(t.getLabel())) {
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
