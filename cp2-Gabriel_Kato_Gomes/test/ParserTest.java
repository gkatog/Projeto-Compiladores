import java.util.List;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Suíte de testes do parser (Checkpoint 2, Seção 3.3).
 * Mesmo estilo da suíte do CP1: sem JUnit, cada verificação é um "assert" simples.
 * O programa termina com código 1 se algum teste falhar.
 */
public class ParserTest {

    private static int aprovados = 0;
    private static int reprovados = 0;

    public static void main(String[] args) throws Exception {
        System.out.println("==================================================");
        System.out.println("   SUÍTE DE TESTES - CHECKPOINT 2 (PARSER)");
        System.out.println("==================================================");

        // 1. Programas válidos
        testarValidoTiposEDeclaracoes();
        testarValidoEstruturasDeControle();
        testarValidoFuncoesEProcedimentos();
        testarValidoScriptCompleto();

        // 2. Programas inválidos (um erro cada)
        testarErroWhileSemEnddo();
        testarErroDeclaracaoSemTipo();
        testarErroParentesesNaoFechado();
        testarErroExpressaoIncompleta();
        testarErroParametroSemTipo();
        testarErroProgramaSemFunction();

        // 3. Vários erros no mesmo programa (modo pânico)
        testarTracoCompletoEstiloAula6();
        testarMultiplosErrosNaMesmaFuncao();
        testarMultiplosErrosEmFuncoesDiferentes();

        // 4. Nenhum caminho de erro trava o parser
        testarNaoTravaComEntradasAleatorias();

        System.out.println("\n==================================================");
        System.out.printf("   RESULTADO: %d verificações aprovadas, %d reprovadas%n", aprovados, reprovados);
        System.out.println("==================================================");
        System.exit(reprovados > 0 ? 1 : 0);
    }

    // ==================================================================
    // Programas válidos
    // ==================================================================

    private static void testarValidoTiposEDeclaracoes() {
        titulo("VÁLIDO: os cinco tipos básicos, declarações e expressões");
        String fonte =
            "FUNCTION Tipos()\n" +
            "    LOCAL INT nQtd := 10, nTotal\n" +
            "    LOCAL DOUBLE nPreco := 2.5\n" +
            "    LOCAL BOOL lOk := .T.\n" +
            "    LOCAL CHAR cLetra := 'A'\n" +
            "    LOCAL STRING cNome := \"TOTVS\"\n" +
            "    nTotal := -nQtd * (nPreco + 1) / 2 - 3\n" +
            "    lOk := .NOT. lOk .OR. nQtd >= 5 .AND. cLetra <> 'B'\n" +
            "    RETURN()\n";
        verificarAceito(fonte);
    }

    private static void testarValidoEstruturasDeControle() {
        titulo("VÁLIDO: IF/ELSE, IF sem ELSE, WHILE aninhado e ';'");
        String fonte =
            "FUNCTION INT Controle(INT n)\n" +
            "    LOCAL INT i := 0, nPares := 0\n" +
            "    WHILE i < n\n" +
            "        IF i / 2 * 2 == i\n" +
            "            nPares := nPares + 1\n" +
            "        ELSE\n" +
            "            IF i > 100\n" +
            "                i := n\n" +
            "            ENDIF\n" +
            "        ENDIF\n" +
            "        i := i + 1 ; n := n\n" +
            "    ENDDO\n" +
            "    RETURN(nPares)\n";
        verificarAceito(fonte);
    }

    private static void testarValidoFuncoesEProcedimentos() {
        titulo("VÁLIDO: funções, procedimentos, chamadas e RETURN com e sem valor");
        String fonte =
            "FUNCTION DOUBLE Media(INT nA, INT nB)\n" +
            "    RETURN((nA + nB) / 2)\n" +
            "\n" +
            "FUNCTION Avisa(STRING cMsg, CHAR cNivel)\n" +     // procedimento: sem tipo de retorno
            "    IF cNivel == 'E'\n" +
            "        Log(\"ERRO: \", cMsg)\n" +                 // chamada como comando
            "    ENDIF\n" +
            "    RETURN()\n" +                                  // retorno sem valor
            "\n" +
            "FUNCTION BOOL Principal()\n" +                     // função sem parâmetros
            "    LOCAL DOUBLE nM := Media(Media(1, 2), 3) + 1\n" + // chamada aninhada em expressão
            "    Avisa(\"media calculada\", 'I')\n" +
            "    Limpa()\n" +                                   // chamada sem argumentos
            "    RETURN(nM > 0)\n" +
            "\n" +
            "FUNCTION Vazia()\n";                               // corpo vazio (ListaCmd → ε)
        verificarAceito(fonte);
    }

    private static void testarValidoScriptCompleto() {
        titulo("VÁLIDO: script completo (palavras reservadas em minúsculas)");
        String fonte =
            "// Rotina de Calculo e Validacao de Estoque\n" +
            "function bool ControlaEstoque(double nQtd)\n" +
            "    local double nEstoqueAtual := 150.75\n" +
            "    local int nLimiteMinimo := 50\n" +
            "    local bool lAtivo := .T.\n" +
            "    /* Verifica se precisa\n" +
            "       repor o estoque */\n" +
            "    while nEstoqueAtual <= nLimiteMinimo .and. lAtivo == .T.\n" +
            "        nEstoqueAtual := nEstoqueAtual + nQtd\n" +
            "        if nEstoqueAtual <> 150\n" +
            "            nEstoqueAtual := nEstoqueAtual + 1\n" +
            "        else\n" +
            "            lAtivo := .F.\n" +
            "        endif\n" +
            "    enddo\n" +
            "    return(lAtivo)\n";
        verificarAceito(fonte);
    }

    // ==================================================================
    // Programas inválidos (um erro cada)
    // ==================================================================

    private static void testarErroWhileSemEnddo() {
        titulo("ERRO: WHILE sem ENDDO");
        Parser p = analisar(
            "FUNCTION Laco()\n" +
            "    WHILE x < 10\n" +
            "        x := x + 1\n" +
            "FUNCTION Outra()\n" +
            "    RETURN()\n");
        verificarErros(p, 1);
        verificarErro(p, 0, "esperado ENDDO, encontrado 'FUNCTION'", 4, 1);
    }

    private static void testarErroDeclaracaoSemTipo() {
        titulo("ERRO: declaração sem tipo");
        Parser p = analisar(
            "FUNCTION F()\n" +
            "    LOCAL nX := 1\n" +
            "    nX := nX + 1\n" +
            "    RETURN()\n");
        verificarErros(p, 1);
        verificarErro(p, 0, "esperado um tipo (INT, DOUBLE, BOOL, CHAR ou STRING), encontrado 'nX'", 2, 11);
    }

    private static void testarErroParentesesNaoFechado() {
        titulo("ERRO: parêntese não fechado");
        Parser p = analisar(
            "FUNCTION F()\n" +
            "    x := (a + b * 2\n" +
            "    y := 3\n" +
            "    RETURN()\n");
        verificarErros(p, 1);
        verificarErro(p, 0, "esperado ')', encontrado 'y'", 3, 5);
    }

    private static void testarErroExpressaoIncompleta() {
        titulo("ERRO: expressão incompleta antes do ENDIF");
        Parser p = analisar(
            "FUNCTION F()\n" +
            "    IF x > 0\n" +
            "        x := x *\n" +
            "    ENDIF\n" +
            "    RETURN()\n");
        verificarErros(p, 1);
        verificarErro(p, 0, "esperado operando, encontrado 'ENDIF'", 4, 5);
    }

    private static void testarErroParametroSemTipo() {
        titulo("ERRO: parâmetro sem tipo");
        Parser p = analisar(
            "FUNCTION INT Dobro(n)\n" +
            "    RETURN(n * 2)\n");
        verificarErros(p, 1);
        verificarErro(p, 0, "esperado tipo do parâmetro ou ')', encontrado 'n'", 1, 20);
    }

    private static void testarErroProgramaSemFunction() {
        titulo("ERRO: comando fora de função");
        Parser p = analisar(
            "x := 1\n" +
            "FUNCTION F()\n" +
            "    RETURN()\n");
        verificarErros(p, 1);
        verificarErro(p, 0, "esperado FUNCTION, encontrado 'x'", 1, 1);
    }

    // ==================================================================
    // Vários erros no mesmo programa
    // ==================================================================

    private static void testarTracoCompletoEstiloAula6() {
        titulo("MÚLTIPLOS ERROS: traço completo, no formato do exemplo da Aula 6");
        // mesmo erro do exemplo da aula ("id + * id"): falta o operando entre '+' e '*'.
        // O parser reporta, sincroniza em FOLLOW(Unario) (que contém '*'), não descarta nada,
        // retorna, e o TermoResto continua consumindo "* b". Depois acha o ')' sobrando.
        String fonte =
            "FUNCTION F()\n" +
            "    x := a + * b\n" +
            "    RETURN(x))\n";
        Parser p = analisar(fonte, Parser.Traco.COMPLETO);
        verificarErros(p, 2);
        verificarErro(p, 0, "esperado operando, encontrado '*'", 2, 14);
        verificarErro(p, 1, "esperado ';' ou início de novo comando, encontrado ')'", 3, 14);
    }

    private static void testarMultiplosErrosNaMesmaFuncao() {
        titulo("MÚLTIPLOS ERROS: quatro erros numa única rodada (traço resumido)");
        String fonte =
            "FUNCTION INT Calc(INT n\n" +          // falta ')'
            "    LOCAL INT r := n *\n" +           // falta operando
            "    WHILE r > 0\n" +
            "        r := r - 1 )\n" +             // ')' sobrando
            "    ENDDO\n" +
            "    ENDIF\n" +                        // ENDIF sem IF
            "    RETURN(r)\n" +
            "FUNCTION Ok()\n" +
            "    RETURN()\n";
        Parser p = analisar(fonte, Parser.Traco.RESUMIDO);
        verificarErros(p, 4);
        verificarErro(p, 0, "esperado ',' ou ')' na lista de parâmetros, encontrado 'LOCAL'", 2, 5);
        verificarErro(p, 1, "esperado operando, encontrado 'WHILE'", 3, 5);
        verificarErro(p, 2, "esperado ';' ou início de novo comando, encontrado ')'", 4, 20);
        verificarErro(p, 3, "esperado FUNCTION ou fim do arquivo, encontrado 'ENDIF'", 6, 5);
    }

    private static void testarMultiplosErrosEmFuncoesDiferentes() {
        titulo("MÚLTIPLOS ERROS: um erro em cada uma de três funções (traço resumido)");
        String fonte =
            "FUNCTION A()\n" +
            "    LOCAL INT x := 1 +\n" +          // erro 1
            "    RETURN()\n" +
            "FUNCTION INT B(INT n)\n" +
            "    IF n > 0\n" +
            "        n := n - 1\n" +
            "    RETURN(n)\n" +                   // erro 2: falta ENDIF
            "FUNCTION C()\n" +
            "    Log(1, , 2)\n" +                 // erro 3: argumento vazio
            "    RETURN()\n";
        Parser p = analisar(fonte, Parser.Traco.RESUMIDO);
        verificarErros(p, 3);
        verificarErro(p, 0, "esperado operando, encontrado 'RETURN'", 3, 5);
        // quem detecta é o Senao: FUNCTION não está em FIRST(Senao) nem em FOLLOW(Senao) = {ENDIF}
        verificarErro(p, 1, "esperado ELSE ou ENDIF, encontrado 'FUNCTION'", 8, 1);
        verificarErro(p, 2, "esperado expressão, encontrado ','", 9, 12);
    }

    // ==================================================================
    // Robustez: nenhum caminho de erro pode entrar em loop infinito
    // ==================================================================

    private static void testarNaoTravaComEntradasAleatorias() throws Exception {
        titulo("ROBUSTEZ: 5000 sequências aleatórias de tokens, nenhuma trava o parser");
        String[] lexemas = {
            "FUNCTION", "INT", "DOUBLE", "BOOL", "CHAR", "STRING", "LOCAL", "IF", "ELSE", "ENDIF",
            "WHILE", "ENDDO", "RETURN", "x", "f", ":=", "==", "<>", "<", ">=", "+", "-", "*", "/",
            "(", ")", ",", ";", ".AND.", ".OR.", ".NOT.", ".T.", "1", "2.5", "'a'", "\"s\"", "\n" };
        Random r = new Random(2026);              // semente fixa: o teste é reproduzível
        ExecutorService exec = Executors.newSingleThreadExecutor();
        int travou = 0, excecao = 0;
        for (int i = 0; i < 5000; i++) {
            StringBuilder sb = new StringBuilder();
            int tamanho = r.nextInt(60);
            for (int k = 0; k < tamanho; k++) sb.append(lexemas[r.nextInt(lexemas.length)]).append(' ');
            String fonte = sb.toString();
            Future<Boolean> f = exec.submit(() -> new Parser(new Scanner(fonte)).parse());
            try {
                f.get(2, TimeUnit.SECONDS);
            } catch (TimeoutException e) {
                travou++;
                f.cancel(true);
            } catch (Exception e) {
                excecao++;
            }
        }
        exec.shutdownNow();
        verificar(travou == 0, "nenhuma entrada travou o parser (travaram: " + travou + ")");
        verificar(excecao == 0, "nenhuma entrada gerou exceção (exceções: " + excecao + ")");
    }

    // ==================================================================
    // Utilitários
    // ==================================================================

    private static Parser analisar(String fonte) {
        return analisar(fonte, Parser.Traco.NENHUM);
    }

    /** Mostra o programa com as linhas numeradas, roda o parser e imprime os erros. */
    private static Parser analisar(String fonte, Parser.Traco traco) {
        String[] linhas = fonte.split("\n");
        for (int i = 0; i < linhas.length; i++) System.out.printf("    %2d | %s%n", i + 1, linhas[i]);
        Parser p = new Parser(new Scanner(fonte));
        p.setTraco(traco);
        if (traco != Parser.Traco.NENHUM) System.out.println("  Traço da análise:");
        p.parse();
        if (traco == Parser.Traco.NENHUM) for (SyntaxError e : p.getErrors()) System.out.println("  " + e);
        return p;
    }

    private static void verificarAceito(String fonte) {
        Parser p = new Parser(new Scanner(fonte));
        boolean ok = p.parse();
        for (SyntaxError e : p.getErrors()) System.out.println("  " + e);
        verificar(ok, "programa aceito, sem erros léxicos nem sintáticos");
    }

    private static void verificarErros(Parser p, int esperado) {
        List<SyntaxError> erros = p.getErrors();
        verificar(erros.size() == esperado,
                  esperado + " erro(s) sintático(s) reportado(s) (obtido: " + erros.size() + ")");
        verificar(p.getLexicalErrors().isEmpty(), "nenhum erro léxico");
    }

    private static void verificarErro(Parser p, int indice, String mensagem, int linha, int coluna) {
        List<SyntaxError> erros = p.getErrors();
        boolean ok = indice < erros.size()
                && erros.get(indice).message.equals(mensagem)
                && erros.get(indice).line == linha
                && erros.get(indice).col == coluna;
        verificar(ok, "erro \"" + mensagem + "\" em " + linha + ":" + coluna
                  + (ok ? "" : " | obtido: " + (indice < erros.size() ? erros.get(indice) : "nenhum")));
    }

    private static void titulo(String nome) {
        System.out.println("\n--- " + nome + " ---");
    }

    private static void verificar(boolean condicao, String descricao) {
        if (condicao) {
            aprovados++;
            System.out.println("  [OK]    " + descricao);
        } else {
            reprovados++;
            System.out.println("  [FALHA] " + descricao);
        }
    }
}