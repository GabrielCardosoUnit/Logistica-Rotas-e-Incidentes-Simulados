# Fontes dos dados

## Pontos (coordenadas)

| Item | Valor |
|---|---|
| Fonte | OpenStreetMap, via serviço de busca Nominatim (`https://nominatim.openstreetmap.org/search`) |
| Licença | Dados © colaboradores do OpenStreetMap, licença ODbL 1.0 (<https://www.openstreetmap.org/copyright>) |
| Data de acesso | 25/10/2026 |
| Região | Aracaju/SE, Brasil |
| Sistema de coordenadas | WGS84 (graus decimais) |
| Arquivos | `locais.csv` (consulta usada, coordenadas, tipo e id do objeto no OSM, endereço retornado) e `nominatim_respostas.json` (resposta original de cada consulta) |

Escolha dos locais:

- **Pedidos pela equipe:** Universidade Tiradentes (UNIT), Shopping RioMar e Shopping Jardins.
- **Os outros 17:** locais públicos ou comerciais conhecidos, escolhidos para espalhar a rede pela cidade e ter pontos com papel logístico (aeroporto, rodoviária, distrito industrial, hospitais).
- **Sem endereços residenciais** nem dados pessoais.

Consultas que não retornaram resultado e foram trocadas por outro nome:

- "Terminal Rodoviário José Rollemberg Leite" → "Rodoviária Nova Aracaju"
- "Mercado Municipal Antônio Franco, Aracaju" → "Mercado Antônio Franco"

O ponto retornado para o Mercado é o **Mercado Municipal de Artesanato Antônio Franco**.

A coordenada é a que o OSM devolve para o objeto. Em áreas grandes (aeroporto, distrito industrial, parques), é um ponto representativo, não uma entrada específica.

## Vias (ligações e pesos)

| Item | Origem do dado |
|---|---|
| Quais pontos se ligam (`ligacoes.csv`) | **Simulado.** Definido pela equipe com base na proximidade geográfica e nas principais avenidas. Não reproduz o traçado real das ruas |
| Tipo da via (`arterial` / `local`) | **Assumido** pela equipe |
| `distanciaKm` | **Assumido:** distância em linha reta (fórmula de haversine) × 1,3 (fator de sinuosidade) |
| `tempoMin` | **Assumido:** `distanciaKm` ÷ velocidade média × 60, com 35 km/h para via arterial e 20 km/h para via local |

As regras de geração estão em `dados/scripts/gerar_rede.py`. Não há aleatoriedade, portanto não há semente: o mesmo bruto gera sempre os mesmos tratados.

> A rede é **simulada**. Distâncias e tempos não representam trânsito real e não devem ser usados para planejar deslocamentos.
