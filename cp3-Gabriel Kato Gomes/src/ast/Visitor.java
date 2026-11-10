package ast;

/**
 * Um método visitX() por tipo concreto de nó (Aula 7, slide 11).
 * Cada operação sobre a AST é uma classe que implementa esta interface.
 */
public interface Visitor<T> {
    T visitPrograma(Programa n);
    T visitFuncDecl(FuncDecl n);
    T visitParamDecl(ParamDecl n);
    T visitVarDecl(VarDecl n);
    T visitBloco(Bloco n);
    T visitLocal(LocalCmd n);
    T visitAtrib(AtribCmd n);
    T visitChamadaCmd(ChamadaCmd n);
    T visitIf(IfCmd n);
    T visitWhile(WhileCmd n);
    T visitReturn(ReturnCmd n);
    T visitInt(IntExpr n);
    T visitDouble(DoubleExpr n);
    T visitBool(BoolExpr n);
    T visitChar(CharExpr n);
    T visitString(StringExpr n);
    T visitId(IdExpr n);
    T visitChamada(ChamadaExpr n);
    T visitBin(BinExpr n);
    T visitUn(UnExpr n);
}
