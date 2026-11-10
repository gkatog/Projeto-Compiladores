package ast;

/** Operadores unários, com o símbolo usado na sintaxe concreta. */
public enum OpUn {
    NOT(".NOT."), NEG("-");

    public final String simbolo;

    OpUn(String simbolo) { this.simbolo = simbolo; }
}
