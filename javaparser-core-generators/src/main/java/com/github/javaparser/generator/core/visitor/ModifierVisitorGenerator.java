package com.github.javaparser.generator.core.visitor;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.expr.BinaryExpr;
import com.github.javaparser.ast.stmt.BlockStmt;
import com.github.javaparser.generator.VisitorGenerator;
import com.github.javaparser.utils.SeparatedItemStringBuilder;
import com.github.javaparser.utils.SourceRoot;
import com.github.javaparser.metamodel.BaseNodeMetaModel;
import com.github.javaparser.metamodel.JavaParserMetaModel;
import com.github.javaparser.metamodel.PropertyMetaModel;

import static com.github.javaparser.utils.CodeGenerationUtils.f;

/**
 * Generates JavaParser's EqualsVisitor.
 */
public class ModifierVisitorGenerator extends VisitorGenerator {
    public ModifierVisitorGenerator(JavaParser javaParser, SourceRoot sourceRoot) {
        super(javaParser, sourceRoot, "com.github.javaparser.ast.visitor", "ModifierVisitor", "Visitable", "A", true);
    }

    @Override
    protected void generateVisitMethodBody(BaseNodeMetaModel node, MethodDeclaration visitMethod, CompilationUnit compilationUnit) {
        BlockStmt body = visitMethod.getBody().get();
        body.getStatements().clear();

        String s1 = "Expression target = (Expression) n.getTarget().accept(this, arg);" +
                "        if (target == null) return null;" +
                "        n.setTarget(target);";

        String s = "n.setExtendedTypes(modifyList(n.getExtendedTypes(), arg));";
        String s2 = "n.getPackageDeclaration().ifPresent(p -> n.setPackageDeclaration((PackageDeclaration) p.accept(this, arg)));";


        for (PropertyMetaModel property : node.getAllPropertyMetaModels()) {
            if (property.isNode()) {
                if (property.isNodeList()) {
                    body.addStatement(f("NodeList<%s> %s = modifyList(n.%s(), arg);",
                            property.getTypeNameGenerified(),
                            property.getName(),
                            property.getGetterMethodName()));
                } else if (property.isOptional()) {
                    body.addStatement(f("%s %s = n.%s().map(s -> (%s) s.accept(this, arg)).orElse(null);",
                            property.getTypeNameGenerified(),
                            property.getName(),
                            property.getGetterMethodName(),
                            property.getTypeNameGenerified()));
                } else {
                    body.addStatement(f("%s %s = (%s) n.%s().accept(this, arg);",
                            property.getTypeNameGenerified(),
                            property.getName(),
                            property.getTypeNameGenerified(),
                            property.getGetterMethodName()));
                }
            }
        }

        if(node.is(BinaryExpr.class)){
            body.addStatement("if (left == null) return right;");
            body.addStatement("if (right == null) return left;");
        }else {
            final SeparatedItemStringBuilder collapseCheck = new SeparatedItemStringBuilder("if(", "||", ") return null;");
            for (PropertyMetaModel property : node.getAllPropertyMetaModels()) {
                if (property.isRequired() && property.isNode()) {
                    if (property.isNodeList()) {
                        if(property.isNonEmpty()){
                            collapseCheck.append(f("%s.isEmpty()", property.getName()));
                        }
                    } else {
                        collapseCheck.append(f("%s==null", property.getName()));
                    }
                }
            }
            if (collapseCheck.hasItems()) {
                body.addStatement(collapseCheck.toString());
            }
        }

        for (PropertyMetaModel property : node.getAllPropertyMetaModels()) {
            if (property.isNode()) {
                body.addStatement(f("n.%s(%s);", property.getSetterMethodName(), property.getName()));
            }
        }
        body.addStatement("return n;");
    }

    public static void main(String[] args) {
        JavaParserMetaModel.getNodeMetaModels().forEach(n -> n.getDeclaredPropertyMetaModels().stream().filter(PropertyMetaModel::isNodeList)
                .forEach(p -> System.out.println(p.getContainingNodeMetaModel().getTypeName()+"." +p.getName())));
    }
}

// TODO FieldDeclaration.variables may not be empty
// TODO VariableDeclarationExpr.variables may not be empty

