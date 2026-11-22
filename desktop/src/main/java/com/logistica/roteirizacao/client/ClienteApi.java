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

    /**
     * POST /redes/importacoes: substitui a rede atual pela dos dois CSVs.
     *
     * @param pontos arquivo pontos.csv
     * @param vias   arquivo vias.csv
     * @return quantidades importadas, nova versão da rede e linhas rejeitadas
     * @throws ClienteApiException se a API recusar o pedido ou não responder
     */
    ImportacaoResultado importarRede(File pontos, File vias) throws ClienteApiException;

    /**
     * POST /rotas. "Sem caminho" não lança exceção: veja {@link RotaResultado#semCaminho()}.
     *
     * @param origemId  ponto de partida
     * @param destinoId ponto de chegada
     * @param criterio  critério de custo
     * @return a rota calculada, ou o registro sem caminho
     * @throws ClienteApiException se a API recusar o pedido ou não responder
     */
    RotaResultado calcularRota(String origemId, String destinoId, Criterio criterio) throws ClienteApiException;

    /**
     * POST /rotas com o critério padrão da aplicação, menor distância.
     *
     * @param origemId  ponto de partida
     * @param destinoId ponto de chegada
     * @return a rota calculada, ou o registro sem caminho
     * @throws ClienteApiException se a API recusar o pedido ou não responder
     */
    default RotaResultado calcularRota(String origemId, String destinoId) throws ClienteApiException {
        return calcularRota(origemId, destinoId, Criterio.MENOR_DISTANCIA);
    }

    /**
     * GET /rotas/{id}.
     *
     * @param id identificador da rota
     * @return a rota gravada
     * @throws ClienteApiException se a API recusar o pedido ou não responder
     */
    RotaResultado buscarRota(long id) throws ClienteApiException;

    /**
     * POST /incidentes: bloqueia a via nos dois sentidos.
     *
     * @param viaId     via a bloquear
     * @param descricao motivo do bloqueio (3 a 200 caracteres)
     * @return o incidente registrado
     * @throws ClienteApiException se a API recusar o pedido ou não responder
     */
    IncidenteResultado registrarIncidente(String viaId, String descricao) throws ClienteApiException;

    /**
     * POST /incidentes/{id}/encerramento: desbloqueia a via do incidente.
     *
     * @param incidenteId identificador do incidente
     * @return o incidente encerrado
     * @throws ClienteApiException se a API recusar o pedido ou não responder
     */
    IncidenteResultado encerrarIncidente(long incidenteId) throws ClienteApiException;
}
