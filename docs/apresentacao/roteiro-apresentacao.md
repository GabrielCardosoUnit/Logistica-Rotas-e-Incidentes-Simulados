# Roteiro da apresentação: arquitetura monólito e novidades da semana

Apresentação semanal de 24/11/2026. Slides: `slides-apresentacao.pdf` (nesta pasta) ou o HTML `slides-apresentacao.html` (abre no navegador; setas ← → trocam de slide).

- **Duração-alvo:** 12 a 14 minutos, mais perguntas.
- **Período coberto:** o commit `6e92f93` de 21/11 (os commits de 10/11 já foram apresentados na semana anterior).
- **Divisão sugerida:** três blocos. A tabela abaixo é só uma sugestão, troquem os nomes como a equipe preferir.

| Bloco                     |
| ------------------------- |
| Abertura e sistema        |
| Arquitetura monólito      |
| Novidades da semana       |
| Demonstração e fechamento |

---

## Antes de começar (checklist)

- [ ] API já rodando: `mvnw.cmd -pl api spring-boot:run` (perfil dev, rede de Aracaju carregada).
- [ ] Desktop aberto **a partir da raiz do repositório**: `mvnw.cmd -pl desktop compile exec:java`.
- [ ] Plano B: prints ou vídeo curto do fluxo, caso a demo falhe.

---

## Slide 1 · Capa (30 s)

**Tópicos**

- Tema 3: logística, rotas e incidentes simulados.
- Equipe.
- Roteiro: arquitetura → novidades da semana → demo.

**Fala sugerida**

> "Boa tarde. Somos o grupo do tema 3, Logística: rotas e incidentes simulados. Hoje vamos mostrar como o sistema está montado, numa arquitetura monólito em camadas, o que mudou desde a última apresentação e, no fim, uma demonstração ao vivo."

---

## Slide 2 · O sistema (1 min)

**Tópicos**

- Objetivo: menor caminho entre dois pontos numa rede simulada.
- Fluxo em 6 passos: importar, escolher, calcular, bloquear, recalcular, desbloquear.
- Duas redes: Aracaju (20 pontos, 30 vias) e campus da UNIT a pé (13 pontos, 21 caminhos).

**Fala sugerida**

> "O sistema calcula o menor caminho entre dois pontos de uma rede, por distância ou por tempo, usando Dijkstra. Quando um trecho é bloqueado por um incidente, a rota é recalculada sem ele. Os passos em laranja, recalcular e desbloquear, são o que fechamos nesta semana: antes, um bloqueio era permanente."
>
> "A rede é simulada: as coordenadas dos pontos são reais, tiradas do OpenStreetMap, mas servem só para desenhar o mapa. Quem define as ligações são as vias que a equipe criou."

**Atenção:** deixar claro que não é trânsito real; é pergunta comum.

---

## Slide 3 · Arquitetura monólito (1,5 min)

**Tópicos**

- Um único artefato Spring Boot (`api/`): um processo, um `.jar`, um banco.
- Separação **lógica** em camadas (pacotes), não física em serviços.
- Desktop Swing é só cliente: fala com a API por HTTP/JSON e nunca acessa o banco.
- Banco: H2 em dev/test, PostgreSQL via docker compose em prod.

**Fala sugerida**

> "Este é o desenho do sistema. No meio, tracejado em laranja, está o monólito: toda a API é uma única aplicação Spring Boot. Importação da rede, cálculo de rotas e incidentes rodam no mesmo processo e usam o mesmo banco."
>
> "Dentro dele a organização é em camadas: o controller recebe a requisição, o service aplica as regras e controla as transações, o model tem o domínio e o Dijkstra, e o repository cuida da persistência com JPA. DTO, exceções e leitura de arquivos são transversais."
>
> "À esquerda, o cliente desktop. Ele é outro módulo Maven, mas não acessa o banco: tudo passa pela API REST."

**Se perguntarem "não era para ser microsserviço?":**

> "O back-end é um serviço só, que expõe a API. Internamente ele é monolítico: um deploy, um banco. Para o tamanho do domínio, separar em vários serviços traria mais custo do que benefício, que é o próximo slide."

---

## Slide 4 · Por que monólito (1 min)

**Tópicos**

- **Transação única:** bloquear uma via e subir a `versaoRede` acontecem juntos, numa só `@Transactional`.
- **Um deploy:** sobe com um comando; PostgreSQL com docker compose.
- **Contrato claro:** `openapi.yaml` e DTOs isolam o desktop das entidades.
- **Custo aceito:** tudo escala junto e uma falha derruba a API inteira.

**Fala sugerida**

> "Escolhemos o monólito por três motivos. Primeiro, consistência: quando bloqueamos uma via, o incidente e a versão da rede mudam na mesma transação, sem precisar coordenar serviços diferentes. Segundo, simplicidade de execução: um comando sobe tudo. Terceiro, a fronteira importante, que é a API REST, já está bem definida no contrato OpenAPI."
>
> "O custo é que tudo escala junto e uma falha afeta a API inteira. Com cinco endpoints e uma equipe, achamos que valia a pena."

---

## Slide 5 · Camadas no código (45 s)

**Tópicos**

- Pacote raiz `com.logistica.roteirizacao`, projeto Maven multimódulo (`api` e `desktop`).
- Cada pacote tem uma responsabilidade (ler a tabela rapidamente, sem detalhar linha a linha).
- Dependências apontam para dentro: o model não conhece HTTP nem Spring Web.

**Fala sugerida**

> "No código, as camadas viram pacotes. Não vou ler a tabela toda: o importante é que o controller é fino, só valida e delega; a regra fica no service; e o model, onde está o Dijkstra, não depende de nada da web. Isso facilita testar o algoritmo sozinho."

---

## Slide 6 · Uma requisição atravessando as camadas (1 min)

**Tópicos**

- Exemplo: `POST /rotas` da Rodoviária (P05) até a UNIT (P01).
- controller → service → model → repository → resposta `201`.
- Resultado: P05, P15, P01; 8,5 km; 14,5 min.
- Erro em qualquer camada vira o mesmo JSON, pelo `TratadorErrosApi` (`@ControllerAdvice`).

**Fala sugerida**

> "Para ver as camadas funcionando, um pedido de rota da Rodoviária até a UNIT. O controller valida o pedido. O service confere se os pontos existem e monta o grafo já sem as vias bloqueadas. O CalculadorRota roda o Dijkstra com o critério escolhido: distância ou tempo são duas implementações da mesma interface, então aqui tem polimorfismo. A rota é gravada e volta como 201: 8,5 km passando pelo Distrito Industrial."
>
> "Se não houver caminho, não é erro: a resposta é 201 com status SEM_CAMINHO e uma mensagem dizendo quais vias estão bloqueadas."

---

## Slide 7 · O que mudou nesta semana (30 s)

**Tópicos**

- Commit de 21/11: 93 arquivos, +3.034 / −197 linhas, 1 endpoint novo.
- Quatro entregas: encerramento de incidentes, mapa interativo, Javadoc, relatório final.

**Fala sugerida**

> "Passando para as novidades. Nesta semana entrou um commit grande, com 93 arquivos e cerca de três mil linhas. São quatro entregas, que vou mostrar agora."

---

## Slide 8 · Encerramento de incidentes (1,5 min)

**Tópicos**

- Endpoint novo: `POST /incidentes/{id}/encerramento` → `200`.
- Exemplo V24: livre 8,5 km → bloqueada 11,7 km → encerrada 8,5 km.
- API: incidente fica no histórico (`ativo = false`, `encerradoEm`), `versaoRede` sobe; já encerrado dá `409`, id inexistente `404`.
- Tela: painel 4 ganhou o combo **Bloqueada** e o botão **Desbloquear via** (Alt+U); recálculo automático.

**Fala sugerida**

> "A primeira novidade fecha o ciclo dos incidentes. Antes, dava para bloquear uma via mas não desbloquear. Agora existe o endpoint de encerramento."
>
> "No exemplo, a rota da Rodoviária até a UNIT tem 8,5 km. Bloqueando a V24, ela é recalculada para 11,7 km, desviando pelo ponto P03. Encerrando o incidente, a via volta e a rota retorna aos 8,5 km. O incidente não é apagado: fica no histórico como inativo."
>
> "Na tela, cada via aparece só na lista onde faz sentido: livres para bloquear, bloqueadas para desbloquear. E a última rota é recalculada sozinha."

---

## Slide 9 · Mapa interativo (1 min)

**Tópicos**

- Mapa no centro da janela; resultado numa faixa abaixo.
- Dois cliques: 1º define a origem ("Você está aqui"), 2º define o destino e já calcula.
- Zoom com a roda do mouse, arrastar para mover, teclas + − setas e 0.
- Rota não indicada só por cor: traço grosso, setas, números de ordem, alfinete e legenda (acessibilidade).
- Vias bloqueadas tracejadas; fundo de quarteirões só decorativo.

**Fala sugerida**

> "A segunda novidade é no desktop. O mapa virou o centro da tela e passou a responder ao usuário: dá para escolher origem e destino clicando nos pontos, dar zoom e arrastar."
>
> "Tivemos cuidado com acessibilidade: a rota não é indicada só pela cor, tem traço mais grosso, setas de sentido e números de ordem. Vou mostrar isso funcionando na demo."

---

## Slide 10 · Documentação, testes e relatório (45 s)

**Tópicos**

- Javadoc em todo o código, com `package-info` por pacote; `mvnw compile javadoc:aggregate`.
- Testes novos: `IncidenteControllerTest`, `PainelMapaTest`; ampliados: `JanelaPrincipalTest`, `ClienteApiHttpTest`.
- `openapi.yaml`, `contrato.md` e `gui-telas-e-fluxo.md` atualizados.
- Relatório final em `docs/relatorio` (.docx e .pdf).

**Fala sugerida**

> "Por fim, qualidade e entrega. Todo o código ganhou Javadoc, e um comando gera a documentação da API e do desktop juntos. As funcionalidades novas vieram com testes, e o contrato da API e a documentação das telas foram atualizados. O relatório final também já está no repositório."

---

## Slide 11 · Demonstração ao vivo (3 min)

**Passo a passo (falar enquanto faz)**

1. Mostrar a API rodando e o desktop aberto. _"A API está no perfil dev, que já sobe com a rede de Aracaju."_
2. Clicar em **Importar rede**. Clicar no mapa na **Rodoviária (P05)** e depois na **UNIT (P01)**. _"Primeiro clique é a origem, o segundo é o destino, e a rota já é calculada."_
3. Ler o resultado: **8,5 km, 14,5 min, passando por P15**.
4. No painel 4, escolher **V24** e clicar **Bloquear via**. _"A via fica tracejada e a rota é recalculada: 11,7 km."_
5. No combo **Bloqueada**, escolher V24 e clicar **Desbloquear via**. _"A rota volta para 8,5 km."_
6. **Exportar CSV…** e mostrar o arquivo. Fechar e reabrir a janela: _"as preferências voltam, lidas do config.bin, nosso arquivo binário."_

**Se algo der errado:** não depurar ao vivo. Dizer "vou mostrar pelos prints" e seguir para o plano B.

---

## Slide 12 · Próximos passos e perguntas (30 s + perguntas)

**Tópicos** (preencher antes de apresentar)

- Teste de uso com outra equipe: roteiro pronto em `docs/teste-usabilidade.md`; data: [__/__].
- Levar `develop` para `main` com revisão de outro integrante.
- Apresentação final: [responsável e prazo].

**Fala sugerida**

> "Os próximos passos são o teste de uso com outra equipe, a integração da develop na main com revisão, e a preparação da apresentação final. Obrigado, ficamos abertos a perguntas."

---

## Perguntas prováveis e respostas curtas

| Pergunta                                         | Resposta                                                                                                                                                |
| ------------------------------------------------ | ------------------------------------------------------------------------------------------------------------------------------------------------------- |
| Por que Dijkstra e não A\*?                      | A rede é pequena (até 20 pontos) e os pesos são positivos; Dijkstra é exato e simples de conferir à mão, como na rede de validação de 5 nós dos testes. |
| E se não houver caminho?                         | A API responde 201 com `SEM_CAMINHO` e uma mensagem listando as vias bloqueadas; a tentativa fica registrada.                                           |
| Por que monólito e não microsserviços?           | Domínio pequeno, que muda junto e precisa de transação única; um deploy só. A API REST já é a fronteira se um dia for preciso separar.                  |
| A GUI acessa o banco?                            | Não. Só a API acessa; o desktop usa `HttpClient` em `SwingWorker`, sem travar a tela.                                                                   |
| Onde entram herança e polimorfismo?              | `CriterioRotaBase` com `MenorDistancia` e `MenorTempo`; o `CalculadorRota` recebe qualquer `CriterioRota`. Também a hierarquia de `ApiException`.       |
| Onde estão os arquivos texto e binário?          | CSV na importação da rede e na exportação da rota; `config.bin` com `DataOutputStream`/`DataInputStream` no desktop.                                    |
| O que acontece ao bloquear uma via já bloqueada? | `409 Conflito`; por isso a tela tira a via da lista de bloqueio.                                                                                        |
| Os dados são reais?                              | As coordenadas dos pontos sim (OpenStreetMap); as vias e os pesos são simulados.                                                                        |
