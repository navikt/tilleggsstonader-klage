package no.nav.tilleggsstonader.klage.behandlingsstatistikk

import no.nav.tilleggsstonader.klage.infrastruktur.config.ObjectMapperProvider.objectMapper
import no.nav.tilleggsstonader.kontrakter.saksstatistikk.BehandlingKlageDvh
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.stereotype.Component

@Component
class BehandlingsstatistikkProducer(
    private val kafkaTemplate: KafkaTemplate<String, String>,
) {
    @Value("\${BEHANDLINGSSTATISTIKK_TOPIC}")
    lateinit var topic: String

    private val logger = LoggerFactory.getLogger(javaClass)
    private val secureLogger = LoggerFactory.getLogger("secureLogger")

    fun sendBehandlingsstatistikk(statistikk: BehandlingKlageDvh) {
        logger.info("Sending to Kafka topic: {}", topic)
        if (secureLogger.isDebugEnabled) {
            secureLogger.debug("Sending to Kafka topic: {}\nBehandlingsstatistikkKlage: {}", topic, statistikk)
        }
        runCatching {
            kafkaTemplate.send(topic, statistikk.behandlingId.toString(), statistikk.toJson()).get()
            logger.info(
                "Behandlingstatistikk for behandling=${statistikk.behandlingId} " +
                    "behandlingStatus=${statistikk.behandlingStatus} sent til Kafka",
            )
        }.onFailure {
            val errorMessage = "Could not send behandling to Kafka. Check secure logs for more information."
            logger.error(errorMessage)
            secureLogger.error("Could not send behandling to Kafka", it)
            throw RuntimeException(errorMessage)
        }
    }

    private fun Any.toJson(): String = objectMapper.writeValueAsString(this)
}
