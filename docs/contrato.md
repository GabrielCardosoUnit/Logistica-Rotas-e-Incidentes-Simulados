# Contrato da API REST

Endpoints implementados no módulo `api` (pacote `controller`), com os formatos de
requisição e resposta. Versão formal com schemas: [openapi.yaml](openapi.yaml).

- **Base:** `http://localhost:8080`
- **Formato:** JSON (UTF-8), exceto a importação, que usa `multipart/form-data`
- **Datas:** ISO 8601 com fuso de Brasília, por exemplo `2026-10-26T10:15:00-03:00`
- **Autenticação:** nenhuma (MVP local)

| Método | Rota                 | Descrição                               | Sucesso | Erros possíveis    |
| ------ | -------------------- | --------------------------------------- | ------- | ------------------ |
| `POST` | `/redes/importacoes` | Importa `pontos.csv` e `vias.csv`       | `201`   | 400, 503           |
| `POST` | `/rotas`             | Calcula uma rota entre origem e destino | `201`   | 400, 404, 503      |
| `GET`  | `/rotas/{id}`        | Consulta uma rota já calculada          | `200`   | 400, 404, 503      |
| `POST` | `/incidentes`        | Bloqueia uma via                        | `201`   | 400, 404, 409, 503 |

Os exemplos usam a rede de `dados/tratados/` (Aracaju, 20 pontos e 30 vias).

---

## 1. `POST /redes/importacoes`

Substitui a rede atual pela dos arquivos enviados e incrementa `versaoRede`.
Os incidentes da rede anterior são apagados. As rotas já calculadas continuam
no histórico.

As linhas com problema são rejeitadas e listadas na resposta; as outras são
importadas. Se um arquivo não tiver nenhuma linha válida, nada é alterado e a
resposta é `400`.

### Requisição

`Content-Type: multipart/form-data`

| Campo    | Tipo    | Cabeçalho obrigatório do CSV                         |
| -------- | ------- | ---------------------------------------------------- |
| `pontos` | arquivo | `id,nome,latitude,longitude`                         |
| `vias`   | arquivo | `id,origem,destino,distanciaKm,tempoMin,habilitada`  |

```bash
curl -X POST http://localhost:8080/redes/importacoes \
  -F "pontos=@dados/tratados/pontos.csv" \
  -F "vias=@dados/tratados/vias.csv"
```

Regras de cada linha:

- `id` não pode se repetir no mesmo arquivo.
- `latitude` entre -90 e 90; `longitude` entre -180 e 180.
- `origem` e `destino` precisam existir em `pontos.csv` e ser diferentes.
- `distanciaKm` e `tempoMin` maiores que zero, com ponto como separador decimal.
- `habilitada` deve ser `true` ou `false`.

### Resposta `201 Created`

```json
{
  "versaoRede": 1,
  "pontosImportados": 20,
  "viasImportadas": 30,
  "linhasRejeitadas": []
}
```

Com linhas rejeitadas (`linha` conta o cabeçalho como 1):

```json
{
  "versaoRede": 2,
  "pontosImportados": 20,
  "viasImportadas": 28,
  "linhasRejeitadas": [
    {
      "arquivo": "vias.csv",
      "linha": 5,
      "motivo": "distanciaKm deve ser maior que zero (valor: -1.2)."
    },
    {
      "arquivo": "vias.csv",
      "linha": 12,
      "motivo": "Ponto de destino P99 não existe em pontos.csv."
    }
  ]
}
```

| Campo              | Tipo    | Descrição                                |
| ------------------ | ------- | ---------------------------------------- |
| `versaoRede`       | inteiro | Versão da rede depois da importação      |
| `pontosImportados` | inteiro | Pontos gravados                          |
| `viasImportadas`   | inteiro | Vias gravadas                            |
| `linhasRejeitadas` | lista   | `arquivo`, `linha` e `motivo` de cada uma |

### Erros

| Status | `campo`            | Quando                                                     |
| ------ | ------------------ | ---------------------------------------------------------- |
| 400    | `pontos` ou `vias` | Arquivo ausente, vazio, cabeçalho errado ou nenhuma linha válida |

```json
{
  "status": 400,
  "erro": "VALIDACAO",
  "campo": "vias",
  "mensagem": "Cabeçalho inválido em vias.csv. Esperado: id,origem,destino,distanciaKm,tempoMin,habilitada."
}
```

---

## 2. `POST /rotas`

Calcula o menor caminho com Dijkstra pelo critério escolhido e grava o
resultado. Vias desabilitadas ou com incidente ativo não entram no cálculo.
As vias são de mão dupla.

Para **recalcular** depois de um incidente, basta chamar `POST /rotas` de novo
com a mesma origem e destino.

### Requisição

```json
{
  "origemId": "P05",
  "destinoId": "P01",
  "criterio": "MENOR_DISTANCIA"
}
```

| Campo       | Tipo  | Obrigatório | Valores                          |
| ----------- | ----- | ----------- | -------------------------------- |
| `origemId`  | texto | sim         | id de um ponto da rede           |
| `destinoId` | texto | sim         | id de um ponto da rede           |
| `criterio`  | texto | sim         | `MENOR_DISTANCIA` ou `MENOR_TEMPO` |

### Resposta `201 Created`

O cabeçalho `Location` aponta para a rota criada (`/rotas/1`).

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

Mesma rota depois de bloquear a `V24`:

```json
{
  "id": 2,
  "origemId": "P05",
  "destinoId": "P01",
  "criterio": "MENOR_DISTANCIA",
  "status": "CALCULADA",
  "pontos": ["P05", "P15", "P03", "P01"],
  "vias": ["V14", "V23", "V25"],
  "distanciaTotalKm": 11.7,
  "tempoTotalMin": 23.2,
  "versaoRede": 2,
  "mensagem": null,
  "calculadaEm": "2026-10-26T10:17:00-03:00"
}
```

**Sem caminho disponível** também responde `201` (a tentativa fica registrada),
com `status: SEM_CAMINHO`, listas vazias e totais `null`:

```json
{
  "id": 3,
  "origemId": "P05",
  "destinoId": "P07",
  "criterio": "MENOR_DISTANCIA",
  "status": "SEM_CAMINHO",
  "pontos": [],
  "vias": [],
  "distanciaTotalKm": null,
  "tempoTotalMin": null,
  "versaoRede": 4,
  "mensagem": "Não há caminho disponível de P05 até P07. Vias bloqueadas: V29, V30.",
  "calculadaEm": "2026-10-26T10:20:00-03:00"
}
```

**Origem igual ao destino:** `201` com um único ponto, `vias` vazia e totais `0.0`.

| Campo              | Tipo           | Descrição                                             |
| ------------------ | -------------- | ----------------------------------------------------- |
| `id`               | inteiro        | Identificador da rota gravada                         |
| `status`           | texto          | `CALCULADA` ou `SEM_CAMINHO`                          |
| `pontos`           | lista de texto | Pontos na ordem do percurso (vazia se `SEM_CAMINHO`)  |
| `vias`             | lista de texto | Vias percorridas, na ordem (vazia se `SEM_CAMINHO`)   |
| `distanciaTotalKm` | número ou null | Soma das distâncias, em km, com até 2 casas decimais  |
| `tempoTotalMin`    | número ou null | Soma dos tempos, em minutos, com até 2 casas decimais |
| `versaoRede`       | inteiro        | Versão da rede usada no cálculo                       |
| `mensagem`         | texto ou null  | Motivo quando não há caminho                          |
| `calculadaEm`      | data e hora    | Momento do cálculo                                    |

### Erros

| Status | `campo`                  | Mensagem                                                               |
| ------ | ------------------------ | ---------------------------------------------------------------------- |
| 400    | `origemId`               | Informe o ponto de origem.                                             |
| 400    | `destinoId`              | Informe o ponto de destino.                                            |
| 400    | `criterio`               | Critério inválido. Use MENOR_DISTANCIA ou MENOR_TEMPO.                 |
| 400    | `null`                   | Corpo da requisição ausente ou mal formado. Envie um JSON válido.      |
| 404    | `origemId` / `destinoId` | Ponto P99 não existe na rede. Importe a rede ou escolha outro ponto.   |

---

## 3. `GET /rotas/{id}`

Devolve uma rota gravada por `POST /rotas`, com os mesmos campos.

```bash
curl http://localhost:8080/rotas/1
```

### Resposta `200 OK`

Igual à resposta de `POST /rotas` para o mesmo `id`.

### Erros

| Status | `campo` | Mensagem                                                     |
| ------ | ------- | ------------------------------------------------------------ |
| 400    | `id`    | O valor 'abc' não é válido para id. Informe um número inteiro. |
| 404    | `id`    | Rota 42 não encontrada.                                      |

---

## 4. `POST /incidentes`

Registra um incidente ativo na via. A via deixa de ser usada nos cálculos nos
dois sentidos, e `versaoRede` sobe em 1.

### Requisição

```json
{
  "viaId": "V24",
  "descricao": "Alagamento simulado na Av. Tancredo Neves"
}
```

| Campo       | Tipo  | Obrigatório | Regra                      |
| ----------- | ----- | ----------- | -------------------------- |
| `viaId`     | texto | sim         | id de uma via da rede      |
| `descricao` | texto | sim         | entre 3 e 200 caracteres   |

### Resposta `201 Created`

```json
{
  "id": 1,
  "viaId": "V24",
  "descricao": "Alagamento simulado na Av. Tancredo Neves",
  "ativo": true,
  "registradoEm": "2026-10-26T10:16:00-03:00",
  "versaoRede": 2
}
```

### Erros

| Status | `campo`     | Mensagem                                               |
| ------ | ----------- | ------------------------------------------------------ |
| 400    | `viaId`     | Informe a via a ser bloqueada.                         |
| 400    | `descricao` | A descrição deve ter entre 3 e 200 caracteres.         |
| 404    | `viaId`     | Via V99 não existe na rede.                            |
| 409    | `viaId`     | A via V24 já está bloqueada pelo incidente 1.          |
| 409    | `viaId`     | A via V24 já está desabilitada desde a importação.     |

---

## Formato dos erros

Todos os erros saem no mesmo formato, montado pelo `TratadorErrosApi`
(`@RestControllerAdvice`, pacote `exception`):

```json
{
  "status": 404,
  "erro": "NAO_ENCONTRADO",
  "campo": "origemId",
  "mensagem": "Ponto P99 não existe na rede. Importe a rede ou escolha outro ponto."
}
```

| Campo      | Descrição                                               |
| ---------- | ------------------------------------------------------- |
| `status`   | Código HTTP repetido no corpo                           |
| `erro`     | Tipo do erro (tabela abaixo)                            |
| `campo`    | Campo que causou o erro, ou `null` quando não se aplica |
| `mensagem` | O que está errado e como corrigir; pode ir direto para a GUI |

| `erro`           | Status | Quando                                                       |
| ---------------- | ------ | ------------------------------------------------------------ |
| `VALIDACAO`      | 400    | Campo ausente ou inválido, JSON mal formado, CSV inválido    |
| `NAO_ENCONTRADO` | 404    | Ponto, via, rota ou endereço inexistente                     |
| `CONFLITO`       | 409    | Via já bloqueada ou já desabilitada                          |
| `INDISPONIVEL`   | 503    | Banco de dados fora do ar                                    |
| `INTERNO`        | 500    | Falha inesperada (detalhes só no log do servidor)            |

Casos menos comuns, no mesmo formato com `erro: VALIDACAO`: `405` (método não
aceito no endereço) e `415` (tipo de conteúdo não suportado).

---

## Fluxo da demonstração

```text
1. POST /redes/importacoes          → versaoRede 1
2. POST /rotas  P05 → P01 distância → P05, P15, P01 (8.5 km)
3. POST /incidentes  V24            → versaoRede 2
4. POST /rotas  P05 → P01 distância → P05, P15, P03, P01 (11.7 km)
5. GET  /rotas/{id}                 → consulta qualquer rota calculada
```

No perfil `dev` a rede de `dados/tratados/` já é carregada ao subir a API, então
o passo 1 é opcional.
