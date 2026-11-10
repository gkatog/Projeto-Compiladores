package ast;

/** Operação unária: op operando (.NOT. ou - unário). */
public final class UnExpr extends Expr {
    public final OpUn op;
    public final Expr operando;

    public UnExpr(OpUn op, Expr operando, int linha, int coluna) {
        super(linha, coluna);
        this.op = op;
        this.operando = operando;
    }

    @Override
    public <T> T accept(Visitor<T> v) {
        return v.visitUn(this);
    }
}
