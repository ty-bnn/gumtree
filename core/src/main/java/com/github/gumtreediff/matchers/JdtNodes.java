package com.github.gumtreediff.matchers;

import java.util.Set;

public class JdtNodes {
    static final public Set<String> smallNodes = Set.of(
            "PackageDeclaration", "ImportDeclaration", "AnnotationTypeMemberDeclaration", "FieldDeclaration",
            "ProvidesDirective", "RequiresDirective", "ModuleModifier", "UsesDirective", "ExportsDirective",
            "OpensDirective", "MarkerAnnotation", "NormalAnnotation", "MemberValuePair", "SingleMemberAnnotation",
            "ArrayAccess", "ArrayCreation", "ArrayInitializer", "Assignment", "AssignmentOperator", "BooleanLiteral",
            "CaseDefaultExpression", "CastExpression", "CharacterLiteral", "ClassInstanceCreation", "ConditionalExpression",
            "FieldAccess", "InfixExpression", "InfixOperator", "InstanceofExpression", "LambdaExpression", "MethodInvocation",
            "creationReference", "expressionMethodReference", "superMethodReference", "typeMethodReference",
            "ModuleQualifiedName", "QualifiedName", "SimpleName", "NullLiteral", "NumberLiteral", "ParenthesizedExpression",
            "EitherOrMultiPattern", "GuardedPattern", "NullPattern", "RecordPattern", "TypePattern",
            "PatternInstanceofExpression", "PostfixExpression", "PrefixExpression", "StringLiteral", "SuperFieldAccess",
            "SuperMethodInvocation", "SwitchExpression", "TextBlock", "ThisExpression", "TypeLiteral", "VariableDeclarationExpression",
            "AssertStatement", "BreakStatement", "ConstructorInvocation", "ContinueStatement", "EmptyStatement", "ExpressionStatement",
            "LabeledStatement", "ReturnStatement", "SuperConstructorInvocation", "SwitchCase", "ThrowStatement",
            "TypeDeclarationStatement", "VariableDeclarationStatement", "YieldStatement", "SingleVariableDeclaration",
            "VariableDeclarationFragment", "Javadoc", "DocElement", "TagElement", "MemberRef", "MethodRef", "MethodRefParameter",
            "Modifier", "NameQualifiedType", "PrimitiveType", "QualifiedType", "SimpleType", "WildcardType",
            "ArrayType", "IntersectionType", "ParameterizedType", "UnionType", "TypeParameter", "Dimension"
    );

    static final public Set<String> bigNodes = Set.of(
            "CompilationUnit", "AnnotationTypeDeclaration", "EnumDeclaration", "RecordDeclaration", "TypeDeclaration",
            "EnumConstantDeclaration", "Initializer", "MethodDeclaration", "ModuleDeclaration",
            "Block", "DoStatement", "EnhancedForStatement", "ForStatement", "IfStatement", "SwitchStatement",
            "SynchronizedStatement", "TryStatement", "CatchClause", "WhileStatement", "AnonymousClassDeclaration"

    );
}
