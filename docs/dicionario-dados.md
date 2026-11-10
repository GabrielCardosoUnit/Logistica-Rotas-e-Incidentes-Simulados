# Dicionário de dados

Classificação da origem de cada campo:
- **Publicado:** obtido de uma fonte externa.
- **Assumido:** premissa da equipe.
- **Simulado:** construído para o exercício.
- **Gerado:** calculado por script a partir de outros campos.

Nenhum campo é **medido** em campo.

## `dados/tratados/pontos.csv`

| Campo | Tipo | Unidade | Regra | Origem |
|---|---|---|---|---|
| `id` | texto | — | `P01` a `P20`, único | Simulado |
| `nome` | texto | — | Nome do local | Publicado (OSM), padronizado pela equipe |
| `latitude` | decimal | graus (WGS84) | Entre -90 e 90. Usada só para desenhar o mapa | Publicado (OSM) |
| `longitude` | decimal | graus (WGS84) | Entre -180 e 180. Usada só para desenhar o mapa | Publicado (OSM) |

## `dados/tratados/vias.csv`

| Campo | Tipo | Unidade | Regra | Origem |
|---|---|---|---|---|
| `id` | texto | — | `V01` a `V30`, único | Simulado |
| `origem` | texto | — | `id` existente em `pontos.csv` | Simulado |
| `destino` | texto | — | `id` existente em `pontos.csv`, diferente de `origem` | Simulado |
| `distanciaKm` | decimal | km | Maior que 0, uma casa decimal. Linha reta × 1,3 | Gerado (premissa assumida) |
| `tempoMin` | decimal | minutos | Maior que 0, uma casa decimal. `distanciaKm` ÷ velocidade do tipo × 60 | Gerado (premissa assumida) |
| `habilitada` | booleano | — | `true` ou `false`. Com `false`, a via não entra no cálculo | Simulado (todas `true` na carga inicial) |

As vias são de **mão dupla**. Um incidente bloqueia os dois sentidos.

## `dados/brutos/locais.csv`

Lista original consultada, mantida sem alteração.

| Campo | Descrição |
|---|---|
| `id`, `nome` | Identificador e nome atribuídos pela equipe |
| `consulta` | Texto enviado ao Nominatim |
| `latitude`, `longitude` | Coordenadas retornadas |
| `osm_tipo`, `osm_id` | Objeto do OpenStreetMap encontrado (`node`, `way` ou `relation`) |
| `endereco_osm` | Endereço completo retornado pelo OSM |

## `dados/brutos/ligacoes.csv`

| Campo | Descrição |
|---|---|
| `id`, `origem`, `destino` | Ligação definida pela equipe |
| `tipo` | `arterial` (avenida principal, 35 km/h) ou `local` (rua de bairro, 20 km/h) |

## Tratamento aplicado (bruto → tratado)

| Verificação | Resultado na carga atual |
|---|---|
| Ausências (campos vazios) | Nenhuma |
| Duplicidades de `id` | Nenhuma |
| Valores inválidos (peso ≤ 0, ponto inexistente) | Nenhum |
| Conversões | Coordenadas mantidas como texto do OSM. Pesos arredondados para uma casa decimal |
| Consultas sem resultado | Duas, refeitas com outro nome (ver `dados/brutos/FONTES.md`) |

---

## Rede do campus da UNIT (`dados/tratados/unit/`)

Os arquivos tratados têm **os mesmos campos** da rede da cidade (veja acima). Mudam os ids e a origem dos dados:

| Campo | Diferença em relação à rede da cidade | Origem |
|---|---|---|
| `id` (pontos) | `U01` a `U13` | Simulado |
| `nome`, `latitude`, `longitude` | Coletados pela equipe no Google Maps (o OSM não tem os prédios do campus com nome) | Coletado pela equipe |
| `id` (vias) | `C01` a `C21` (caminhos a pé) | Simulado |
| `distanciaKm` | Linha reta × 1,2, com **2 casas decimais** (precisão de 10 m) | Gerado (premissa assumida) |
| `tempoMin` | `distanciaKm` ÷ velocidade de caminhada × 60: `livre` a 5 km/h, `movimentado` a 3 km/h | Gerado (premissa assumida) |

Arquivos brutos em `dados/brutos/unit/`:

| Arquivo | Conteúdo |
|---|---|
| `coletado-pela-equipe.txt` | Texto enviado pela equipe, sem nenhuma alteração |
| `locais.csv` | `id`, `nome`, `latitude`, `longitude` e `observacao` (correção aplicada, quando houver) |
| `ligacoes.csv` | `id`, `origem`, `destino` e `tipo` (`livre` ou `movimentado`) |

### Tratamento aplicado (campus)

| Verificação | Resultado |
|---|---|
| Ausências | Nenhuma |
| Duplicidades de `id` | Nenhuma |
| Valores inválidos | 1: latitude do Bloco E/F sem o sinal negativo (`10.966727`), corrigida para `-10.966727` |
| Padronização | "Unit portaria norte" passou a "Portaria norte" |
| Conversões | Pesos com 2 casas decimais em km e 1 em minutos |
