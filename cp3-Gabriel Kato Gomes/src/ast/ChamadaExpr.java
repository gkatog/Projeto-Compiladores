package ast;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Chamada de função dentro de uma expressão: nome(args). */
public final class ChamadaExpr extends Expr {
    public final String nome;
    public final List<Expr> args;

    public ChamadaExpr(String nome, List<Expr> args, int linha, int coluna) {
        super(linha, coluna);
        this.nome = nome;
        this.args = Collections.unmodifiableList(new ArrayList<>(args));
    }

    @Override
    public <T> T accept(Visitor<T> v) {
        return v.visitChamada(this);
    }
}
