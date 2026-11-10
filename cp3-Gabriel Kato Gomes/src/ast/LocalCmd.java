package ast;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** LOCAL Tipo Var, Var, ... — uma declaração com uma ou mais variáveis. */
public final class LocalCmd extends Cmd {
    public final Tipo tipo;
    public final List<VarDecl> vars;

    public LocalCmd(Tipo tipo, List<VarDecl> vars, int linha, int coluna) {
        super(linha, coluna);
        this.tipo = tipo;
        this.vars = Collections.unmodifiableList(new ArrayList<>(vars));
    }

    @Override
    public <T> T accept(Visitor<T> v) {
        return v.visitLocal(this);
    }
}
