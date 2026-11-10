package ast;

/** Literal entre aspas com exatamente um caractere (tipo char). */
public final class CharExpr extends Expr {
    public final char valor;

    public CharExpr(char valor, int linha, int coluna) {
        super(linha, coluna);
        this.valor = valor;
    }

    @Override
    public <T> T accept(Visitor<T> v) {
        return v.visitChar(this);
    }
}
