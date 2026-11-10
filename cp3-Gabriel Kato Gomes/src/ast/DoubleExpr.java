package ast;

/** Literal decimal (com '.'). Guarda o texto do lexema. */
public final class DoubleExpr extends Expr {
    public final String texto;

    public DoubleExpr(String texto, int linha, int coluna) {
        super(linha, coluna);
        this.texto = texto;
    }

    @Override
    public <T> T accept(Visitor<T> v) {
        return v.visitDouble(this);
    }
}
