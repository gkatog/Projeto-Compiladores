package ast;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Lista de comandos (ListaCmd): corpo de função, de IF, de ELSE ou de WHILE. */
public final class Bloco extends Cmd {
    public final List<Cmd> comandos;

    public Bloco(List<Cmd> comandos, int linha, int coluna) {
        super(linha, coluna);
        this.comandos = Collections.unmodifiableList(new ArrayList<>(comandos));
    }

    @Override
    public <T> T accept(Visitor<T> v) {
        return v.visitBloco(this);
    }
}
