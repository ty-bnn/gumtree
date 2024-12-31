package com.github.gumtreediff.matchers;

import java.util.Set;

public class JdtNodes {
    static final public Set<String> expressions = Set.of(
            "Annotation", "ArrayAccess", "ArrayCreation", "ArrayInitializer", "Assignment", "BooleanLiteral",
            "CaseDefaultExpression", "CastExpression", "CharacterLiteral", "ClassInstanceCreation", "ConditionalExpression",
            "FieldAccess", "InfixExpression", "InstanceofExpression", "LambdaExpression", "MethodInvocation", "MethodReference",
            "ModuleQualifiedName", "QualifiedName", "SimpleName", "NullLiteral", "NumberLiteral", "ParenthesizedExpression",
            "EitherOrMultiPattern", "GuardedPattern", "NullPattern", "RecordPattern", "TypePattern",
            "PatternInstanceofExpression", "PostfixExpression", "PrefixExpression", "StringLiteral", "SuperFieldAccess",
            "SuperMethodInvocation", "SwitchExpression", "TextBlock", "ThisExpression", "TypeLiteral", "VariableDeclarationExpression"
    );

    static final public Set<String> bigNodes = Set.of(
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
}
