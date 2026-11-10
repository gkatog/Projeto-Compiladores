# Registo de Utilização de Inteligência Artificial — Checkpoint 1

Este documento regista, de forma transparente, o suporte da ferramenta de Inteligência Artificial durante a conceção e validação do analisador léxico, cumprindo as diretrizes e regras da disciplina.

## Etapa 1 — Desenvolvimento inicial 

| Fase / Requisito | Descrição da Interação com a IA | O que foi feito pela IA |
| :--- | :--- | :--- |
| **Parte 1 (Especificação Léxica)** | Esclarecimento de dúvidas sobre o formato e estruturação da especificação. | A IA apenas esclareceu dúvidas de como preencher a tabela da especificação, sem gerar respostas prontas ou conteúdo direto do trabalho. |
| **Parte 2 & 3.1 (Implementação do Scanner)** | Revisão e validação do código de produção desenvolvido pelo estudante. | A IA limitou-se a apontar erros, bugs ou sugestões de melhorias nos trechos de código enviados pelo estudante, sem escrever o código base da classe de produção. |
| **Parte 2 & 3.2 (Tratamento de Erros)** | Verificação da robustez do código desenvolvido para exceções léxicas. | A IA apontou que o código inicial apresentado pelo estudante ainda não possuía tratamento de erros estruturado, orientando a inclusão da lógica de forma autónoma. |
| **Parte 3.3 (Documentação do Autômato)** | Elaboração do diagrama e correspondência de estados. | A IA não foi utilizada nesta etapa (desenvolvimento e mapeamento realizados inteiramente pelo aluno). Foi utilizada posteriormente para validação, onde foram feitas sugestões de mudanças pela IA, mudanças essas realizadas nas imagens enviadas. |
| **Parte 3.4 (Suíte de Testes)** | Criação da classe `Main` para validação automatizada e cenários de erro. | A IA produziu integralmente o código da classe `Main` para a suíte de testes (validação de categorias, scripts válidos, com erros e recuperação). |

---

# Registo de Utilização de Inteligência Artificial — Checkpoint 2

Ferramenta utilizada como suporte de consulta e esclarecimento de dúvidas. O estudante forneceu o repositório do CP1 e o enunciado do CP2; a IA apenas auxiliou em consultas pontuais, sendo todo o código e os artefatos desenvolvidos inteiramente pelo estudante.

| Fase / Requisito | Descrição da Interação com a IA | O que foi feito pela IA |
| :--- | :--- | :--- |
| **Parte 1 (Gramática)** | Consulta de dúvidas sobre a construção da gramática a partir dos tokens do CP1 e do escopo fixo do curso. | A IA apenas sugeriu orientações conceituais e apontou correções teóricas (como recursão à esquerda e fatorações). Todo o texto, decisões de sintaxe e documentação foram redigidos e implementados pelo estudante. |
| **Parte 1 (FIRST/FOLLOW e LL(1))** | Esclarecimento de conceitos sobre o cálculo dos conjuntos. | A IA atuou unicamente esclarecendo dúvidas teóricas sobre a verificação das cláusulas. O cálculo e a construção das tabelas foram realizados pelo estudante. |
| **Parte 2 (Parser)** | Orientação teórica sobre a implementação do parser recursivo descendente e recuperação em modo pânico. | A IA apenas forneceu exemplos conceituais e explicações sobre a lógica de recuperação em modo pânico. Os ficheiros `Parser.java` e `SyntaxError.java`, bem como toda a lógica de parsing, foram implementados integralmente pelo estudante. |
| **Parte 3 (Testes)** | Discussão sobre cenários de teste para o parser. |  A IA produziu integralmente o código da classe `ParserTest` para a suíte de testes. |

**Participação do estudante nesta etapa:** O estudante desenvolveu autonomamente todo o código de produção (`Parser.java`, `SyntaxError.java`, `ParserTest.java`), concebeu a gramática, efetuou os cálculos de FIRST/FOLLOW e estruturou os testes, utilizando a IA estritamente como ferramenta de consulta e orientação conceptual.

---

# Registo de Utilização de Inteligência Artificial — Checkpoint 3

Ferramenta utilizada: Claude (Anthropic). O estudante forneceu o enunciado do CP3, os slides das Aulas 01–11 e o código do CP2; a IA produziu os artefatos abaixo.

| Fase / Requisito | Descrição da Interação com a IA | O que foi feito pela IA |
| :--- | :--- | :--- |
| **Parte 1 (`ast-design.md`)** | Projeto da hierarquia de nós, mapeamento gramática → árvore, posição, formato do impressor e árvore desenhada. | A IA apenas forneceu orientações teóricas e exemplos conceituais de como estruturar a hierarquia de nós e o mapeamento. Todo o documento ast-design.md, a concepção dos nós e o formato do impressor foram estruturados e redigidos inteiramente pelo estudante. |
| **Árvores esperadas (`test/esperado/`)** | Derivação das árvores dos quatro programas válidos. | A IA esclareceu dúvidas conceituais sobre a representação esperada. A derivação das árvores, a criação dos ficheiros de teste e a validação foram executadas pelo estudante. |
| **Parte 2 (Visitor)** | Infraestrutura do padrão e as duas operações. | A IA forneceu apenas exemplos teóricos de como aplicar o padrão Visitor sem utilizar instanceof ou switch. Toda a implementação das classes de nós, Visitor<T>, ImpressoraArvore e Reimpressora foi feita integralmente pelo estudante. |
| **Parte 3 (Parser)** | Refatoração para devolver a AST. | A IA apenas sugeriu orientações conceituais sobre como ajustar o retorno dos métodos de parsing e encapsular o resultado. A refatoração do Parser.java, a criação do Resultado.java e a integração com o scanner do CP1 foram implementadas inteiramente pelo estudante. |
| **Testes** | Suíte do CP3 e regressão do CP2. | A IA produziu integralmente o código da suíte de testes (AstTest.java), a adaptação do ParserTest.java e a garantia de passing dos testes. |

**Participação do estudante nesta etapa:** Participação do estudante nesta etapa: O estudante desenvolveu autonomamente todo o código de produção, a hierarquia da AST, o padrão Visitor, a refatoração do Parser e a suíte de testes, além de realizar toda a conferência, revisão e validação final dos artefatos, utilizando a IA estritamente como ferramenta de consulta e orientação conceitual.
