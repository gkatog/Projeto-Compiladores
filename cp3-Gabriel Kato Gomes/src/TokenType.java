public enum TokenType {
    INT, DOUBLE, BOOL, CHAR, STRING_TYPE,

    LOCAL, IF, ELSE, ENDIF, WHILE, ENDDO, FUNCTION, RETURN,

    ID,          // nome de variável/função
    NUM,         // literal numérico (inteiro ou decimal)
    LIT_STRING,  // literal de string ("..." ou '...')
    LOGICAL,     // literal lógico (.T. / .F.)

    ASSIGN,   // :=
    EQ,       // = ou ==
    NEQ,      // <>
    LT,       // <
    LE,       // <=
    GT,       // >
    GE,       // >=

    AND,      // .AND.
    OR,       // .OR.
    NOT,      // .NOT.

    PLUS, MINUS, MULT, DIV,

    LPAREN, RPAREN, COMMA, SEMI,

    EOF
}
