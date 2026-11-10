# Logística: rotas e incidentes simulados

Trabalho da disciplina Projeto de Programação.

Microsserviço Java que calcula o menor caminho entre dois pontos de uma rede viária simulada e recalcula a rota quando um trecho é bloqueado por um incidente. Um cliente desktop em Swing usa a API REST e desenha a rota em um mapa esquemático.

A rede é simulada e não representa trânsito nem a malha viária real.

## Sumário

1. [Equipe](#equipe)
2. [Escopo](#escopo)
3. [Tecnologias](#tecnologias)
4. [Estrutura do repositório](#estrutura-do-repositório)
5. [Modelo de domínio](#modelo-de-domínio)
6. [Dados](#dados)
7. [Contrato da API REST](#contrato-da-api-rest)
8. [Regras e cenários](#regras-e-cenários)
9. [Arquivos texto e binário](#arquivos-texto-e-binário)
10. [Testes](#testes)
11. [Matriz de conteúdos do PEA](#matriz-de-conteúdos-do-pea)
12. [Como executar](#como-executar)
13. [Convenções](#convenções)
14. [Referências](#referências)

---

## Equipe

Alan Oliveira Silva Júnior, Brunno Passos da Vitória, Gabriel Oliveira Cardoso, Isadora Matos Bispo, Jarlisson Brito, Luiz Gustavo de Oliveira

## Escopo

**Objetivo:** calcular caminhos em uma rede local de pontos e vias e recalcular quando um trecho fica bloqueado.

**Fluxo básico:** carregar a rede, escolher origem e destino, calcular, bloquear um trecho e recalcular.

O sistema:

- importa pontos e vias a partir de arquivos CSV;
- calcula o menor caminho com Dijkstra, por distância (km) ou por tempo (min);
- registra incidentes que bloqueiam vias e recalcula sem usar os trechos bloqueados;
- avisa, com uma mensagem clara, quando não existe caminho até o destino;
- desenha a rede e a rota em um mapa esquemático (Swing);
- exporta a rota em CSV e guarda configurações em um arquivo binário.

Ficam de fora a roteirização de várias entregas, APIs de mapas ou de trânsito reais, e Redis, WebSocket, Kafka ou nuvem. O projeto não depende de contas pagas nem de serviços externos.

---

## Tecnologias

| Item                | Escolha                                                                |
| ------------------- | ---------------------------------------------------------------------- |
| Java                | 17                                                                     |
| Build               | Maven com Maven Wrapper (`mvnw`), projeto multimódulo                  |
| Framework           | Spring Boot 3.5.x                                                      |
| Dependências da API | Spring Web, Validation, Data JPA                                       |
| Banco               | H2 nos perfis `dev` e `test`; PostgreSQL no perfil de entrega (`prod`) |
| GUI                 | Swing e `java.net.http.HttpClient`                                     |
| Testes              | JUnit 5 (com `spring-boot-starter-test`)                               |
| Containers          | `compose.yaml` para subir o PostgreSQL                                 |

Restrições do projeto:

- Sem Lombok nas classes avaliadas: construtores, getters e encapsulamento são escritos à mão.
- A GUI não acessa o banco. Ela só conversa com a API.
- As chamadas HTTP da GUI rodam em `SwingWorker`, e os componentes só são atualizados na EDT.

---

## Estrutura do repositório

```text
roteirizacao-logistica/
├── pom.xml                          # pom pai (módulos api e desktop)
├── mvnw, mvnw.cmd, .mvn/            # Maven Wrapper
├── compose.yaml                     # PostgreSQL para o perfil prod (a criar)
├── .env.example                     # variáveis de ambiente, sem senhas reais (a criar)
├── README.md
├── api/
│   ├── pom.xml
│   └── src/
│       ├── main/java/com/logistica/roteirizacao/
│       │   ├── controller/          # endpoints REST (finos, sem regra de negócio)
│       │   ├── service/             # regras, Dijkstra, transações
│       │   ├── model/               # entidades e tipos do domínio (Grafo, Ponto, Via...)
│       │   ├── repository/          # Spring Data JPA (somente persistência)
│       │   ├── dto/                 # contrato externo (request/response)
│       │   ├── exception/           # exceções de domínio e @ControllerAdvice
│       │   └── files/               # leitura e escrita de CSV e binário
│       ├── main/resources/          # application.yml e perfis dev, test e prod
│       └── test/java/com/logistica/roteirizacao/
├── desktop/
│   ├── pom.xml
│   └── src/main/java/com/logistica/roteirizacao/
│       ├── client/                  # chamadas à API com HttpClient (e dto/)
│       ├── files/                   # config.bin (binário) e exportação da rota em CSV
│       └── ui/                      # telas Swing e mapa esquemático
├── docs/
│   ├── openapi.yaml                 # contrato REST com exemplos
│   ├── dicionario-dados.md          # significado, tipo e unidade de cada campo
│   ├── gui-telas-e-fluxo.md         # telas e fluxo do cliente desktop
│   └── registros-semanais.md
└── dados/
    ├── brutos/                      # dados como foram obtidos ou gerados
    ├── tratados/                    # pontos.csv e vias.csv prontos para importar
    └── scripts/                     # gerar_rede.py
```

---

## Modelo de domínio

| Tipo                            | Papel                                                                              | Conteúdo do PEA                                                     |
| ------------------------------- | ---------------------------------------------------------------------------------- | ------------------------------------------------------------------- |
| `Ponto`                         | Vértice da rede (`id`, `nome`, `latitude`, `longitude`)                            | classes, encapsulamento, construtores, `equals`/`hashCode` por `id` |
| `Via`                           | Aresta de mão dupla (`origem`, `destino`, `distanciaKm`, `tempoMin`, `habilitada`) | validação no construtor (pesos > 0)                                 |
| `Grafo`                         | TAD que encapsula nós e arestas: `Map<String, Ponto>` e mapa de adjacências        | TAD, coleções, encapsulamento                                       |
| `CriterioRota`                  | Contrato que define o peso de uma via                                              | interfaces, polimorfismo                                            |
| `CriterioRotaBase`              | Classe abstrata com a validação comum dos critérios                                | herança                                                             |
| `MenorDistancia` / `MenorTempo` | Estratégias concretas, subclasses de `CriterioRotaBase`                            | polimorfismo, override                                              |
| `CalculadorRota`                | Dijkstra sobre o `Grafo` com um `CriterioRota`                                     | métodos, passagem de parâmetros, overload                           |
| `Rota`                          | Resultado: sequência de pontos, total, critério, status                            | `toString`                                                          |
| `Incidente`                     | Bloqueio de uma via (`viaId`, `descricao`, `instante`, `ativo`)                    |                                                                     |
| `CaminhoInexistenteException`   | Destino sem caminho disponível                                                     | exceções                                                            |

---

## Dados

Os arquivos prontos ficam em `dados/tratados/`, em UTF-8, separados por vírgula e com cabeçalho.

`pontos.csv`

```csv
id,nome,latitude,longitude
P01,Universidade Tiradentes (UNIT) - Farolândia,-10.9684319,-37.0612574
```

`vias.csv`

```csv
id,origem,destino,distanciaKm,tempoMin,habilitada
V01,P12,P17,1.9,5.7,true
```

A rede tem 20 pontos reais de Aracaju, com coordenadas do OpenStreetMap (fonte e data em [dados/brutos/FONTES.md](dados/brutos/FONTES.md)), e 30 vias simuladas, definidas pela equipe em [dados/brutos/ligacoes.csv](dados/brutos/ligacoes.csv). As vias não seguem o traçado real das ruas.

As coordenadas servem só para o desenho do mapa. Quem define quais pontos se ligam são as vias. Os pesos são assumidos: a distância é a linha reta multiplicada por 1,3, e o tempo sai da velocidade média do tipo de via (35 km/h nas arteriais, 20 km/h nas locais). O script [dados/scripts/gerar_rede.py](dados/scripts/gerar_rede.py) gera os CSVs, e o significado de cada campo está em [docs/dicionario-dados.md](docs/dicionario-dados.md).

Toda via é de mão dupla e tem pesos maiores que zero, porque o Dijkstra não aceita pesos negativos. Um incidente bloqueia os dois sentidos.

### Rede de validação

Para conferir o Dijkstra à mão, os testes usam uma rede pequena de 5 nós, com resultado conhecido:

| Via | Origem e destino | km  | min |
| --- | ---------------- | --- | --- |
| V1  | A - B            | 4   | 14  |
| V2  | A - C            | 2   | 15  |
| V3  | B - D            | 5   | 5   |
| V4  | C - D            | 8   | 6   |
| V5  | C - E            | 11  | 4   |
| V6  | D - E            | 2   | 3   |

| Cenário (A até E)                | Rota esperada | Total         |
| -------------------------------- | ------------- | ------------- |
| Menor distância                  | A, B, D, E    | 11 km         |
| Menor tempo                      | A, C, E       | 19 min        |
| Menor distância com V3 bloqueada | A, C, D, E    | 12 km         |
| V5 e V6 bloqueadas               | sem caminho   | `SEM_CAMINHO` |

---

## Contrato da API REST

Base: `http://localhost:8080`. O contrato completo, com exemplos, está em [docs/openapi.yaml](docs/openapi.yaml).

| Método | Rota                 | Descrição                                     | Sucesso |
| ------ | -------------------- | --------------------------------------------- | ------- |
| `POST` | `/redes/importacoes` | Importa `pontos.csv` e `vias.csv` (multipart) | `201`   |
| `POST` | `/rotas`             | Calcula uma rota entre origem e destino       | `201`   |
| `GET`  | `/rotas/{id}`        | Consulta uma rota calculada                   | `200`   |
| `POST` | `/incidentes`        | Bloqueia uma via                              | `201`   |

### `POST /rotas`

Exemplo, da Rodoviária (P05) até a UNIT (P01):

```json
{ "origemId": "P05", "destinoId": "P01", "criterio": "MENOR_DISTANCIA" }
```

Resposta `201`:

```json
{
  "id": 1,
  "origemId": "P05",
  "destinoId": "P01",
  "criterio": "MENOR_DISTANCIA",
  "status": "CALCULADA",
  "pontos": ["P05", "P15", "P01"],
  "vias": ["V14", "V24"],
  "distanciaTotalKm": 8.5,
  "tempoTotalMin": 14.5,
  "versaoRede": 1,
  "mensagem": null,
  "calculadaEm": "2026-10-26T10:15:00-03:00"
}
```

Quando não há caminho, a API também responde `201`, mas com `status` igual a `SEM_CAMINHO`, listas vazias, totais `null` e uma mensagem como "Não há caminho disponível de P05 até P07. Vias bloqueadas: V29, V30." A tentativa fica registrada. Quando a origem é igual ao destino, a resposta tem um único ponto e totais zero.

### `POST /redes/importacoes`

Substitui a rede atual e incrementa `versaoRede`. Linhas inválidas (peso menor ou igual a zero, ponto inexistente, id duplicado) são rejeitadas e listadas em `linhasRejeitadas`, e as demais são importadas.

### `POST /incidentes`

```json
{ "viaId": "V24", "descricao": "Alagamento simulado na Av. Tancredo Neves" }
```

O incidente desabilita a via e incrementa a `versaoRede`. Depois dele, a mesma rota Rodoviária até UNIT passa a ser P05, P15, P03, P01, com 11,7 km. Para recalcular, o cliente chama `POST /rotas` de novo com a mesma origem e destino.

### Erros

| Código | Quando                                                                                    |
| ------ | ----------------------------------------------------------------------------------------- |
| `400`  | Campo ausente, critério inválido, JSON mal formado, arquivo ausente ou cabeçalho inválido |
| `404`  | Ponto, via ou rota inexistente                                                            |
| `405`  | Método HTTP não permitido                                                                 |
| `409`  | Conflito, por exemplo via já bloqueada                                                    |
| `415`  | Tipo de conteúdo não suportado                                                            |
| `500`  | Erro inesperado no servidor                                                               |
| `503`  | Banco de dados indisponível                                                               |

Todos os erros seguem o mesmo formato, gerado pelo `@ControllerAdvice`:

```json
{
  "status": 400,
  "erro": "VALIDACAO",
  "campo": "origemId",
  "mensagem": "Informe o ponto de origem."
}
```

---

## Regras e cenários

Duas regras do domínio:

1. O menor caminho usa o peso definido pelo `CriterioRota` escolhido (distância ou tempo).
2. Vias com `habilitada = false` ou com incidente ativo nunca entram no cálculo.

Três cenários que o sistema precisa tratar:

| Cenário | Exemplo                                                            |
| ------- | ------------------------------------------------------------------ |
| Normal  | A até E por distância retorna A, B, D, E, com 11 km                |
| Limite  | Origem igual ao destino; empate entre caminhos; via de peso mínimo |
| Falha   | Destino sem caminho; CSV com peso negativo; ponto inexistente      |

Em rede pequena o resultado confere com o cálculo manual, bloquear uma via da rota muda a rota recalculada, e os dois critérios dão rotas diferentes na rede de validação.

---

## Arquivos texto e binário

| Tipo          | Uso                                                                                            | Classe                          | Situação |
| ------------- | ---------------------------------------------------------------------------------------------- | ------------------------------- | -------- |
| CSV (entrada) | Importar `pontos.csv` e `vias.csv`, validar o cabeçalho e listar as linhas rejeitadas          | `ImportadorRedeCsv`             | feito    |
| CSV (saída)   | Exportar a rota calculada (sequência de pontos e totais), botão **Exportar CSV…** no Resultado | `ExportadorRotaCsv` (desktop)   | feito    |
| Binário       | `config.bin` com as configurações da sessão, via `DataOutputStream` e `DataInputStream`        | `ConfiguracaoBinaria` (desktop) | feito    |

O CSV exportado tem uma linha por ponto do percurso, na ordem. Os dados da rota se repetem em cada linha:

```csv
rotaId,criterio,ordem,pontoId,pontoNome,viaChegadaId,distanciaTotalKm,tempoTotalMin,versaoRede,calculadaEm
1,MENOR_DISTANCIA,1,P05,Rodoviária de Aracaju,,8.5,14.5,1,2026-11-10T10:15-03:00
1,MENOR_DISTANCIA,2,P15,Distrito Industrial de Aracaju (DIA),V14,8.5,14.5,1,2026-11-10T10:15-03:00
1,MENOR_DISTANCIA,3,P01,Universidade Tiradentes (UNIT) - Farolândia,V24,8.5,14.5,1,2026-11-10T10:15-03:00
```

`viaChegadaId` é a via usada para chegar ao ponto (vazia no primeiro). Campos com vírgula ou aspas vão entre aspas.

O `config.bin` guarda só parâmetros, nunca os registros do domínio:

| Ordem | Tipo  | Campo                                          |
| ----- | ----- | ---------------------------------------------- |
| 1     | `int` | versão do formato (`1`)                        |
| 2     | `int` | critério padrão (`0` = distância, `1` = tempo) |
| 3     | `UTF` | último ponto de origem                         |
| 4     | `UTF` | último ponto de destino                        |
| 5     | `UTF` | URL base da API                                |

Arquivo ausente, truncado, com versão não suportada ou com valores inválidos gera um erro tratado.

O `config.bin` fica na pasta em que o desktop é iniciado (a raiz do repositório) e está no `.gitignore`. Ele é lido ao abrir a janela e gravado a cada rota calculada e ao fechar. Para demonstrar:

1. Calcule uma rota por menor tempo e feche a janela.
2. Abra de novo: a barra de status mostra as preferências recuperadas.
3. Importe a rede: o critério, a origem e o destino voltam preenchidos.

Se o arquivo estiver corrompido, a tela avisa, usa os valores padrão e regrava o arquivo.

---

## Testes

```bash
./mvnw test
```

| #   | Tipo             | O que verifica                                                       | Situação |
| --- | ---------------- | -------------------------------------------------------------------- | -------- |
| 1   | Regra            | Menor distância na rede de 5 nós: A, B, D, E, 11 km                  | feito    |
| 2   | Regra            | Menor tempo na rede de 5 nós: A, C, E, 19 min                        | feito    |
| 3   | Regra            | Com V3 bloqueada, a rota muda para A, C, D, E                        | feito    |
| 4   | Entrada inválida | Via com peso menor ou igual a zero é rejeitada                       | feito    |
| 5   | Entrada inválida | Ponto de origem inexistente gera `404`                               | feito    |
| 6   | Arquivo          | Exportar e importar CSV mantém os dados                              | feito    |
| 7   | Arquivo          | Gravar e ler `config.bin`; arquivo truncado gera erro tratado        | feito    |
| 8   | Integração       | CSV inexistente e falha de conexão com o banco são tratados          | feito    |
| 9   | REST             | `POST /rotas` retorna `201` e `GET /rotas/{id}` devolve a mesma rota | feito    |
| 10  | Regra            | Sem caminho gera `SEM_CAMINHO` com mensagem                          | feito    |

---

## Matriz de conteúdos do PEA

Onde cada conteúdo aparece no nosso código. Atualizar conforme o projeto avança.

| Conteúdo                | Onde aparece                                                                                                                                           | Teste                                                                       |
| ----------------------- | ------------------------------------------------------------------------------------------------------------------------------------------------------ | --------------------------------------------------------------------------- |
| POO e introdução a Java | `CalculadorRota` calcula sobre um `Grafo` usando um `CriterioRota`; os serviços usam repositórios e o domínio                                          | `CalculadorRotaTest`                                                        |
| Classes e objetos       | `Ponto`, `Via`, `Grafo`, `Rota`, `Incidente`                                                                                                           | `CalculadorRotaTest`                                                        |
| Encapsulamento          | Campos privados em `Ponto` e `Via`; `Grafo` esconde seus mapas e expõe operações                                                                       | `viaComPesoNaoPositivoEhRecusada`                                           |
| Construtores            | `Via` valida os pesos ao ser criada; `PontoRede` (desktop) exige id e nome                                                                             | `viaComPesoNaoPositivoEhRecusada`                                           |
| Pacotes e acesso        | API em `controller`, `service`, `model`, `repository`, `dto`, `exception`, `files`; desktop em `client` e `ui`                                         | -                                                                           |
| Métodos estáticos       | `Rota.arredondar`, `RotaCalculada.calculada` e `semCaminho`, `RotaResponse.de`; no desktop, `Mensagens` e `Desfecho.de`                                | `RotaControllerTest`                                                        |
| Passagem de parâmetros  | `CalculadorRota.calcular(grafo, origemId, destinoId, criterio)` recebe tudo de forma explícita                                                         | `CalculadorRotaTest`                                                        |
| Herança                 | `CriterioRotaBase` com `MenorDistancia` e `MenorTempo`; `ApiException` com `ValidacaoException`, `ConflitoException` e `RecursoNaoEncontradoException` | `menorDistanciaDeAateE`, `menorTempoDeAateE`                                |
| Programação por eventos | Botões Importar, Calcular, Bloquear via e Exportar CSV, e o fechamento da janela, em `JanelaPrincipal.ligarEventos`                                    | `JanelaPrincipalTest`                                                       |
| Interfaces e TADs       | Interface `CriterioRota`; `Grafo` como TAD (`adicionarPonto`, `adicionarVia`, `bloquearVia`); `ClienteApi` no desktop                                  | `CalculadorRotaTest`                                                        |
| Override                | `MenorDistancia` e `MenorTempo` sobrescrevem `pesoDe` e `descricao`; `toString` em várias classes                                                      | `menorDistanciaDeAateE`                                                     |
| Polimorfismo            | `CalculadorRota` recebe um `CriterioRota` e funciona com qualquer estratégia                                                                           | `menorDistanciaDeAateE`, `menorTempoDeAateE`                                |
| Overload                | `CalculadorRota.calcular` com e sem critério (o padrão é menor distância)                                                                              | `criterioPadraoEhMenorDistancia`                                            |
| Classe Object           | `equals` e `hashCode` em `Ponto` e `Via`; `toString` em `Ponto`, `Via`, `Rota`, `Grafo` e `Incidente`                                                  | -                                                                           |
| Exceções                | `TratadorErrosApi` (`@ControllerAdvice`) e `CaminhoInexistenteException`; no desktop, `ClienteApiException` e `Desfecho`                               | `rotaInexistenteGera404`, `falhaDeConexaoComOBancoGera503`                  |
| Classe File             | `File.isFile()` em `PainelImportacao.validar`, `LeitorPontosCsv` e `LeitorViasCsv`                                                                     | `ImportadorRedeCsvTest`                                                     |
| Arquivos texto          | `ImportadorRedeCsv` (API); `LeitorPontosCsv`, `LeitorViasCsv` e `ExportadorRotaCsv` (desktop)                                                          | `rejeitaLinhasInvalidasEImportaAsDemais`, `exportarELerMantemOsDadosDaRota` |
| Arquivos binários       | `ConfiguracaoBinaria.salvar` e `ler` (`config.bin`, desktop)                                                                                           | `ConfiguracaoBinariaTest`                                                   |
| Coleções                | `Grafo` usa `Map`, `List` e `Set`; o desktop guarda os pontos em `Map<String, PontoRede>`                                                              | `CalculadorRotaTest`                                                        |
| GUI                     | Pacote `desktop/.../ui`: `JanelaPrincipal`, painéis de importação, seleção, incidente e resultado, e `PainelMapa`                                      | `JanelaPrincipalTest` e demonstração manual                                 |

---

## Como executar

Pré-requisito: JDK 17 ou superior. O Docker só é necessário no perfil `prod`.

```bash
# API (perfil dev: H2 em memória, já sobe com a rede de Aracaju carregada)
./mvnw -pl api spring-boot:run

# Cliente desktop (em outro terminal, na raiz do repositório)
./mvnw -pl desktop compile exec:java

# Testes
./mvnw test

# API com PostgreSQL (perfil prod)
cp .env.example .env
docker compose up -d
./mvnw -pl api spring-boot:run -Dspring-boot.run.profiles=prod
```

No Windows, use `mvnw.cmd` no lugar de `./mvnw`.

Se a porta 8080 estiver ocupada, suba a API em outra porta e aponte o cliente para ela (no PowerShell, mantenha as aspas):

```bash
./mvnw -pl api spring-boot:run "-Dspring-boot.run.arguments=--server.port=8081"
./mvnw -pl desktop compile exec:java "-Dexec.args=http://localhost:8081"
```

Variáveis de ambiente (`.env.example`, sem senhas reais):

```dotenv
POSTGRES_DB=roteirizacao
POSTGRES_USER=roteirizacao
POSTGRES_PASSWORD=troque-me
SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/roteirizacao
```

O `.env` real não vai para o repositório. O `compose.yaml` e o `.env.example` ainda não foram criados, então o perfil `prod` fica para a próxima etapa.

### Cliente desktop

Rode sempre da raiz do repositório, para a tela achar `dados/tratados/`. A janela tem três etapas: importar a rede (os dois CSVs já vêm preenchidos), escolher origem, destino e critério, e calcular. O resultado mostra os pontos do percurso, as vias e os totais de distância e tempo, e a rota é destacada no mapa à direita.

Para conferir: da Rodoviária (P05) até a UNIT (P01), por menor distância, o resultado é 8,5 km e 14,5 min, passando por P15. Os erros aparecem sob o campo e na barra de status. Detalhes em [docs/gui-telas-e-fluxo.md](docs/gui-telas-e-fluxo.md).

---

## Convenções

- O controller é fino: valida a entrada e delega ao service. As regras e as transações ficam no service, e o repository só cuida da persistência. DTOs separam o contrato externo das entidades JPA.
- Nomes em português e métodos com uma tarefa só. Comentários explicam decisões e fórmulas, não o óbvio.
- Na GUI, os rótulos trazem a unidade (km, min), as mensagens dizem qual campo está errado e como corrigir, e a rota não é indicada só por cor.
- Git: uma branch `feature/<assunto>` para cada tarefa, Pull Request e revisão de outro integrante, commits pequenos e descritivos.

---

## Referências

- OpenStreetMap, via Nominatim: fonte das coordenadas dos pontos.
- Requisitos do Spring Boot 3.5: <https://docs.spring.io/spring-boot/3.5/system-requirements.html>
