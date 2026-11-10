package ast;

/** Operação binária: esq op dir. */
public final class BinExpr extends Expr {
    public final Expr esq;
    public final OpBin op;
    public final Expr dir;

    public BinExpr(Expr esq, OpBin op, Expr dir, int linha, int coluna) {
        super(linha, coluna);
        this.esq = esq;
        this.op = op;
        this.dir = dir;
    }

    @Override
    public <T> T accept(Visitor<T> v) {
        return v.visitBin(this);
    }
}
