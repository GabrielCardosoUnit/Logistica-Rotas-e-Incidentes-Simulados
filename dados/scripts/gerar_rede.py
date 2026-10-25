"""Gera dados/tratados/pontos.csv e vias.csv a partir de dados/brutos/.

Regras de geração (premissas, não medições):
- distanciaKm = distância em linha reta (haversine) x FATOR_SINUOSIDADE,
  porque a rua nunca é reta entre dois pontos.
- tempoMin = distanciaKm / velocidade média do tipo de via x 60.
- Valores arredondados para 1 casa decimal.

Uso (na raiz do repositório):  python dados/scripts/gerar_rede.py
"""
import csv
import math
from pathlib import Path

RAIZ = Path(__file__).resolve().parents[2]
BRUTOS = RAIZ / "dados" / "brutos"
TRATADOS = RAIZ / "dados" / "tratados"

RAIO_TERRA_KM = 6371.0
FATOR_SINUOSIDADE = 1.3
VELOCIDADE_KMH = {"arterial": 35.0, "local": 20.0}


def haversine_km(lat1, lon1, lat2, lon2):
    p1, p2 = math.radians(lat1), math.radians(lat2)
    dp, dl = p2 - p1, math.radians(lon2 - lon1)
    a = math.sin(dp / 2) ** 2 + math.cos(p1) * math.cos(p2) * math.sin(dl / 2) ** 2
    return 2 * RAIO_TERRA_KM * math.asin(math.sqrt(a))


def main():
    with open(BRUTOS / "locais.csv", encoding="utf-8") as f:
        locais = list(csv.DictReader(f))
    coords = {l["id"]: (float(l["latitude"]), float(l["longitude"])) for l in locais}

    TRATADOS.mkdir(parents=True, exist_ok=True)
    with open(TRATADOS / "pontos.csv", "w", newline="", encoding="utf-8") as f:
        w = csv.writer(f)
        w.writerow(["id", "nome", "latitude", "longitude"])
        for l in locais:
            w.writerow([l["id"], l["nome"], l["latitude"], l["longitude"]])

    with open(BRUTOS / "ligacoes.csv", encoding="utf-8") as f:
        ligacoes = list(csv.DictReader(f))
    with open(TRATADOS / "vias.csv", "w", newline="", encoding="utf-8") as f:
        w = csv.writer(f)
        w.writerow(["id", "origem", "destino", "distanciaKm", "tempoMin", "habilitada"])
        for v in ligacoes:
            reta = haversine_km(*coords[v["origem"]], *coords[v["destino"]])
            distancia = round(reta * FATOR_SINUOSIDADE, 1)
            tempo = round(distancia / VELOCIDADE_KMH[v["tipo"]] * 60, 1)
            w.writerow([v["id"], v["origem"], v["destino"], distancia, tempo, "true"])

    print(f"{len(locais)} pontos e {len(ligacoes)} vias gerados em {TRATADOS}")


if __name__ == "__main__":
    main()
