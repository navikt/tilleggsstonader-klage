package no.nav.tilleggsstonader.klage.infrastruktur.repository

import no.nav.tilleggsstonader.klage.felles.domain.BehandlingId
import org.springframework.core.convert.converter.Converter
import org.springframework.data.convert.ReadingConverter
import org.springframework.data.convert.WritingConverter
import java.util.UUID

/**
 * Spring data jdbc støtter ikke value classes for primary keys, dvs @Id-markerte felter
 */
object IdConverters {
    @WritingConverter
    abstract class ValueClassWriter<T : Any>(
        val convert: (T) -> UUID,
    ) : Converter<T, UUID> {
        override fun convert(valueClass: T): UUID = this.convert.invoke(valueClass)
    }

    @ReadingConverter
    abstract class ValueClassReader<T : Any>(
        val convert: (UUID) -> T,
    ) : Converter<UUID, T> {
        override fun convert(id: UUID): T = this.convert.invoke(id)
    }

    class BehandlingIdWritingConverter : ValueClassWriter<BehandlingId>({ it.id })

    class BehandlingIdReaderConverter : ValueClassReader<BehandlingId>({ BehandlingId(it) })

    val alleValueClassConverters =
        listOf(
            BehandlingIdWritingConverter(),
            BehandlingIdReaderConverter(),
        )
}
