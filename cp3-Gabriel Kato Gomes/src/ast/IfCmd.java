package ast;

/** IF condicao entao [ELSE senao] ENDIF. senao == null quando não há ELSE. */
public final class IfCmd extends Cmd {
    public final Expr condicao;
    public final Bloco entao;
    public final Bloco senao;

    public IfCmd(Expr condicao, Bloco entao, Bloco senao, int linha, int coluna) {
        super(linha, coluna);
        this.condicao = condicao;
        this.entao = entao;
        this.senao = senao;
    }

    @Override
    public <T> T accept(Visitor<T> v) {
        return v.visitIf(this);
    }
}
