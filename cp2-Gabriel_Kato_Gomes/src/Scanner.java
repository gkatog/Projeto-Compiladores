import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class Scanner {

    private enum State {
        Q0_INICIO,
        Q1_ID,
        Q2_INT, Q3_DEC,
        Q4_STR, Q5_STR_FIM,
        Q6_PONTO, Q7_PONTO_PALAVRA, Q8_PONTO_FIM,
        Q9_OP_SIMPLES,
        Q10_IGUAL, Q11_IGUAL_IGUAL,
        Q12_DOIS_PONTOS, Q13_ATRIB,
        Q14_MENOR, Q15_MENOR_IGUAL, Q16_DIFERENTE,
        Q17_MAIOR, Q18_MAIOR_IGUAL
    }

    private enum SkipState {
        C0_INICIO, C1_BARRA, C2_COMENT_LINHA, C3_COMENT_BLOCO, C4_COMENT_BLOCO_ESTRELA
    }

    private static final char EOF_CHAR = '\0';

    private final String source;
    private int pos = 0;
    private int line = 1;
    private int col = 1;

    private final List<LexicalError> errors = new ArrayList<>();

    private static final Map<String, TokenType> KEYWORDS = new HashMap<>();
    private static final Map<String, TokenType> DOT_WORDS = new HashMap<>();

    static {
        KEYWORDS.put("INT", TokenType.INT);
        KEYWORDS.put("DOUBLE", TokenType.DOUBLE);
        KEYWORDS.put("BOOL", TokenType.BOOL);
        KEYWORDS.put("CHAR", TokenType.CHAR);
        KEYWORDS.put("STRING", TokenType.STRING_TYPE);
        KEYWORDS.put("LOCAL", TokenType.LOCAL);
        KEYWORDS.put("IF", TokenType.IF);
        KEYWORDS.put("ELSE", TokenType.ELSE);
        KEYWORDS.put("ENDIF", TokenType.ENDIF);
        KEYWORDS.put("WHILE", TokenType.WHILE);
        KEYWORDS.put("ENDDO", TokenType.ENDDO);
        KEYWORDS.put("FUNCTION", TokenType.FUNCTION);
        KEYWORDS.put("RETURN", TokenType.RETURN);

        DOT_WORDS.put("T", TokenType.LOGICAL);
        DOT_WORDS.put("F", TokenType.LOGICAL);
        DOT_WORDS.put("AND", TokenType.AND);
        DOT_WORDS.put("OR", TokenType.OR);
        DOT_WORDS.put("NOT", TokenType.NOT);
    }

    public Scanner(String source) {
        this.source = source;
    }

    public List<LexicalError> getErrors() {
        return Collections.unmodifiableList(errors);
    }

    
    private char peek() {
        return pos < source.length() ? source.charAt(pos) : EOF_CHAR;
    }

    private char peekNext() {
        return pos + 1 < source.length() ? source.charAt(pos + 1) : EOF_CHAR;
    }

    private char advance() {
        char c = source.charAt(pos++);
        if (c == '\n') {
            line++;
            col = 1;
        } else {
            col++;
        }
        return c;
    }

    private boolean hasNext() {
        return pos < source.length();
    }

    private static boolean isLetra(char c)  { return (c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z'); }
    private static boolean isDigito(char c) { return c >= '0' && c <= '9'; }

    private void error(String message, int l, int c) {
        LexicalError e = new LexicalError(message, l, c);
        errors.add(e);
        System.err.println(e);
    }

  

    private void skipWhitespaceAndComments() {
        SkipState state = SkipState.C0_INICIO;
        int startL = line, startC = col; 

        while (true) {
            char c = peek();
            switch (state) {
                case C0_INICIO:
                    if (c == ' ' || c == '\t' || c == '\r' || c == '\n') {
                        advance();
                    } else if (c == '/' && (peekNext() == '/' || peekNext() == '*')) {
                        startL = line; startC = col;
                        advance();
                        state = SkipState.C1_BARRA;
                    } else {
                        return; 
                    }
                    break;

                case C1_BARRA: 
                    state = (advance() == '/') ? SkipState.C2_COMENT_LINHA : SkipState.C3_COMENT_BLOCO;
                    break;

                case C2_COMENT_LINHA: 
                    if (c == '\n' || !hasNext()) {
                        state = SkipState.C0_INICIO;
                    } else {
                        advance();
                    }
                    break;

                case C3_COMENT_BLOCO:
                    if (!hasNext()) {
                        error("comentário de bloco não fechado até o fim do arquivo", startL, startC);
                        return;
                    }
                    if (advance() == '*') state = SkipState.C4_COMENT_BLOCO_ESTRELA;
                    break;

                case C4_COMENT_BLOCO_ESTRELA: 
                    if (!hasNext()) {
                        error("comentário de bloco não fechado até o fim do arquivo", startL, startC);
                        return;
                    }
                    if (c == '/') {
                        advance();
                        state = SkipState.C0_INICIO; 
                    } else if (c == '*') {
                        advance(); 
                    } else {
                        advance();
                        state = SkipState.C3_COMENT_BLOCO;
                    }
                    break;
            }
        }
    }

 
    public Token nextToken() {
        while (true) {
            skipWhitespaceAndComments();
            if (!hasNext()) {
                return new Token(TokenType.EOF, "", line, col);
            }
            Token t = scanToken();
            if (t != null) return t;
            
        }
    }

    private Token scanToken() {
        final int startPos = pos, startL = line, startC = col;
        State state = State.Q0_INICIO;
        char delim = 0; 

        int markPos = 0, markL = 0, markC = 0;

        while (true) {
            char c = peek();
            switch (state) {

                case Q0_INICIO:
                    if (isLetra(c) || c == '_') { advance(); state = State.Q1_ID; }
                    else if (isDigito(c))       { advance(); state = State.Q2_INT; }
                    else if (c == '"' || c == '\'') { delim = advance(); state = State.Q4_STR; }
                    else if (c == '.')          { advance(); state = State.Q6_PONTO; }
                    else if (c == '=')          { advance(); state = State.Q10_IGUAL; }
                    else if (c == ':')          { advance(); state = State.Q12_DOIS_PONTOS; }
                    else if (c == '<')          { advance(); state = State.Q14_MENOR; }
                    else if (c == '>')          { advance(); state = State.Q17_MAIOR; }
                    else if ("+-*/(),;".indexOf(c) >= 0) { advance(); state = State.Q9_OP_SIMPLES; }
                    else {
                        advance();
                        error("caractere fora do alfabeto '" + c + "'", startL, startC);
                        return null;
                    }
                    break;

                case Q1_ID: // [ACEITA] (letra | dígito | '_')*
                    if (isLetra(c) || isDigito(c) || c == '_') {
                        advance();
                    } else {
                        String lexeme = source.substring(startPos, pos);
                        TokenType type = KEYWORDS.getOrDefault(lexeme.toUpperCase(), TokenType.ID);
                        return new Token(type, lexeme, startL, startC);
                    }
                    break;

                case Q2_INT: // [ACEITA] dígito+
                    if (isDigito(c)) {
                        advance();
                    } else if (c == '.' && isDigito(peekNext())) {
                        // lookahead: só entra na parte decimal se houver dígito depois do ponto
                        // ("3.T." e "3." deixam o ponto para o próximo token)
                        advance();
                        state = State.Q3_DEC;
                    } else {
                        return new Token(TokenType.NUM, source.substring(startPos, pos), startL, startC);
                    }
                    break;

                case Q3_DEC: // [ACEITA] dígito+ '.' dígito+
                    if (isDigito(c)) {
                        advance();
                    } else {
                        return new Token(TokenType.NUM, source.substring(startPos, pos), startL, startC);
                    }
                    break;

                case Q4_STR: // dentro da string, esperando a aspa de fechamento
                    if (!hasNext()) {
                        error("string não fechada até o fim do arquivo", startL, startC);
                        return null;
                    } else if (c == '\n' || c == '\r') {
                        // strings não podem ocupar mais de uma linha; a quebra não é consumida
                        error("string não fechada até o fim da linha", startL, startC);
                        return null;
                    } else if (c == delim) {
                        advance();
                        state = State.Q5_STR_FIM;
                    } else {
                        advance();
                    }
                    break;

                case Q5_STR_FIM: // [ACEITA] lexema inclui as aspas (texto original)
                    return new Token(TokenType.LIT_STRING, source.substring(startPos, pos), startL, startC);

                case Q6_PONTO: // leu '.'; guarda o ponto de retrocesso
                    markPos = pos; markL = line; markC = col;
                    if (isLetra(c)) {
                        advance();
                        state = State.Q7_PONTO_PALAVRA;
                    } else {
                        error("'.' isolado não é um token válido", startL, startC);
                        return null;
                    }
                    break;

                case Q7_PONTO_PALAVRA: // '.' letra+
                    if (isLetra(c)) {
                        advance();
                    } else if (c == '.') {
                        advance();
                        state = State.Q8_PONTO_FIM;
                    } else {
                        // não fechou com ponto: retrocede para logo após o '.' inicial
                        pos = markPos; line = markL; col = markC;
                        error("'.' não inicia um operador lógico nem literal lógico válido", startL, startC);
                        return null;
                    }
                    break;

                case Q8_PONTO_FIM: { // [ACEITA se a palavra estiver na tabela] '.' letra+ '.'
                    String lexeme = source.substring(startPos, pos);
                    String word = lexeme.substring(1, lexeme.length() - 1).toUpperCase();
                    TokenType type = DOT_WORDS.get(word);
                    if (type != null) {
                        return new Token(type, lexeme, startL, startC);
                    }
                    // palavra desconhecida entre pontos (ex.: ".ANDX."): descarta o trecho todo
                    error("operador/literal delimitado por ponto desconhecido '" + lexeme + "'", startL, startC);
                    return null;
                }

                case Q9_OP_SIMPLES: { // [ACEITA] + - * / ( ) , ;
                    char op = source.charAt(startPos);
                    TokenType type;
                    switch (op) {
                        case '+': type = TokenType.PLUS;   break;
                        case '-': type = TokenType.MINUS;  break;
                        case '*': type = TokenType.MULT;   break;
                        case '/': type = TokenType.DIV;    break;
                        case '(': type = TokenType.LPAREN; break;
                        case ')': type = TokenType.RPAREN; break;
                        case ',': type = TokenType.COMMA;  break;
                        default:  type = TokenType.SEMI;   break; // ';'
                    }
                    return new Token(type, String.valueOf(op), startL, startC);
                }

                case Q10_IGUAL: // [ACEITA] =
                    if (c == '=') { advance(); state = State.Q11_IGUAL_IGUAL; }
                    else return new Token(TokenType.EQ, "=", startL, startC);
                    break;

                case Q11_IGUAL_IGUAL: // [ACEITA] ==
                    return new Token(TokenType.EQ, "==", startL, startC);

                case Q12_DOIS_PONTOS: // ':' sozinho não é token
                    if (c == '=') {
                        advance();
                        state = State.Q13_ATRIB;
                    } else {
                        error("':' deve ser seguido de '=' (atribuição ':=')", startL, startC);
                        return null;
                    }
                    break;

                case Q13_ATRIB: // [ACEITA] :=
                    return new Token(TokenType.ASSIGN, ":=", startL, startC);

                case Q14_MENOR: // [ACEITA] <
                    if (c == '=')      { advance(); state = State.Q15_MENOR_IGUAL; }
                    else if (c == '>') { advance(); state = State.Q16_DIFERENTE; }
                    else return new Token(TokenType.LT, "<", startL, startC);
                    break;

                case Q15_MENOR_IGUAL: // [ACEITA] <=
                    return new Token(TokenType.LE, "<=", startL, startC);

                case Q16_DIFERENTE: // [ACEITA] <>
                    return new Token(TokenType.NEQ, "<>", startL, startC);

                case Q17_MAIOR: // [ACEITA] >
                    if (c == '=') { advance(); state = State.Q18_MAIOR_IGUAL; }
                    else return new Token(TokenType.GT, ">", startL, startC);
                    break;

                case Q18_MAIOR_IGUAL: // [ACEITA] >=
                    return new Token(TokenType.GE, ">=", startL, startC);
            }
        }
    }
}
