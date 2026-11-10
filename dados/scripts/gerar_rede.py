"""Gera pontos.csv e vias.csv (dados/tratados/...) a partir de dados/brutos/...

Há duas redes, cada uma com as suas premissas (não são medições):

- aracaju: 20 pontos da cidade, vias de carro.
  distanciaKm = linha reta (haversine) x 1,3; tempo pela velocidade do tipo de via
  (arterial 35 km/h, local 20 km/h); 1 casa decimal.
- unit: 13 pontos dentro do campus da UNIT (Farolândia), caminhos a pé.
  distanciaKm = linha reta x 1,2; tempo pela velocidade de caminhada do tipo de trecho
  (livre 5 km/h, movimentado 3 km/h); 2 casas decimais, porque os trechos têm dezenas de metros.

Em ambas: tempoMin = distanciaKm / velocidade x 60, com 1 casa decimal.

Uso (na raiz do repositório):
    python dados/scripts/gerar_rede.py            # as duas redes
    python dados/scripts/gerar_rede.py unit       # só uma
"""
import csv
import math
import sys
from pathlib import Path

RAIZ = Path(__file__).resolve().parents[2]
RAIO_TERRA_KM = 6371.0

REDES = {
    "aracaju": {
        "brutos": RAIZ / "dados" / "brutos",
        "tratados": RAIZ / "dados" / "tratados",
        "fator_sinuosidade": 1.3,
        "velocidade_kmh": {"arterial": 35.0, "local": 20.0},
        "casas_km": 1,
    },
    "unit": {
        "brutos": RAIZ / "dados" / "brutos" / "unit",
        "tratados": RAIZ / "dados" / "tratados" / "unit",
        "fator_sinuosidade": 1.2,
        "velocidade_kmh": {"livre": 5.0, "movimentado": 3.0},
        "casas_km": 2,
    },
}


def haversine_km(lat1, lon1, lat2, lon2):
    p1, p2 = math.radians(lat1), math.radians(lat2)
    dp, dl = p2 - p1, math.radians(lon2 - lon1)
    a = math.sin(dp / 2) ** 2 + math.cos(p1) * math.cos(p2) * math.sin(dl / 2) ** 2
    return 2 * RAIO_TERRA_KM * math.asin(math.sqrt(a))


def gerar(nome, rede):
    brutos, tratados = rede["brutos"], rede["tratados"]
    with open(brutos / "locais.csv", encoding="utf-8") as f:
        locais = list(csv.DictReader(f))
    coords = {l["id"]: (float(l["latitude"]), float(l["longitude"])) for l in locais}

    tratados.mkdir(parents=True, exist_ok=True)
    with open(tratados / "pontos.csv", "w", newline="", encoding="utf-8") as f:
        w = csv.writer(f)
        w.writerow(["id", "nome", "latitude", "longitude"])
        for l in locais:
            w.writerow([l["id"], l["nome"], l["latitude"], l["longitude"]])

    with open(brutos / "ligacoes.csv", encoding="utf-8") as f:
        ligacoes = list(csv.DictReader(f))
    with open(tratados / "vias.csv", "w", newline="", encoding="utf-8") as f:
        w = csv.writer(f)
        w.writerow(["id", "origem", "destino", "distanciaKm", "tempoMin", "habilitada"])
        for v in ligacoes:
            reta = haversine_km(*coords[v["origem"]], *coords[v["destino"]])
            distancia = round(reta * rede["fator_sinuosidade"], rede["casas_km"])
            if distancia <= 0:
                raise ValueError(f"{nome}: via {v['id']} ficou com distância zero; aumente casas_km.")
            tempo = round(distancia / rede["velocidade_kmh"][v["tipo"]] * 60, 1)
            w.writerow([v["id"], v["origem"], v["destino"], distancia, tempo, "true"])

    print(f"{nome}: {len(locais)} pontos e {len(ligacoes)} vias gerados em {tratados}")


def main(argumentos):
    nomes = argumentos or list(REDES)
    for nome in nomes:
        if nome not in REDES:
            sys.exit(f"Rede desconhecida: {nome}. Use uma de: {', '.join(REDES)}.")
        gerar(nome, REDES[nome])


if __name__ == "__main__":
    main(sys.argv[1:])
