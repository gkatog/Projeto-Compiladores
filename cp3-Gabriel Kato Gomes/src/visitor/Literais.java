package visitor;

/** Como escrever literais de texto, compartilhado pelo impressor e pelo reimpressor. */
final class Literais {
    private Literais() { }

    /** Aspas duplas, a menos que o conteúdo tenha uma aspa dupla (aí usa simples). */
    static String string(String s) {
        return s.indexOf('"') >= 0 ? "'" + s + "'" : "\"" + s + "\"";
    }

    /** Aspas simples, a menos que o caractere seja a própria aspa simples. */
    static String caractere(char c) {
        return c == '\'' ? "\"'\"" : "'" + c + "'";
    }
}
