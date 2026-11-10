package ast;

/** Literal inteiro. Guarda o texto do lexema; conferir a faixa de valores é tarefa da semântica. */
public final class IntExpr extends Expr {
    public final String texto;

    public IntExpr(String texto, int linha, int coluna) {
        super(linha, coluna);
        this.texto = texto;
    }

    @Override
    public <T> T accept(Visitor<T> v) {
        return v.visitInt(this);
    }
}
