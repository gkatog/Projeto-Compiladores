package ast;

/**
 * Categoria das expressões: literais, IdExpr, ChamadaExpr, BinExpr e UnExpr.
 * Na análise semântica, esta classe ganha o campo com o tipo da expressão.
 */
public abstract class Expr extends Node {
    protected Expr(int linha, int coluna) {
        super(linha, coluna);
    }
}
