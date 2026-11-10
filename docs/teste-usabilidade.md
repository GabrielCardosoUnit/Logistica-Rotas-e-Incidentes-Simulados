# Teste de uso com outra equipe

O guia da disciplina pede, para a semana de 16/11: "Teste com outra equipe: registrar dificuldades de uso e demonstrar as melhorias". Este documento tem tudo para conduzir o teste e registrar o resultado.

## 1. Agendamento

Marquem até **13/11**. Assim sobra tempo para corrigir o que aparecer antes da apresentação de 16/11.

| Item | Valor |
|---|---|
| Equipe convidada | _[nome da equipe]_ |
| Participantes (2 ou 3 pessoas que **não** conhecem o nosso sistema) | _[nomes]_ |
| Data e horário | _[dd/mm, hh:mm]_ (cerca de 30 min) |
| Local | _[laboratório / sala]_ |
| Condutor (lê as tarefas, não ajuda) | _[nome]_ |
| Anotadores (preenchem as fichas) | _[nomes]_ |
| Responsável pela máquina (API e desktop rodando) | _[nome]_ |

Em troca, a nossa equipe pode testar o sistema deles. Todas as equipes precisam cumprir esse item.

### Mensagem de convite

> Oi, pessoal! Somos a equipe de **Logística: rotas e incidentes simulados**. O guia pede que cada equipe tenha o sistema testado por outra. Vocês topam testar o nosso (uns 30 minutos, com 2 ou 3 pessoas) em _[data e horário]_, no _[local]_? Em troca, testamos o de vocês quando for melhor para vocês. Não precisa preparar nada: a gente leva as tarefas e só observa e anota.

## 2. Preparação (no dia, antes de começar)

- [ ] Na raiz do repositório: `git pull`. Depois `mvnw.cmd -pl api spring-boot:run` (perfil dev, já com a rede de Aracaju).
- [ ] Apagar o `config.bin` da raiz, para a tela abrir como na primeira execução.
- [ ] Abrir o desktop: `mvnw.cmd -pl desktop compile exec:java`. Conferir que a janela abre e que os CSVs vêm preenchidos.
- [ ] Imprimir ou abrir as fichas da seção 4, uma por participante.
- [ ] Deixar o README e os documentos **fechados**: o participante usa só a tela.

## 3. Como conduzir

1. Explique só o objetivo do sistema, em uma frase: "calcula o melhor caminho entre dois locais de Aracaju e recalcula quando uma via é bloqueada".
2. Peça para o participante **pensar em voz alta** enquanto usa.
3. **Não ajude nem explique a tela.** Se ele travar por mais de 2 minutos, anote "não concluiu" e passe para a próxima tarefa.
4. Leia uma tarefa por vez, exatamente como está escrita abaixo.

### Tarefas

| # | Tarefa (leia assim para o participante) | Sucesso quando… | Tempo máx. |
|---|---|---|---|
| T1 | "Carregue a rede de pontos e vias que já está indicada na tela." | a barra mostra "Rede importada: 20 pontos, 30 vias" | 2 min |
| T2 | "Descubra o caminho mais curto, em quilômetros, da Rodoviária até a UNIT." | o resultado mostra 8,5 km, passando pelo Distrito Industrial | 3 min |
| T3 | "Do Parque da Cidade até os Arcos da Orla: qual caminho é o mais rápido, em minutos?" | troca o critério para tempo e chega a 31,3 min (pela Ponte Construtor João Alves) | 3 min |
| T4 | "Houve um alagamento na via entre o Distrito Industrial e a UNIT. Registre isso e veja como fica a rota da Rodoviária até a UNIT." | bloqueia a V24 e vê a rota recalculada com 11,7 km | 3 min |
| T5 | "Salve essa rota num arquivo na Área de Trabalho." | usa "Exportar CSV…" e o arquivo é criado | 2 min |
| T6 | "Feche o programa e abra de novo. A última rota que você consultou continua lá?" | depois de importar, origem, destino e critério voltam preenchidos | 2 min |
| T7 | "Tente calcular uma rota em que a origem e o destino sejam o mesmo lugar." | entende a mensagem de erro sem ajuda | 1 min |

Entre T5 e T6, o responsável pela máquina reabre o desktop (`mvnw.cmd -pl desktop compile exec:java`), se o participante não souber fazer isso.

## 4. Ficha de observação (uma por participante)

Participante: _[nome]_. Equipe: _[equipe]_. Anotador: _[nome]_. Data: _[dd/mm]_.

| Tarefa | Concluiu? (sim / com dificuldade / não) | Tempo | Onde travou ou errou | O que disse (frases literais) |
|---|---|---|---|---|
| T1 | | | | |
| T2 | | | | |
| T3 | | | | |
| T4 | | | | |
| T5 | | | | |
| T6 | | | | |
| T7 | | | | |

### Perguntas finais (de 1 = discordo totalmente a 5 = concordo totalmente)

| Pergunta | Nota |
|---|---|
| Foi fácil entender o que fazer em cada etapa da tela. | |
| As mensagens de erro explicaram o que corrigir. | |
| Os resultados (km, minutos, pontos do percurso) foram fáceis de entender. | |
| O mapa ajudou a entender a rota. | |
| Eu usaria a tela de novo sem precisar de ajuda. | |

- O que mais atrapalhou?
- O que você mudaria primeiro?

## 5. Dificuldades encontradas e melhorias

Preencham logo depois do teste e atualizem a coluna "Situação" conforme corrigirem. Gravidade: **alta** (impediu a tarefa), **média** (atrasou ou exigiu tentativa e erro), **baixa** (incômodo).

| # | Dificuldade observada | Tarefa | Quantos participantes | Gravidade | Melhoria proposta | Responsável | Situação |
|---|---|---|---|---|---|---|---|
| D1 | | | | | | | a fazer |
| D2 | | | | | | | a fazer |
| D3 | | | | | | | a fazer |

## 6. O que mostrar na apresentação de 16/11

1. Quem testou, quando, e o resumo das fichas: tarefas concluídas por participante e média das notas.
2. As 2 ou 3 dificuldades mais graves da seção 5.
3. **Antes e depois** de cada melhoria: um print ou uma demonstração ao vivo da tela corrigida.
4. O que ficou sem correção e por quê.
