package ast;

/** Literal lógico .T. ou .F. */
public final class BoolExpr extends Expr {
    public final boolean valor;

    public BoolExpr(boolean valor, int linha, int coluna) {
        super(linha, coluna);
        this.valor = valor;
    }

    @Override
    public <T> T accept(Visitor<T> v) {
        return v.visitBool(this);
    }
}
