package com.logistica.roteirizacao.client;

import java.io.File;

import com.logistica.roteirizacao.client.dto.Criterio;
import com.logistica.roteirizacao.client.dto.ImportacaoResultado;
import com.logistica.roteirizacao.client.dto.IncidenteResultado;
import com.logistica.roteirizacao.client.dto.RotaResultado;

/**
 * Contrato entre as telas (pacote ui) e a API.
 *
 * <p>As telas dependem só desta interface: durante o desenvolvimento podem usar
 * uma implementação falsa e, depois, {@link ClienteApiHttp}. Os métodos fazem
 * chamadas bloqueantes; na GUI, chame-os por {@link TarefaEmSegundoPlano}.
 *
 * <p>Toda falha (validação, recurso inexistente, API fora do ar) vira
 * {@link ClienteApiException} com uma mensagem pronta para mostrar ao usuário.
 */
public interface ClienteApi {

    /** POST /redes/importacoes: substitui a rede atual pela dos dois CSVs. */
    ImportacaoResultado importarRede(File pontos, File vias) throws ClienteApiException;

    /** POST /rotas. "Sem caminho" não lança exceção: veja {@link RotaResultado#semCaminho()}. */
    RotaResultado calcularRota(String origemId, String destinoId, Criterio criterio) throws ClienteApiException;

    /** POST /rotas com o critério padrão da aplicação, menor distância. */
    default RotaResultado calcularRota(String origemId, String destinoId) throws ClienteApiException {
        return calcularRota(origemId, destinoId, Criterio.MENOR_DISTANCIA);
    }

    /** GET /rotas/{id}. */
    RotaResultado buscarRota(long id) throws ClienteApiException;

    /** POST /incidentes: bloqueia a via nos dois sentidos. */
    IncidenteResultado registrarIncidente(String viaId, String descricao) throws ClienteApiException;
}
