package ast;

/** Atribuição: alvo := valor. */
public final class AtribCmd extends Cmd {
    public final IdExpr alvo;
    public final Expr valor;

    public AtribCmd(IdExpr alvo, Expr valor, int linha, int coluna) {
        super(linha, coluna);
        this.alvo = alvo;
        this.valor = valor;
    }

    @Override
    public <T> T accept(Visitor<T> v) {
        return v.visitAtrib(this);
    }
}
