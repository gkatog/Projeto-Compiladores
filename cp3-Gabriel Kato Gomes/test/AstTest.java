import ast.AtribCmd;
import ast.Programa;
import visitor.ImpressoraArvore;
import visitor.Reimpressora;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Suíte de testes do Checkpoint 3 (AST e Visitor), no mesmo estilo das anteriores:
 * sem JUnit, cada verificação é um "assert" simples. Termina com código 1 se algo falhar.
 *
 * Rodar a partir da pasta do projeto (os arquivos esperados ficam em test/esperado/).
 */
public class AstTest {

    private static int aprovados = 0;
    private static int reprovados = 0;

    // ==================================================================
    // Programas válidos
    // ==================================================================

    static final String TIPOS =
        "FUNCTION Tipos()\n" +
        "    LOCAL INT nQtd := 10, nTotal\n" +
        "    LOCAL DOUBLE nPreco := 2.5\n" +
        "    LOCAL BOOL lOk := .T.\n" +
        "    LOCAL CHAR cLetra := 'A'\n" +
        "    LOCAL STRING cNome := \"TOTVS\"\n" +
        "    nTotal := -nQtd * (nPreco + 1) / 2 - 3\n" +
        "    lOk := .NOT. lOk .OR. nQtd >= 5 .AND. cLetra <> 'B'\n" +
        "    RETURN()\n";

    static final String CONTROLE =
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

    static final String FUNCOES =
        "FUNCTION DOUBLE Media(INT nA, INT nB)\n" +
        "    RETURN((nA + nB) / 2)\n" +
        "\n" +
        "FUNCTION Avisa(STRING cMsg, CHAR cNivel)\n" +
        "    IF cNivel == 'E'\n" +
        "        Log(\"ERRO: \", cMsg)\n" +
        "    ENDIF\n" +
        "    RETURN()\n" +
        "\n" +
        "FUNCTION BOOL Principal()\n" +
        "    LOCAL DOUBLE nM := Media(Media(1, 2), 3) + 1\n" +
        "    Avisa(\"media calculada\", 'I')\n" +
        "    Limpa()\n" +
        "    RETURN(nM > 0)\n" +
        "\n" +
        "FUNCTION Vazia()\n";

    static final String SOMA =
        "FUNCTION INT Soma(INT n)\n" +
        "    LOCAL INT s := 0\n" +
        "    WHILE n > 0\n" +
        "        IF n <> 3\n" +
        "            s := s + n\n" +
        "        ENDIF\n" +
        "        n := n - 1\n" +
        "    ENDDO\n" +
        "    RETURN(s)\n";

    /** O script completo do CP2 (palavras reservadas em minúsculas, comentários). */
    static final String SCRIPT =
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

    /** Casos de borda para a reimpressão: aspas, unários aninhados, parênteses que importam. */
    static final String BORDAS =
        "FUNCTION STRING Bordas(CHAR c)\n" +
        "    LOCAL STRING s := 'diz \"oi\"', t := \"\", u := \"it's\"\n" +
        "    LOCAL CHAR q := \"'\", r := '\"'\n" +
        "    LOCAL BOOL b := -(.NOT. .F.) == - -1 .OR. .NOT. (1 < 2)\n" +
        "    LOCAL INT k := (1 - (2 - 3)) * -(4 + 5) / ((6))\n" +
        "    LOCAL DOUBLE d := 99999999999999999999.5\n" +
        "    WHILE .F.\n" +
        "    ENDDO\n" +
        "    IF b\n" +
        "    ELSE\n" +
        "    ENDIF\n" +
        "    RETURN(s)\n";

    public static void main(String[] args) throws Exception {
        System.out.println("==================================================");
        System.out.println("   SUÍTE DE TESTES - CHECKPOINT 3 (AST E VISITOR)");
        System.out.println("==================================================");

        // 1. Árvores esperadas (escritas à mão em test/esperado/)
        testarArvoreEsperada("tipos e declarações", TIPOS, "tipos.txt");
        testarArvoreEsperada("controle (IF/ELSE, WHILE)", CONTROLE, "controle.txt");
        testarArvoreEsperada("funções e procedimentos", FUNCOES, "funcoes.txt");
        testarArvoreEsperada("programa da árvore desenhada no ast-design.md", SOMA, "soma.txt");

        // 2. Precedência e associatividade
        testarPrecedencia();

        // 3. A AST não é a árvore de derivação
        testarParentesesRedundantes();

        // 4. Ida e volta
        testarIdaEVolta();

        // 5. Regressão do CP2
        testarRegressaoCP2();

        // 6. Robustez: nenhuma exceção e nenhum laço infinito por causa dos nós
        testarRobustez();

        System.out.println("\n==================================================");
        System.out.printf("   RESULTADO: %d verificações aprovadas, %d reprovadas%n", aprovados, reprovados);
        System.out.println("==================================================");
        System.exit(reprovados > 0 ? 1 : 0);
    }

    // ==================================================================
    // 1. Árvores esperadas
    // ==================================================================

    private static void testarArvoreEsperada(String nome, String fonte, String arquivo) throws Exception {
        titulo("ÁRVORE ESPERADA: " + nome + " (test/esperado/" + arquivo + ")");
        Resultado r = new Parser(new Scanner(fonte)).parse();
        verificar(r.aceito() && r.arvore() != null, "programa aceito e árvore construída");
        if (r.arvore() == null) return;
        String obtida = ImpressoraArvore.imprimir(r.arvore());
        String esperada = lerEsperado(arquivo);
        verificar(obtida.equals(esperada), "impressão igual à árvore esperada"
                  + (obtida.equals(esperada) ? "" : "\n" + diferenca(esperada, obtida)));
    }

    // ==================================================================
    // 2. Precedência e associatividade
    // ==================================================================

    private static void testarPrecedencia() {
        titulo("PRECEDÊNCIA E ASSOCIATIVIDADE");

        // precedências diferentes: o * liga mais forte que o +
        verificarExpressao("a + b * c",
            "Bin(+)\n" +
            "  Id(a)\n" +
            "  Bin(*)\n" +
            "    Id(b)\n" +
            "    Id(c)\n");

        // mesmo operador repetido: associatividade à esquerda, (a - b) - c
        verificarExpressao("a - b - c",
            "Bin(-)\n" +
            "  Bin(-)\n" +
            "    Id(a)\n" +
            "    Id(b)\n" +
            "  Id(c)\n");

        // parênteses mudam a árvore: a - (b - c)
        verificarExpressao("a - (b - c)",
            "Bin(-)\n" +
            "  Id(a)\n" +
            "  Bin(-)\n" +
            "    Id(b)\n" +
            "    Id(c)\n");

        verificar(!arvoreDe("a - b - c").equals(arvoreDe("a - (b - c)")),
                  "a - b - c e a - (b - c) produzem árvores diferentes");

        // níveis lógicos: .AND. liga mais forte que .OR.; relacional mais forte que .AND.
        verificarExpressao("a .OR. b .AND. c < d",
            "Bin(.OR.)\n" +
            "  Id(a)\n" +
            "  Bin(.AND.)\n" +
            "    Id(b)\n" +
            "    Bin(<)\n" +
            "      Id(c)\n" +
            "      Id(d)\n");

        // menos unário liga mais forte que *
        verificarExpressao("-a * b",
            "Bin(*)\n" +
            "  Un(-)\n" +
            "    Id(a)\n" +
            "  Id(b)\n");
    }

    // ==================================================================
    // 3. Parênteses redundantes
    // ==================================================================

    private static void testarParentesesRedundantes() {
        titulo("A AST NÃO É A ÁRVORE DE DERIVAÇÃO: parênteses redundantes");
        verificarExpressao("(((a)))", "Id(a)\n");
        verificar(arvoreDe("(((a)))").equals(arvoreDe("a")), "(((a))) tem a mesma árvore que a");
        verificar(arvoreDe("((a + b)) * (c)").equals(arvoreDe("(a + b) * c")),
                  "((a + b)) * (c) tem a mesma árvore que (a + b) * c");
    }

    // ==================================================================
    // 4. Ida e volta
    // ==================================================================

    private static void testarIdaEVolta() {
        titulo("IDA E VOLTA: ler, reimprimir, ler o reimpresso e reimprimir de novo");
        String[][] programas = {
            {"tipos", TIPOS}, {"controle", CONTROLE}, {"funcoes", FUNCOES},
            {"soma", SOMA}, {"script do CP2", SCRIPT}, {"casos de borda", BORDAS},
        };
        for (String[] p : programas) {
            String erro = idaEVolta(p[1]);
            verificar(erro == null, "ida e volta de \"" + p[0] + "\"" + (erro == null ? "" : " | " + erro));
        }
        // mostra um exemplo da forma canônica
        System.out.println("  Exemplo de reimpressão (programa \"soma\"):");
        String texto = Reimpressora.reimprimir(new Parser(new Scanner(SOMA)).parse().arvore());
        for (String l : texto.split("\n")) System.out.println("    | " + l);
    }

    /** Devolve null se a ida e volta funcionou, ou a descrição do problema. */
    private static String idaEVolta(String fonte) {
        Resultado r1 = new Parser(new Scanner(fonte)).parse();
        if (r1.arvore() == null) return "programa original não foi aceito";
        String texto1 = Reimpressora.reimprimir(r1.arvore());
        Resultado r2 = new Parser(new Scanner(texto1)).parse();
        if (r2.arvore() == null) return "texto reimpresso não foi aceito pelo parser: " + r2.erros() + "\n" + texto1;
        String texto2 = Reimpressora.reimprimir(r2.arvore());
        if (!texto1.equals(texto2)) return "segunda reimpressão diferente da primeira";
        if (!ImpressoraArvore.imprimir(r1.arvore()).equals(ImpressoraArvore.imprimir(r2.arvore())))
            return "as duas árvores têm impressões diferentes";
        return null;
    }

    // ==================================================================
    // 5. Regressão do CP2
    // ==================================================================

    private static void testarRegressaoCP2() {
        titulo("REGRESSÃO DO CP2: os programas inválidos continuam rejeitados, com os mesmos erros");

        regressao("WHILE sem ENDDO",
            "FUNCTION Laco()\n    WHILE x < 10\n        x := x + 1\nFUNCTION Outra()\n    RETURN()\n",
            "4:1 esperado ENDDO, encontrado 'FUNCTION'");
        regressao("declaração sem tipo",
            "FUNCTION F()\n    LOCAL nX := 1\n    nX := nX + 1\n    RETURN()\n",
            "2:11 esperado um tipo (INT, DOUBLE, BOOL, CHAR ou STRING), encontrado 'nX'");
        regressao("parêntese não fechado",
            "FUNCTION F()\n    x := (a + b * 2\n    y := 3\n    RETURN()\n",
            "3:5 esperado ')', encontrado 'y'");
        regressao("expressão incompleta",
            "FUNCTION F()\n    IF x > 0\n        x := x *\n    ENDIF\n    RETURN()\n",
            "4:5 esperado operando, encontrado 'ENDIF'");
        regressao("parâmetro sem tipo",
            "FUNCTION INT Dobro(n)\n    RETURN(n * 2)\n",
            "1:20 esperado tipo do parâmetro ou ')', encontrado 'n'");
        regressao("comando fora de função",
            "x := 1\nFUNCTION F()\n    RETURN()\n",
            "1:1 esperado FUNCTION, encontrado 'x'");
        regressao("dois erros (traço da Aula 6)",
            "FUNCTION F()\n    x := a + * b\n    RETURN(x))\n",
            "2:14 esperado operando, encontrado '*'",
            "3:14 esperado ';' ou início de novo comando, encontrado ')'");
        regressao("quatro erros na mesma função",
            "FUNCTION INT Calc(INT n\n    LOCAL INT r := n *\n    WHILE r > 0\n        r := r - 1 )\n"
            + "    ENDDO\n    ENDIF\n    RETURN(r)\nFUNCTION Ok()\n    RETURN()\n",
            "2:5 esperado ',' ou ')' na lista de parâmetros, encontrado 'LOCAL'",
            "3:5 esperado operando, encontrado 'WHILE'",
            "4:20 esperado ';' ou início de novo comando, encontrado ')'",
            "6:5 esperado FUNCTION ou fim do arquivo, encontrado 'ENDIF'");
        regressao("um erro em cada uma de três funções",
            "FUNCTION A()\n    LOCAL INT x := 1 +\n    RETURN()\nFUNCTION INT B(INT n)\n    IF n > 0\n"
            + "        n := n - 1\n    RETURN(n)\nFUNCTION C()\n    Log(1, , 2)\n    RETURN()\n",
            "3:5 esperado operando, encontrado 'RETURN'",
            "8:1 esperado ELSE ou ENDIF, encontrado 'FUNCTION'",
            "9:12 esperado expressão, encontrado ','");
    }

    private static void regressao(String nome, String fonte, String... errosEsperados) {
        Resultado r;
        try {
            r = new Parser(new Scanner(fonte)).parse();
        } catch (Exception e) {
            verificar(false, nome + ": parse() lançou " + e);
            return;
        }
        List<String> obtidos = new ArrayList<>();
        for (SyntaxError e : r.erros()) obtidos.add(e.line + ":" + e.col + " " + e.message);
        boolean mesmos = obtidos.equals(Arrays.asList(errosEsperados));
        verificar(!r.aceito() && r.arvore() == null && mesmos,
                  nome + ": rejeitado, árvore null e os mesmos " + errosEsperados.length + " erro(s)"
                  + (mesmos ? "" : " | obtido: " + obtidos));
    }

    // ==================================================================
    // 6. Robustez
    // ==================================================================

    private static void testarRobustez() throws Exception {
        titulo("ROBUSTEZ: 10000 entradas aleatórias e mutações dos programas válidos");
        String[] lexemas = {
            "FUNCTION", "INT", "DOUBLE", "BOOL", "CHAR", "STRING", "LOCAL", "IF", "ELSE", "ENDIF",
            "WHILE", "ENDDO", "RETURN", "x", "f", ":=", "==", "<>", "<", ">=", "+", "-", "*", "/",
            "(", ")", ",", ";", ".AND.", ".OR.", ".NOT.", ".T.", "1", "2.5", "'a'", "\"s\"", "\n" };
        String[] base = { TIPOS, CONTROLE, FUNCOES, SOMA, BORDAS };
        Random r = new Random(2026);                 // semente fixa: teste reproduzível
        ExecutorService exec = Executors.newSingleThreadExecutor();
        int travou = 0, excecao = 0, incoerente = 0, idaVoltaFalhou = 0, aceitos = 0;

        for (int i = 0; i < 10000; i++) {
            String fonte;
            if (i % 2 == 0) {                        // sequência aleatória de tokens
                StringBuilder sb = new StringBuilder();
                int tamanho = r.nextInt(60);
                for (int k = 0; k < tamanho; k++) sb.append(lexemas[r.nextInt(lexemas.length)]).append(' ');
                fonte = sb.toString();
            } else {                                 // programa válido com 1 a 3 tokens trocados
                List<String> t = new ArrayList<>(Arrays.asList(base[r.nextInt(base.length)].split(" ")));
                for (int k = 0; k < 1 + r.nextInt(3); k++) {
                    int p = r.nextInt(t.size());
                    switch (r.nextInt(3)) {
                        case 0:  t.remove(p); break;
                        case 1:  t.add(p, lexemas[r.nextInt(lexemas.length)]); break;
                        default: t.set(p, lexemas[r.nextInt(lexemas.length)]);
                    }
                    if (t.isEmpty()) t.add("x");
                }
                fonte = String.join(" ", t);
            }
            final String f = fonte;
            Future<Resultado> futuro = exec.submit(() -> new Parser(new Scanner(f)).parse());
            Resultado res;
            try {
                res = futuro.get(2, TimeUnit.SECONDS);
            } catch (TimeoutException e) {
                travou++; futuro.cancel(true); continue;
            } catch (Exception e) {
                excecao++; continue;
            }
            // a árvore existe se e somente se o programa foi aceito
            if (res.aceito() != (res.arvore() != null)) incoerente++;
            if (res.aceito()) {
                aceitos++;
                try {
                    if (idaEVolta(f) != null) idaVoltaFalhou++;
                } catch (Exception e) {
                    excecao++;
                }
            }
        }
        exec.shutdownNow();
        verificar(travou == 0, "nenhuma entrada travou o parser (travaram: " + travou + ")");
        verificar(excecao == 0, "nenhuma exceção escapou, nem NullPointerException (exceções: " + excecao + ")");
        verificar(incoerente == 0, "árvore != null exatamente quando o programa é aceito (incoerentes: " + incoerente + ")");
        verificar(idaVoltaFalhou == 0, "ida e volta funciona para todos os " + aceitos + " programas aceitos");
    }

    // ==================================================================
    // Utilitários
    // ==================================================================

    /** Árvore impressa de uma expressão, colocada como valor de uma atribuição. */
    private static String arvoreDe(String expressao) {
        Resultado r = new Parser(new Scanner("FUNCTION F()\n    x := " + expressao + "\n")).parse();
        if (r.arvore() == null) return "(não aceito: " + r.erros() + ")";
        Programa p = r.arvore();
        AtribCmd atrib = (AtribCmd) p.funcoes.get(0).corpo.comandos.get(0);
        return ImpressoraArvore.imprimir(atrib.valor);
    }

    private static void verificarExpressao(String expressao, String esperada) {
        String obtida = arvoreDe(expressao);
        verificar(obtida.equals(esperada), "árvore de " + expressao
                  + (obtida.equals(esperada) ? "" : "\n" + diferenca(esperada, obtida)));
    }

    private static String lerEsperado(String arquivo) throws Exception {
        Path p = Paths.get("test", "esperado", arquivo);
        String s = new String(Files.readAllBytes(p), StandardCharsets.UTF_8);
        return s.replace("\r\n", "\n");             // aceita o arquivo salvo com quebra de linha do Windows
    }

    /** Mostra a primeira linha diferente entre o esperado e o obtido. */
    private static String diferenca(String esperada, String obtida) {
        String[] e = esperada.split("\n", -1), o = obtida.split("\n", -1);
        for (int i = 0; i < Math.max(e.length, o.length); i++) {
            String le = i < e.length ? e[i] : "(fim)", lo = i < o.length ? o[i] : "(fim)";
            if (!le.equals(lo))
                return "          primeira diferença na linha " + (i + 1) + ": esperado [" + le + "], obtido [" + lo + "]";
        }
        return "";
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
