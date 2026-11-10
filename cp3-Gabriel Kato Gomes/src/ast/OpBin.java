package ast;

/** Operadores binários, com o símbolo usado na sintaxe concreta. */
public enum OpBin {
    OR(".OR."), AND(".AND."),
    EQ("=="), NEQ("<>"), LT("<"), LE("<="), GT(">"), GE(">="),
    ADD("+"), SUB("-"), MUL("*"), DIV("/");

    public final String simbolo;

    OpBin(String simbolo) { this.simbolo = simbolo; }
}
