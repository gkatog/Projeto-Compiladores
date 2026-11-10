package ast;

/** Literal entre aspas com 0 ou 2+ caracteres. valor não inclui as aspas. */
public final class StringExpr extends Expr {
    public final String valor;

    public StringExpr(String valor, int linha, int coluna) {
        super(linha, coluna);
        this.valor = valor;
    }

    @Override
    public <T> T accept(Visitor<T> v) {
        return v.visitString(this);
    }
}
