package ast;

/** RETURN(valor). valor == null para RETURN() sem valor. */
public final class ReturnCmd extends Cmd {
    public final Expr valor;

    public ReturnCmd(Expr valor, int linha, int coluna) {
        super(linha, coluna);
        this.valor = valor;
    }

    @Override
    public <T> T accept(Visitor<T> v) {
        return v.visitReturn(this);
    }
}
