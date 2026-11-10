package ast;

/** WHILE condicao corpo ENDDO. */
public final class WhileCmd extends Cmd {
    public final Expr condicao;
    public final Bloco corpo;

    public WhileCmd(Expr condicao, Bloco corpo, int linha, int coluna) {
        super(linha, coluna);
        this.condicao = condicao;
        this.corpo = corpo;
    }

    @Override
    public <T> T accept(Visitor<T> v) {
        return v.visitWhile(this);
    }
}
