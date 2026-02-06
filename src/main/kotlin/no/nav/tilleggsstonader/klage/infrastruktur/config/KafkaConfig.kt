package no.nav.tilleggsstonader.klage.infrastruktur.config

import no.nav.tilleggsstonader.libs.kafka.KafkaErrorHandler
import org.springframework.boot.kafka.autoconfigure.KafkaProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Import
import org.springframework.kafka.annotation.EnableKafka
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory
import org.springframework.kafka.core.DefaultKafkaConsumerFactory
import org.springframework.kafka.core.DefaultKafkaProducerFactory
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.kafka.support.LoggingProducerListener

@EnableKafka
@Configuration
@Import(
    KafkaErrorHandler::class,
)
class KafkaConfig {
    @Bean
    fun klageEventListenerContainerFactory(
        properties: KafkaProperties,
        kafkaErrorHandler: KafkaErrorHandler,
    ): ConcurrentKafkaListenerContainerFactory<String, String> =
        ConcurrentKafkaListenerContainerFactory<String, String>().apply {
            @Suppress("UsePropertyAccessSyntax")
            setConsumerFactory(DefaultKafkaConsumerFactory(properties.buildConsumerProperties()))
            setCommonErrorHandler(kafkaErrorHandler)
        }

    @Bean
    fun kafkaTemplate(properties: KafkaProperties): KafkaTemplate<String, String> {
        val producerListener = LoggingProducerListener<String, String>()
        producerListener.setIncludeContents(false)
        val producerFactory = DefaultKafkaProducerFactory<String, String>(properties.buildProducerProperties())

        return KafkaTemplate(producerFactory).apply {
            setProducerListener(producerListener)
        }
    }
}
