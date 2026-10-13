# Especificação Léxica — Mini-ADVPL
**Projeto de Construção de Compiladores — Checkpoint 1**

Este documento é o contrato do scanner (`src/Scanner.java`): o código implementa exatamente o que está descrito aqui.

## 1. Tabela de Categorias de Tokens

Notação: `letra = [A-Za-z]`, `dígito = [0-9]`, `|` = alternativa, `*` = zero ou mais, `+` = uma ou mais, `?` = opcional.

| Token | Notação (Regex / EBNF) | Exemplos válidos | Decisões de Implementação |
| :--- | :--- | :--- | :--- |
| **Identificador** | `(letra \| "_") (letra \| dígito \| "_")*` | `cNome`, `nValor2`, `_status` | **Case-insensitive:** `cNome` e `CNOME` são o mesmo identificador. Pode começar com letra ou `_`, nunca com dígito. Sem limite de tamanho. Pelo *maximal munch*, `iffy` é um identificador (e não `if` + `fy`). |
| **Palavra Reservada** | Mesmo padrão do identificador + busca em tabela hash | `IF`, `local`, `EndDo`, `int` | Case-insensitive. O scanner reconhece o lexema como identificador e depois o procura (em maiúsculas) na tabela de palavras reservadas (Seção 2, item 5). |
| **String** | `'"' (qualquer caractere ≠ '"', ≠ quebra de linha)* '"'` <br> \| `"'" (qualquer caractere ≠ "'", ≠ quebra de linha)* "'"` | `"TOTVS"`, `'Protheus'`, `""`, `"diz 'oi'"` | Aspas duplas ou simples; a string termina na mesma aspa que a abriu, então uma aspa do outro tipo pode aparecer dentro dela. **Sem sequências de escape.** Strings não podem ocupar mais de uma linha. O lexema do token inclui as aspas. Valores do tipo `char` são escritos como strings de um caractere (`'a'`); a verificação do tamanho fica para a análise semântica. |
| **Operador** | `"+" \| "-" \| "*" \| "/" \| ":=" \| "=" \| "==" \| "<>" \| "<" \| "<=" \| ">" \| ">=" \| ".AND." \| ".OR." \| ".NOT."` | `+`, `:=`, `==`, `>=`, `.AND.` | Lista fechada (Seção 2, item 4). `=` e `==` são sinônimos (ambos geram o token `EQ`), como no ADVPL. `:` sozinho não é token. Os operadores lógicos são case-insensitive (`.and.` = `.AND.`). |
| **Delimitador** | `"(" \| ")" \| "," \| ";"` | `f(a, b)` | Usados em declarações e chamadas de funções. |
| **Literal Numérico** | `dígito+ ( "." dígito+ )?` | `100`, `3.14`, `0` | Inteiros e decimais. Sem notação científica e sem hexadecimal. O ponto só faz parte do número se for seguido de dígito: `3.` gera `3` e um erro no `.`. |
| **Literal Lógico** | `"." ("T" \| "t" \| "F" \| "f") "."` | `.T.`, `.f.` | Específico do ADVPL (tipo `bool`). |

## 2. Respostas às Perguntas Obrigatórias

**1. Qual é o alfabeto de entrada?**
Somente caracteres ASCII: letras `A-Z` e `a-z`, dígitos `0-9`, os símbolos `_ + - * / = : < > . ( ) , ; " '` e os caracteres de espaço em branco (espaço, `\t`, `\r`, `\n`). Dentro de strings e comentários, qualquer caractere é aceito (exceto a quebra de linha dentro de strings). Qualquer outro caractere fora de string/comentário (ex.: `@`, `$`, `#`, `{`, `!`, `&`, `ç`, `é`) é um erro léxico.

**2. A linguagem é case-sensitive?**
**Não.** Como no ADVPL tradicional, identificadores, palavras reservadas, operadores lógicos (`.AND.`) e literais lógicos (`.T.`) não diferenciam maiúsculas de minúsculas, e todos seguem a mesma regra. O scanner preserva o lexema original no token, mas compara em maiúsculas na tabela de palavras reservadas; `While`, `WHILE` e `while` geram o mesmo token `WHILE`. A tabela de símbolos (fases seguintes) também deverá comparar nomes em maiúsculas.

**3. Como são delimitados espaços em branco e comentários?**
* **Espaços em branco:** espaço, `\t`, `\r` e `\n` separam tokens e são descartados.
* **Comentários de linha:** começam com `//` e vão até a próxima quebra de linha ou o fim do arquivo.
* **Comentários de bloco:** começam com `/*` e terminam no primeiro `*/`, podendo ocupar várias linhas. **Não podem ser aninhados:** um `/*` dentro de um bloco é tratado como texto comum. Um comentário de bloco sem `*/` até o fim do arquivo é um **erro léxico**.

**4. Regra de desambiguação ("maximal munch"):**
O scanner sempre consome o prefixo válido mais longo, usando `peek()` para olhar o próximo caractere sem consumi-lo:
* `=` seguido de `=` gera `==`; senão, `=`.
* `:` seguido de `=` gera `:=`; `:` sozinho é erro léxico.
* `<` seguido de `=` gera `<=`; seguido de `>` gera `<>`; senão, `<`.
* `>` seguido de `=` gera `>=`; senão, `>`.
* `/` seguido de `/` ou `*` inicia comentário; senão, é o operador de divisão.
* `.` seguido de letras e de outro `.` forma `.T.`, `.F.`, `.AND.`, `.OR.` ou `.NOT.` (se a palavra estiver na tabela). Se não houver o `.` final (ex.: `x .AND y`), o scanner reporta erro no primeiro `.` e retrocede, reconhecendo `AND` como identificador. Palavra desconhecida entre pontos (ex.: `.XOR.`) é erro léxico.
* Em um número, o `.` só é consumido se o caractere seguinte for dígito (lookahead de 2 caracteres com `peekNext()`): `1.5.T.` gera `1.5` e `.T.`.
* Identificadores consomem letras, dígitos e `_` até o primeiro caractere diferente, e só então consultam a tabela de palavras reservadas.

**5. Quais são exatamente as palavras reservadas?**
Lista fechada, com 13 palavras (case-insensitive):

| Grupo | Palavras | Token |
| :--- | :--- | :--- |
| Tipos básicos | `INT`, `DOUBLE`, `BOOL`, `CHAR`, `STRING` | `INT`, `DOUBLE`, `BOOL`, `CHAR`, `STRING_TYPE` |
| Declaração | `LOCAL` | `LOCAL` |
| Seleção | `IF`, `ELSE`, `ENDIF` | `IF`, `ELSE`, `ENDIF` |
| Repetição | `WHILE`, `ENDDO` | `WHILE`, `ENDDO` |
| Funções | `FUNCTION`, `RETURN` | `FUNCTION`, `RETURN` |

`.T.`, `.F.`, `.AND.`, `.OR.` e `.NOT.` não são palavras reservadas: são reconhecidos pelo AFD do ponto (Seção 1).


