# Fontes da rede do campus da UNIT (Farolândia)

Segunda rede do projeto: caminhos a pé entre 13 pontos dentro do campus da Universidade Tiradentes, em Aracaju/SE. A rede da cidade (`dados/brutos/`) não muda.

## Pontos (coordenadas)

| Item | Valor |
|---|---|
| Fonte | Coletadas pela equipe no Google Maps, clicando sobre cada local |
| Data da coleta | 10/11/2026 |
| Sistema de coordenadas | WGS84 (graus decimais) |
| Arquivo bruto | `coletado-pela-equipe.txt`: texto exatamente como foi enviado, sem correções |
| Arquivo estruturado | `locais.csv`, com ids `U01` a `U13` e a coluna `observacao` com as correções |

Por que as coordenadas não vieram do OpenStreetMap, como na rede da cidade: em 10/11/2026 o interior do campus (relação OSM 3949801) tinha 42 elementos mapeados, mas nenhum prédio com nome. Só vias de estacionamento e construções sem identificação. Os nomes e as posições dos blocos vieram, portanto, do conhecimento da equipe.

### Tratamento aplicado

| Ponto | Problema no bruto | Tratamento |
|---|---|---|
| U06, Bloco E/F | Latitude `10.966727`, sem o sinal negativo: o ponto cairia no hemisfério norte, a cerca de 2.400 km do campus | Corrigida para `-10.966727`. Todos os outros pontos estão em `-10.96…`, e a posição corrigida fica entre o Bloco C e o Bloco G |
| U13, Portaria norte | Nome informado como "Unit portaria norte" | Nome padronizado para "Portaria norte" |

As demais coordenadas foram mantidas como informadas. Conferência: todos os pontos ficam a até cerca de 650 m uns dos outros, o que é coerente com o tamanho do campus.

## Vias (caminhos e pesos)

| Item | Origem do dado |
|---|---|
| Quais pontos se ligam (`ligacoes.csv`, 21 caminhos) | **Simulado.** Definido pela proximidade entre os pontos. Não reproduz o traçado real das calçadas e corredores. A equipe pode revisar |
| Tipo do caminho (`livre` / `movimentado`) | **Assumido.** "Movimentado" marca os trechos centrais, perto do Bloco A e do Mini Shopping, onde o fluxo de pessoas no intervalo deixa a caminhada mais lenta |
| `distanciaKm` | **Assumido:** distância em linha reta (fórmula de haversine) × 1,2, com 2 casas decimais (precisão de 10 m) |
| `tempoMin` | **Assumido:** `distanciaKm` ÷ velocidade × 60, com 5 km/h em caminho livre e 3 km/h em caminho movimentado |

O fator 1,2 é menor que o da cidade (1,3) porque, no campus, os caminhos a pé são mais diretos que as ruas.

As regras estão em `dados/scripts/gerar_rede.py` (rede `unit`). Não há aleatoriedade: o mesmo bruto gera sempre os mesmos tratados.

> A rede é **simulada**. Distâncias e tempos são estimativas para o exercício, não medições no campus.
