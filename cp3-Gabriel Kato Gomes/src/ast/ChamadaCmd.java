package ast;

/** Chamada de função/procedimento usada como comando. */
public final class ChamadaCmd extends Cmd {
    public final ChamadaExpr chamada;

    public ChamadaCmd(ChamadaExpr chamada, int linha, int coluna) {
        super(linha, coluna);
        this.chamada = chamada;
    }

    @Override
    public <T> T accept(Visitor<T> v) {
        return v.visitChamadaCmd(this);
    }
}
