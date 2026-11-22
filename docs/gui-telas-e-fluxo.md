# GUI desktop: telas e fluxo (Isadora)

Código em `desktop/src/main/java/com/logistica/roteirizacao/ui`. Usa o cliente do Gabriel (`client/`).

## Tela única, quatro seções

| Seção | Campos e botões | Atalho |
|---|---|---|
| **1. Importar rede** | `pontos.csv` e `vias.csv` (caminho + **Escolher…**), **Importar rede**, status da rede | Alt+P, Alt+V, Alt+I |
| **2. Origem, destino e critério** | combos **Origem** e **Destino**, **Inverter**, **Menor distância (km)** / **Menor tempo (min)**, **Calcular rota** | Alt+O, Alt+D, Alt+N, Alt+S, Alt+T, Alt+C |
| **4. Bloquear ou desbloquear via (incidente)** | combo **Via** (com os nomes dos pontos), **Descrição** (padrão "Alagamento simulado"), **Bloquear via**, lista das vias bloqueadas, combo **Bloqueada** e **Desbloquear via** (ativos só quando há via bloqueada) | Alt+A, Alt+R, Alt+B, Alt+L, Alt+U |
| **3. Resultado** | status em texto, totais em km e min, critério, versão da rede, data, pontos do percurso, vias, mensagem, **Exportar CSV…** | Alt+E |
| **Mapa interativo** (centro, em destaque) | fundo urbano só decorativo (quarteirões, ruas brancas, avenidas e pracinhas alinhados às vias: cada região segue a direção da via mais próxima; não são vias da rede nem entram no cálculo), pontos em latitude/longitude, vias da rede como estradas amarelas (cinza com tracejado se bloqueadas), rota com traço grosso e setas de sentido, números de ordem, marcador azul **Você está aqui** na origem, alfinete vermelho **Destino**, caixa "Você está em / Para onde vai" e legenda; botões **+**, **−** e **Ajustar** | roda do mouse, +, −, 0, setas |
| Barra de status | OK / Aviso / Erro + progresso enquanto a API responde | — |

Distribuição na janela (mapa em destaque): as seções 1, 2 e 4 ficam, nessa
ordem, numa coluna à esquerda; o mapa ocupa o centro e a seção 3 é uma faixa logo
abaixo dele (resumo e **Exportar CSV…** à esquerda, percurso à direita). A divisão entre mapa
e resultado pode ser arrastada; ao aumentar a janela, quem cresce é o mapa.

Ao abrir, a tela já vem com `dados/tratados/pontos.csv` e `vias.csv` (cenário de exemplo), se achar a pasta.
Rode a partir da raiz do repositório.

## Fluxo

1. **Importar:** valida os arquivos → `ClienteApi.importarRede` em segundo plano → lê o `pontos.csv` para preencher os combos → libera a seção 2. Linhas rejeitadas abrem uma janela com arquivo, linha e motivo.
2. **Escolher** origem, destino e critério (validação local antes de chamar a API), pelos combos ou pelo mapa:
   - 1º clique num ponto: vira a origem ("Você está aqui") e o destino é limpo;
   - 2º clique em outro ponto: vira o destino e a rota é calculada na hora;
   - um novo clique recomeça pela origem. Clicar de novo na origem só avisa.
   - Roda do mouse dá zoom no ponto sob o cursor, arrastar move o mapa, duplo clique fora dos pontos (ou **Ajustar** / tecla 0) volta à rede inteira. Ao passar o mouse, o ponto ganha destaque e o tooltip diz o que o clique fará.
3. **Calcular:** `ClienteApi.calcularRota` em segundo plano → mostra `CALCULADA` ou `SEM_CAMINHO` e destaca a rota no mapa. Grava critério, origem e destino no `config.bin`.
4. **Bloquear via:** valida a via e a descrição (3 a 200 caracteres, a mesma regra da API) → `ClienteApi.registrarIncidente` em segundo plano → a via fica tracejada no mapa e sai da lista (bloquear de novo daria `409`) → se já havia uma rota calculada, ela é **recalculada automaticamente** com a mesma origem, destino e critério. A barra mostra, por exemplo, "Via V24 bloqueada (rede versão 2). Rota recalculada: 11,7 km e 23,2 min."
5. **Desbloquear via:** escolhe a via no combo **Bloqueada** → `ClienteApi.encerrarIncidente` (com o id do incidente devolvido no bloqueio) em segundo plano → a via volta ao traço normal no mapa, sai do combo **Bloqueada** e volta ao combo **Via** → a última rota é **recalculada** já com a via. A barra mostra, por exemplo, "Via V24 desbloqueada (rede versão 3). Rota recalculada: 8,5 km e 14,5 min." Se a API responder `409` (já encerrado) ou `404` (incidente apagado por uma reimportação), a tela libera a via mesmo assim e avisa.
6. **Exportar CSV…:** só fica ativo com uma rota com caminho na tela. Abre "Salvar como" com o nome sugerido `rota-<id>-<origem>-<destino>.csv` e pede confirmação antes de substituir um arquivo. O formato está no README, em "Arquivos texto e binário".
7. Reimportar limpa o resultado, recarrega os combos e esvazia a lista de vias bloqueadas (a API apaga os incidentes ao importar).

## Preferências no `config.bin`

- Lido ao abrir. A barra de status informa se é a primeira execução, se as preferências foram recuperadas ou se o arquivo estava inválido (nesse caso usa os valores padrão e regrava).
- Gravado a cada rota calculada e ao fechar a janela.
- Depois de importar a rede, a tela volta a selecionar o critério, a origem e o destino guardados.
- A URL da API passada como argumento (`-Dexec.args=...`) tem prioridade e passa a ser guardada.

## Mensagens de erro (sob o campo e na barra, sempre com prefixo "Erro:")

| Situação | Onde | Mensagem |
|---|---|---|
| Arquivo não escolhido | sob o campo | "Escolha o arquivo pontos.csv." / "…vias.csv." |
| Calcular sem importar | barra | "Importe a rede (pontos.csv e vias.csv) antes de calcular." |
| Origem/destino vazio | sob o combo | "Selecione um ponto de origem." / "…de destino." |
| Destino = origem | sob o destino | "Selecione um ponto de destino diferente da origem." |
| Bloquear sem escolher via | sob o combo Via | "Selecione a via a bloquear." |
| Desbloquear sem escolher via | sob o combo Bloqueada | "Selecione a via a desbloquear." |
| Descrição curta ou longa demais | sob a descrição | "Descreva o incidente com 3 a 200 caracteres." |
| Falha ao gravar o CSV | barra | "Não foi possível gravar … Verifique se a pasta existe e se o arquivo não está aberto em outro programa." |
| API recusou (400/404/409/503) | sob o campo indicado em `campo`; senão, barra | texto da API |
| API fora do ar / timeout | barra | texto do `ClienteApiHttp` |
| Sem caminho | seção Resultado (laranja) | mensagem da API |

## Como a GUI usa o cliente do Gabriel

- `ClienteApi`, `ClienteApiHttp`, `ClienteApiException` e os DTOs de `client/dto` são dele; a GUI não os altera.
- `TarefaEmSegundoPlano` só entrega o **texto** do erro. Para destacar o campo (origemId, destinoId, pontos, vias) a GUI usa `ui/Desfecho`, que captura a `ClienteApiException` dentro do segundo plano e a devolve como valor.

## Decisões para combinar com o grupo

1. A API não tem `GET /pontos`: a GUI lê o próprio `pontos.csv` escolhido para preencher os combos.
2. Origem = destino é bloqueado na GUI (convenção do README). O cenário "limite" fica para teste automatizado ou `curl`.

## Rodar

```bash
./mvnw -pl api spring-boot:run      # terminal 1
./mvnw -pl desktop compile exec:java        # terminal 2, na raiz do repositório
```

## Evidências para a matriz do PEA

Eventos: `JanelaPrincipal.ligarEventos` (Importar, Calcular, Bloquear via, Exportar CSV e fechamento da janela). Arquivos binários: `files/ConfiguracaoBinaria`. Arquivos texto: `files/ExportadorRotaCsv`. GUI: pacote `ui`. Exceções: `ClienteApiException`, `Desfecho`, `Mensagens.doErro`. Classe File: `PainelImportacao.validar`, `LeitorPontosCsv`. Estáticos: `Mensagens`, `LeitorPontosCsv`, `LeitorViasCsv`, `Desfecho.de`. Desenho 2D: `PainelMapa.paintComponent` (herança de `JPanel` e override). Coleções: `Map<String,PontoRede>`, `List`. Object: `PontoRede.equals/hashCode/toString`. Encapsulamento: `PontoRede` imutável.
