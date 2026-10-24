# Logística: rotas e incidentes simulados

Trabalho da disciplina **Projeto de Programação**.

Microsserviço Java que **calcula o menor caminho entre dois pontos de uma rede viária simulada** e **recalcula a rota quando um trecho é bloqueado por um incidente**. Um cliente desktop em Swing consome a API REST e desenha a rota em um mapa esquemático.

> MVP acadêmico: a rede é **simulada** e não representa trânsito ou malha viária real. Não é sistema pronto para produção.

---

## Sumário

1. [Equipe](#equipe)
2. [Escopo do MVP](#escopo-do-mvp)
3. [Tecnologias](#tecnologias)
4. [Estrutura do repositório](#estrutura-do-repositório)
5. [Modelo de domínio](#modelo-de-domínio)
6. [Dados](#dados)
7. [Contrato da API REST](#contrato-da-api-rest)
8. [Regras e critérios de aceite](#regras-e-critérios-de-aceite)
9. [Arquivos texto e binário](#arquivos-texto-e-binário)
10. [Testes](#testes)
11. [Matriz de conteúdos do PEA](#matriz-de-conteúdos-do-pea)
12. [Como executar](#como-executar)
13. [Convenções da equipe](#convenções-da-equipe)
14. [Cronograma e apresentações](#cronograma-e-apresentações)
15. [Entrega final](#entrega-final)
16. [Critérios de avaliação](#critérios-de-avaliação)
17. [Referências](#referências)

---

## Equipe

| Integrante | Responsabilidade na semana atual       | GitHub     |
| ---------- | -------------------------------------- | ---------- |
| _Nome_     | _domínio / API / GUI / testes / dados_ | _@usuario_ |
| _Nome_     |                                        |            |
| _Nome_     |                                        |            |
| _Nome_     |                                        |            |

Cada integrante deve passar por **domínio, interface e testes** ao longo do projeto (rodízio de responsabilidades). Registrar a contribuição de cada um em [docs/registros-semanais.md](docs/registros-semanais.md).

---

## Escopo do MVP

**Objetivo:** calcular caminhos em uma rede local e recalcular quando um trecho fica bloqueado.

**Fluxo básico:** carregar rede → escolher origem e destino → calcular → bloquear trecho → recalcular.

### Dentro do escopo (obrigatório)

- Importação de **pontos** e **vias** via CSV.
- Cálculo do menor caminho entre **dois pontos** com **Dijkstra**.
- Dois critérios: **menor distância (km)** e **menor tempo (min)**.
- Registro de **incidentes** que bloqueiam vias.
- Recálculo **sem usar trechos bloqueados**.
- Tratamento de **destino sem caminho disponível**, com mensagem clara.
- **Desenho esquemático** da rede e da rota no Swing.
- Exportação/importação **CSV** e **arquivo binário** de configurações.

### Fora do escopo (só depois do MVP aceito)

- Roteirização de múltiplas entregas / ordem de paradas (otimização global).
- API de mapas real, trânsito real, APIs pagas ou que exigem credenciais.
- Redis (cache), WebSocket, Kafka, nuvem.

> A entrega **não pode depender** de credenciais, contas pagas ou disponibilidade de APIs externas.

---

## Tecnologias

| Item                | escolha                                                                       |
| ------------------- | ----------------------------------------------------------------------------- |
| Java                | **17**                                                                        |
| Build               | **Maven** com **Maven Wrapper** (`mvnw`), projeto multimódulo                 |
| Framework           | **Spring Boot 3.5.x** (fixar o patch: `3.5.__`)                               |
| Dependências da API | Spring Web, Validation, Data JPA                                              |
| Banco               | **H2** nos perfis `dev`/`test` · **PostgreSQL** no perfil de entrega (`prod`) |
| GUI                 | **Swing** + `java.net.http.HttpClient`                                        |
| Testes              | **JUnit 5** (+ `spring-boot-starter-test`)                                    |
| Containers          | `compose.yaml` para subir o PostgreSQL                                        |

Restrições:

- **Não usar Lombok** nas classes avaliadas: construtores, getters e encapsulamento devem estar explícitos.
- A **GUI não acessa o banco**. Ela é apenas cliente da API.
- Chamadas HTTP da GUI rodam em `SwingWorker`. Os componentes são atualizados apenas na EDT.

---

## Estrutura do repositório

```text
roteirizacao-logistica/
├── pom.xml                          # pom pai (módulos api e desktop)
├── mvnw, mvnw.cmd, .mvn/            # Maven Wrapper
├── compose.yaml                     # PostgreSQL para o perfil prod
├── .env.example                     # variáveis de ambiente (sem senhas reais)
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
│       │   ├── exception/           # exceções de domínio + @ControllerAdvice
│       │   ├── integration/         # ProvedorMapa + adapter local (sem API real)
│       │   └── files/               # leitura/escrita CSV e binário
│       ├── main/resources/
│       │   ├── application.yml
│       │   ├── application-dev.yml
│       │   ├── application-test.yml
│       │   └── application-prod.yml
│       └── test/java/com/logistica/roteirizacao/
├── desktop/
│   ├── pom.xml
│   └── src/main/java/com/logistica/roteirizacao/
│       ├── ui/                      # telas Swing, painel do mapa esquemático
│       └── client/                  # chamadas à API com HttpClient
├── docs/
│   ├── openapi.yaml                 # contrato REST com exemplos
│   ├── rastreabilidade.csv          # matriz do PEA (conteúdo → classe/método → teste)
│   ├── dicionario-dados.md          # significado, tipo e unidade de cada campo
│   ├── diagrama-classes.png
│   └── registros-semanais.md
└── dados/
    ├── brutos/                      # dados como foram obtidos/gerados (não editar)
    └── tratados/                    # pontos.csv e vias.csv prontos para importação
```

---

## Modelo de domínio

| Tipo                            | Papel                                                                                              | Conteúdo do PEA                                                     |
| ------------------------------- | -------------------------------------------------------------------------------------------------- | ------------------------------------------------------------------- |
| `Ponto`                         | Vértice da rede (`id`, `nome`, `latitude`, `longitude`)                                            | classes, encapsulamento, construtores, `equals`/`hashCode` por `id` |
| `Via`                           | Aresta **direcionada** (`origem` → `destino`, `distanciaKm`, `tempoMin`, `habilitada`)             | validação no construtor (pesos > 0)                                 |
| `Grafo`                         | **TAD** que encapsula nós e arestas: `Map<String, Ponto>` + `Map<String, List<Via>>` (adjacências) | TAD, coleções, encapsulamento                                       |
| `CriterioRota`                  | Contrato que define o peso de uma via                                                              | interfaces, polimorfismo                                            |
| `MenorDistancia` / `MenorTempo` | Estratégias concretas de `CriterioRota`                                                            | polimorfismo, override                                              |
| `CalculadorRota`                | Dijkstra sobre o `Grafo` com um `CriterioRota`                                                     | métodos, passagem de parâmetros, overload                           |
| `Rota`                          | Resultado: sequência de pontos, total, critério, status                                            | `toString`                                                          |
| `Incidente`                     | Bloqueio de uma via (`viaId`, `descricao`, `instante`, `ativo`)                                    |                                                                     |
| `ProvedorMapa`                  | Interface em `integration/`. A implementação é `ProvedorMapaLocal` (lê CSV)                        | interfaces, adapter                                                 |
| `CaminhoInexistenteException`   | Destino sem caminho disponível                                                                     | exceções                                                            |

Sugestão para cobrir **herança** de forma coerente: classe abstrata `CriterioRotaBase implements CriterioRota` com `descricao()` abstrato e validação comum. `MenorDistancia` e `MenorTempo` estendem essa classe.

Exemplo de overload no serviço:

```java
public Rota calcular(String origem, String destino) {              // critério padrão
    return calcular(origem, destino, new MenorDistancia());
}
public Rota calcular(String origem, String destino, CriterioRota criterio) { ... }
```

---

## Dados

### Formato dos CSVs (UTF-8, separador `,`, com cabeçalho)

`dados/tratados/pontos.csv`

```csv
id,nome,latitude,longitude
P01,Centro de Distribuição,-19.9167,-43.9345
```

`dados/tratados/vias.csv`

```csv
id,origem,destino,distanciaKm,tempoMin,habilitada
V01,P01,P02,3.4,8,true
```

| Campo                   | Tipo    | Unidade / regra                              |
| ----------------------- | ------- | -------------------------------------------- |
| `latitude`, `longitude` | decimal | graus (WGS84). Usadas **só para desenho**    |
| `distanciaKm`           | decimal | km, **> 0**                                  |
| `tempoMin`              | decimal | minutos, **> 0**                             |
| `habilitada`            | boolean | `false` = via fora de uso desde a importação |

Cada via é **direcionada**. Uma via de mão dupla vira **duas linhas** no CSV.

### Requisitos dos dados

- **20 pontos e 30 vias**, todos com pesos positivos (Dijkstra exige pesos não negativos).
- As **coordenadas** são pesquisadas. Registrar fonte e data de acesso em `dados/brutos/FONTES.md`.
- **Coordenadas não definem conectividade.** As arestas são definidas explicitamente pela equipe.
- **Distância geográfica não é distância rodoviária.** Documentar como `distanciaKm` e `tempoMin` foram obtidos ou assumidos.
- Identificar a rede como **simulada** e documentar a regra de geração (e a semente, se houver).
- Manter `brutos/` separado de `tratados/`. Registrar ausências, duplicidades, valores inválidos e conversões feitas.
- Distinguir no dicionário o que é **medido, publicado, assumido ou simulado**.

### Rede de validação (5 nós, resultado conhecido)

Usada nos testes para conferir o Dijkstra com o resultado calculado à mão:

| Via | Origem → Destino | km  | min |
| --- | ---------------- | --- | --- |
| V1  | A → B            | 4   | 14  |
| V2  | A → C            | 2   | 15  |
| V3  | B → D            | 5   | 5   |
| V4  | C → D            | 8   | 6   |
| V5  | C → E            | 11  | 4   |
| V6  | D → E            | 2   | 3   |

| Cenário (A → E)                      | Rota esperada | Total         |
| ------------------------------------ | ------------- | ------------- |
| Menor distância                      | A → B → D → E | 11 km         |
| Menor tempo                          | A → C → E     | 19 min        |
| Menor distância com **V3 bloqueada** | A → C → D → E | 12 km         |
| **V5 e V6 bloqueadas**               | sem caminho   | `SEM_CAMINHO` |

---

## Contrato da API REST

Base: `http://localhost:8080`. O contrato completo, com exemplos, fica em [docs/openapi.yaml](docs/openapi.yaml).

| Método | Rota                 | Descrição                                     | Sucesso |
| ------ | -------------------- | --------------------------------------------- | ------- |
| `POST` | `/redes/importacoes` | Importa `pontos.csv` e `vias.csv` (multipart) | `201`   |
| `POST` | `/rotas`             | Calcula uma rota entre origem e destino       | `201`   |
| `GET`  | `/rotas/{id}`        | Consulta uma rota calculada                   | `200`   |
| `POST` | `/incidentes`        | Bloqueia uma via                              | `201`   |

### `POST /rotas`

Request:

```json
{ "origemId": "A", "destinoId": "E", "criterio": "MENOR_DISTANCIA" }
```

Response `201`:

```json
{
  "id": 12,
  "origemId": "A",
  "destinoId": "E",
  "criterio": "MENOR_DISTANCIA",
  "status": "CALCULADA",
  "pontos": ["A", "B", "D", "E"],
  "vias": ["V1", "V3", "V6"],
  "distanciaTotalKm": 11.0,
  "tempoTotalMin": 22.0,
  "versaoRede": 3,
  "mensagem": null
}
```

Sem caminho: `status` vem como `"SEM_CAMINHO"`, as listas vêm vazias e `mensagem` traz algo como `"Não há caminho disponível de A até E. Vias bloqueadas: V5, V6."`.

### `POST /incidentes`

```json
{ "viaId": "V3", "descricao": "Alagamento simulado" }
```

O incidente desabilita a via e incrementa a `versaoRede`. Para recalcular, a GUI chama `POST /rotas` de novo com a mesma origem e destino.

### Códigos de erro

| Código | Quando                                                                  |
| ------ | ----------------------------------------------------------------------- |
| `400`  | Validação (campo ausente, critério inválido, CSV mal formado, peso ≤ 0) |
| `404`  | Ponto, via ou rota inexistente                                          |
| `409`  | Conflito (id duplicado na importação, via já bloqueada)                 |
| `503`  | Dependência indisponível (ex.: banco)                                   |

Os erros seguem um formato único, gerado pelo `@ControllerAdvice`:

```json
{
  "status": 400,
  "erro": "VALIDACAO",
  "campo": "origemId",
  "mensagem": "Informe o ponto de origem."
}
```

> **A decidir pela equipe na semana 1:** se "sem caminho" responde `201` com `status: SEM_CAMINHO` (proposta acima, deixa a tentativa registrada) ou um código de erro. Registrar a decisão aqui.

---

## Regras e critérios de aceite

**Duas regras do domínio:**

1. **Menor caminho por critério:** Dijkstra usando o peso definido pelo `CriterioRota` escolhido (distância ou tempo).
2. **Bloqueio de vias:** vias com `habilitada = false` ou com incidente ativo **nunca** entram no cálculo.

**Três cenários obrigatórios:**

| Cenário | Exemplo                                                              |
| ------- | -------------------------------------------------------------------- |
| Normal  | A → E por distância retorna A-B-D-E, 11 km                           |
| Limite  | Origem = destino. Empate entre caminhos. Rede com via de peso mínimo |
| Falha   | Destino sem caminho. CSV com peso negativo. Ponto inexistente        |

**Critérios de aceite:**

- [ ] Em rede pequena, o resultado confere com o cálculo manual.
- [ ] Bloquear uma via da rota muda a rota recalculada.
- [ ] Sem caminho, aparece uma mensagem clara na API e na GUI.
- [ ] Os dois critérios produzem rotas diferentes na rede de validação.

---

## Arquivos texto e binário

| Tipo              | Uso                                                                                                 | Classe sugerida (`files/`) |
| ----------------- | --------------------------------------------------------------------------------------------------- | -------------------------- |
| **CSV (entrada)** | Importar `pontos.csv` e `vias.csv`. Validar cabeçalho e campos e **registrar as linhas rejeitadas** | `ImportadorRedeCsv`        |
| **CSV (saída)**   | Exportar a rota calculada (sequência de pontos e totais)                                            | `ExportadorRotaCsv`        |
| **Binário**       | `config.bin` com as configurações da sessão, via `DataOutputStream`/`DataInputStream`               | `ConfiguracaoBinaria`      |

Proposta de layout do `config.bin`. Ele guarda **apenas parâmetros**, nunca os registros do domínio:

| Ordem | Tipo  | Campo                                          |
| ----- | ----- | ---------------------------------------------- |
| 1     | `int` | versão do formato (`1`)                        |
| 2     | `int` | critério padrão (`0` = distância, `1` = tempo) |
| 3     | `UTF` | último ponto de origem                         |
| 4     | `UTF` | último ponto de destino                        |
| 5     | `UTF` | URL base da API                                |

Tratar **arquivo ausente, truncado, versão não suportada e valores inválidos**. Usar `File` para verificar o caminho antes de ler.

Demonstração exigida: **salvar → fechar → reabrir → recuperar a configuração** e **provocar uma falha controlada**.

---

## Testes

Mínimo sugerido: **8 testes úteis** (testes de getters não contam).

| #   | Tipo             | O que verificar                                                                   |
| --- | ---------------- | --------------------------------------------------------------------------------- |
| 1   | Regra            | Menor distância na rede de 5 nós = A-B-D-E, 11 km                                 |
| 2   | Regra            | Menor tempo na rede de 5 nós = A-C-E, 19 min                                      |
| 3   | Regra            | Com V3 bloqueada, a rota muda para A-C-D-E                                        |
| 4   | Entrada inválida | Via com peso ≤ 0 é rejeitada                                                      |
| 5   | Entrada inválida | Ponto de origem inexistente gera `404`                                            |
| 6   | Arquivo          | Round-trip CSV: exportar e importar mantém os dados                               |
| 7   | Arquivo          | Round-trip binário: gravar e ler `config.bin`. Arquivo truncado gera erro tratado |
| 8   | Integração       | Falha no `ProvedorMapa` / CSV inexistente é tratada                               |
| 9   | REST             | `POST /rotas` → `201` → `GET /rotas/{id}` retorna a mesma rota                    |
| 10  | Regra            | Sem caminho gera `SEM_CAMINHO` com mensagem                                       |

```bash
./mvnw test
```

---

## Matriz de conteúdos do PEA

Todos os conteúdos são **obrigatórios**. Preencher com classe, método e teste à medida que o código for escrito. A versão de entrega fica em [docs/rastreabilidade.csv](docs/rastreabilidade.csv).

| Conteúdo                | Evidência esperada                                 | Classe/método e teste |
| ----------------------- | -------------------------------------------------- | --------------------- |
| POO e introdução a Java | Objetos colaboram na aplicação                     |                       |
| Classes e objetos       | Tipos próprios e instâncias dos cenários           |                       |
| Encapsulamento          | Estado privado e mudanças validadas                |                       |
| Construtores            | Dados essenciais e estado inicial válido           |                       |
| Pacotes e acesso        | Organização e visibilidade justificadas            |                       |
| Métodos estáticos       | Operação associada à classe                        |                       |
| Passagem de parâmetros  | Dados explícitos (Java passa por valor)            |                       |
| Herança                 | Classe-base e subclasses coerentes                 |                       |
| Programação por eventos | Ações da GUI acionam operações                     |                       |
| Interfaces e TADs       | Contrato próprio e tipo com operações encapsuladas |                       |
| Override                | Método herdado sobrescrito                         |                       |
| Polimorfismo            | Duas estratégias pelo mesmo contrato               |                       |
| Overload                | Mesmo nome, parâmetros diferentes                  |                       |
| Classe Object           | `toString`; `equals`/`hashCode` quando aplicável   |                       |
| Exceções                | Falha tratada com recuperação                      |                       |
| Classe File             | Referências e verificações de arquivos             |                       |
| Arquivos texto          | Leitura e escrita de CSV documentado               |                       |
| Arquivos binários       | Leitura e escrita de configurações                 |                       |
| Coleções                | List, Map ou Set com justificativa                 |                       |
| GUI                     | Tarefas completas e interface clara                |                       |

---

## Como executar

> Seção a ser confirmada quando o esqueleto do projeto existir.

Pré-requisitos: **JDK 17** e, para o perfil `prod`, **Docker**.

```bash
# API em modo dev (H2 em memória, com dados iniciais)
./mvnw -pl api spring-boot:run -Dspring-boot.run.profiles=dev

# API em modo entrega (PostgreSQL)
cp .env.example .env
docker compose up -d
./mvnw -pl api spring-boot:run -Dspring-boot.run.profiles=prod

# Cliente desktop
./mvnw -pl desktop exec:java

# Testes
./mvnw test
```

No Windows, usar `mvnw.cmd` no lugar de `./mvnw`.

Variáveis de ambiente (`.env.example`, **sem senhas reais**):

```dotenv
POSTGRES_DB=roteirizacao
POSTGRES_USER=roteirizacao
POSTGRES_PASSWORD=troque-me
SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/roteirizacao
```

O `.env` real **não vai para o repositório**. Segredos não aparecem em logs.

---

## Convenções da equipe

**Camadas:**

- O controller é **fino**: valida a entrada com Bean Validation e delega.
- O service concentra as **regras e as transações**.
- O repository cuida **só da persistência**.
- DTOs separam o contrato externo das entidades JPA.

**Código:**

- Nomes expressivos em português, métodos focados em uma tarefa e sem duplicação de cálculos e validações.
- Comentários explicam **decisões, fórmulas e hipóteses**, não o óbvio.

**GUI:**

- Rótulos com unidades (km, min) e um cenário de exemplo pré-carregado.
- Mensagens dizem qual campo está errado e como corrigir, por exemplo: _"Selecione um ponto de destino diferente da origem."_
- Feedback ao importar, calcular e salvar. Navegação por teclado.
- A rota não é indicada só por cor: usar legenda e destaque de traço.
- A janela não trava durante importações e cálculos.

**Git:**

- Branches `feature/<assunto>`, Pull Request para `main` e revisão por outro integrante.
- Commits pequenos e descritivos.

---

## Cronograma e apresentações

A cada semana, todos os grupos apresentam:

1. O que foi feito.
2. Demonstração dos resultados.
3. Dificuldades.
4. Próximos passos, com responsáveis e prazos.

> **Requisitos congelados ao fim da semana 1.** Integrações opcionais só depois do aceite do núcleo.

### Dia 26/10 - Contrato, dados e domínio

A gente deve apresentar: classes, regras, dados e API mínima.

- [ ] Esqueleto Maven multimódulo (`api`, `desktop`) com Maven Wrapper, compilando
- [ ] Banco, Java 17 e dependências validados em todas as máquinas
- [ ] Classes de domínio: `Ponto`, `Via`, `Grafo`, `CriterioRota`, `MenorDistancia`, `MenorTempo`
- [ ] Dijkstra passando nos testes da rede de 5 nós
- [ ] `pontos.csv` (20) e `vias.csv` (30) com fontes documentadas
- [ ] `docs/openapi.yaml` com os 4 endpoints
- [ ] Diagrama de classes inicial

### Dia 02/11 - Fluxo completo

A gente deve apresentar: GUI chama API e grava no banco.

- [ ] Endpoints implementados com JPA (H2) e `@ControllerAdvice`
- [ ] GUI: importar rede, escolher origem/destino/critério, calcular e exibir resultado
- [ ] Chamadas via `SwingWorker` + `HttpClient`

### Dia 09/11 - Desafio técnico e arquivos

A gente deve apresentar: regras, CSV e configurações binárias.

- [ ] Incidentes bloqueiam vias e o recálculo evita os trechos bloqueados
- [ ] Tratamento de "sem caminho" na API e na GUI
- [ ] Mapa esquemático com a rota destacada
- [ ] Exportação CSV da rota e `config.bin` salvo/recuperado

### Dia 16/11 - Testes e usabilidade

A gente deve apresentar: falhas tratadas e teste POR OUTRA EQUIPE.

- [ ] 8+ testes passando (regras, entradas inválidas, arquivos, integração, REST)
- [ ] Teste de uso feito por outra equipe, com dificuldades registradas e melhorias demonstradas
- [ ] Perfil `prod` com PostgreSQL via `compose.yaml`

### Dia 23/11 - Correções e entrega final

A gente deve apresentar: execução limpa e defesa individual.

- [ ] Execução do zero seguindo apenas este README
- [ ] Matriz do PEA completa
- [ ] Cada integrante explica sua contribuição e uma decisão técnica

---

## Entrega final

- **Protótipo executável:** código, dependências, README e configuração de exemplo sem senhas reais.
- **Dados e documentação:** dados brutos e tratados, fontes ou regras de geração, dicionário e contrato REST.
- **Modelagem e validação:** diagrama de classes, matriz do PEA completa e testes com resultados esperados.
- **Demonstração e autoria:**
  - os três cenários
  - configurações recuperadas
  - registros semanais
  - contribuição de cada integrante

---

## Critérios de avaliação

Pesos propostos no guia, sujeitos à confirmação do docente:

| Critério                | Peso | Evidência                                     |
| ----------------------- | ---- | --------------------------------------------- |
| Aplicação do PEA        | 30   | Matriz completa e explicação do código        |
| MVP e regras do domínio | 20   | Critérios de aceite e três cenários           |
| Robustez e persistência | 15   | Testes, erros, CSV e arquivo binário          |
| GUI e documentação      | 15   | Usabilidade, fontes e execução limpa          |
| Apresentações semanais  | 10   | Avanços, dificuldades e próximos passos       |
| Domínio individual      | 10   | Explicar a contribuição e uma decisão técnica |

---

## Referências

- Guia do projeto: _Microsserviços Java aplicados_ (slides 8–9: tema Logística).
- Schildt (2014), _Java: A referência completa_. Horstmann e Cornell (2005), _Core Java 2_.
- Java Nivelando: <https://github.com/Iochem/java-nivelando>
- Requisitos do Spring Boot 3.5: <https://docs.spring.io/spring-boot/3.5/system-requirements.html>
