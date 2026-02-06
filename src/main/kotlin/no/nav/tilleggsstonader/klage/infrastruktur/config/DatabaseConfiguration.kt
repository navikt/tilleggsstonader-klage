package no.nav.tilleggsstonader.klage.infrastruktur.config

import no.nav.familie.prosessering.PropertiesWrapperTilStringConverter
import no.nav.familie.prosessering.StringTilPropertiesWrapperConverter
import no.nav.tilleggsstonader.klage.behandling.domain.FagsystemRevurdering
import no.nav.tilleggsstonader.klage.behandling.domain.PåklagetVedtakDetaljer
import no.nav.tilleggsstonader.klage.behandling.vent.SettPåVent
import no.nav.tilleggsstonader.klage.brev.domain.Brevmottakere
import no.nav.tilleggsstonader.klage.brev.domain.BrevmottakereJournalposter
import no.nav.tilleggsstonader.klage.felles.domain.Endret
import no.nav.tilleggsstonader.klage.felles.domain.Fil
import no.nav.tilleggsstonader.klage.infrastruktur.repository.IdConverters.alleValueClassConverters
import no.nav.tilleggsstonader.klage.infrastruktur.repository.JsonWrapper
import no.nav.tilleggsstonader.klage.vurdering.domain.Hjemler
import no.nav.tilleggsstonader.kontrakter.felles.JsonMapperProvider.jsonMapper
import org.apache.commons.lang3.StringUtils
import org.postgresql.util.PGobject
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.flyway.autoconfigure.FlywayConfigurationCustomizer
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.convert.converter.Converter
import org.springframework.core.env.Environment
import org.springframework.data.convert.ReadingConverter
import org.springframework.data.convert.WritingConverter
import org.springframework.data.domain.AuditorAware
import org.springframework.data.jdbc.core.convert.QueryMappingConfiguration
import org.springframework.data.jdbc.repository.config.AbstractJdbcConfiguration
import org.springframework.data.jdbc.repository.config.DefaultQueryMappingConfiguration
import org.springframework.data.jdbc.repository.config.EnableJdbcAuditing
import org.springframework.data.jdbc.repository.config.EnableJdbcRepositories
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcOperations
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.jdbc.datasource.DataSourceTransactionManager
import org.springframework.transaction.PlatformTransactionManager
import org.springframework.transaction.annotation.EnableTransactionManagement
import org.springframework.transaction.annotation.RollbackOn
import tools.jackson.module.kotlin.readValue
import java.util.Optional
import javax.sql.DataSource

@Configuration
@EnableJdbcAuditing
@EnableJdbcRepositories("no.nav.tilleggsstonader.klage", "no.nav.familie.prosessering")
@EnableTransactionManagement(rollbackOn = RollbackOn.ALL_EXCEPTIONS)
class DatabaseConfiguration : AbstractJdbcConfiguration() {
    @Bean
    fun operations(dataSource: DataSource): NamedParameterJdbcOperations = NamedParameterJdbcTemplate(dataSource)

    @Bean
    fun transactionManager(dataSource: DataSource): PlatformTransactionManager = DataSourceTransactionManager(dataSource)

    @Bean
    fun auditSporbarEndret(): AuditorAware<Endret> =
        AuditorAware {
            Optional.of(Endret())
        }

    @Bean
    fun rowMappers(): QueryMappingConfiguration =
        DefaultQueryMappingConfiguration()
            .registerRowMapper(SettPåVent::class.java, SettPåVentRowMapper())

    override fun userConverters(): List<*> =
        listOf(
            PropertiesWrapperTilStringConverter(),
            StringTilPropertiesWrapperConverter(),
            StringListTilStringConverter(),
            StringTilStringList(),
            FilTilBytearrayConverter(),
            BytearrayTilFilConverter(),
            BrevmottakereTilBytearrayConverter(),
            BytearrayTilBrevmottakereConverter(),
            BrevmottakereJournalposterTilBytearrayConverter(),
            BytearrayTilBrevmottakereJournalposterConverter(),
            PåklagetVedtakDetaljerTilBytearrayConverter(),
            BytearrayTilPåklagetVedtakDetaljerConverter(),
            OpprettetRevurderingTilBytearrayConverter(),
            BytearrayTilOpprettetRevurderingConverter(),
            HjemlerTilJsonConverter(),
            JsonTilHjemlerConverter(),
            PGobjectTilJsonWrapperConverter(),
            JsonWrapperTilPGobjectConverter(),
        ) + alleValueClassConverters

    @Bean
    fun verifyIgnoreIfProd(
        @Value("\${spring.flyway.placeholders.ignoreIfProd}") ignoreIfProd: String,
        environment: Environment,
    ): FlywayConfigurationCustomizer {
        val isProd = environment.activeProfiles.contains("prod")
        val ignore = ignoreIfProd == "--"
        return FlywayConfigurationCustomizer {
            if (isProd && !ignore) {
                throw RuntimeException("Prod profile men har ikke riktig verdi for placeholder ignoreIfProd=$ignoreIfProd")
            }
            if (!isProd && ignore) {
                throw RuntimeException("Profile=${environment.activeProfiles} men har ignoreIfProd=--")
            }
        }
    }

    data class StringListWrapper(
        val verdier: List<String>,
    )

    @WritingConverter
    class StringListTilStringConverter : Converter<StringListWrapper, String> {
        override fun convert(wrapper: StringListWrapper): String = StringUtils.join(wrapper.verdier, ";")
    }

    @ReadingConverter
    class StringTilStringList : Converter<String, StringListWrapper> {
        override fun convert(verdi: String): StringListWrapper = StringListWrapper(verdi.split(";"))
    }

    @WritingConverter
    class FilTilBytearrayConverter : Converter<Fil, ByteArray> {
        override fun convert(fil: Fil): ByteArray = fil.bytes
    }

    @ReadingConverter
    class BytearrayTilFilConverter : Converter<ByteArray, Fil> {
        override fun convert(bytes: ByteArray): Fil = Fil(bytes)
    }

    @WritingConverter
    class BrevmottakereTilBytearrayConverter : Converter<Brevmottakere, PGobject> {
        override fun convert(o: Brevmottakere): PGobject =
            PGobject().apply {
                type = "json"
                value = jsonMapper.writeValueAsString(o)
            }
    }

    @ReadingConverter
    class BytearrayTilBrevmottakereConverter : Converter<PGobject, Brevmottakere> {
        override fun convert(pGobject: PGobject): Brevmottakere = jsonMapper.readValue(pGobject.value!!)
    }

    @WritingConverter
    class BrevmottakereJournalposterTilBytearrayConverter : Converter<BrevmottakereJournalposter, PGobject> {
        override fun convert(o: BrevmottakereJournalposter): PGobject =
            PGobject().apply {
                type = "json"
                value = jsonMapper.writeValueAsString(o)
            }
    }

    @ReadingConverter
    class BytearrayTilBrevmottakereJournalposterConverter : Converter<PGobject, BrevmottakereJournalposter> {
        override fun convert(pGobject: PGobject): BrevmottakereJournalposter = jsonMapper.readValue(pGobject.value!!)
    }

    @WritingConverter
    class PåklagetVedtakDetaljerTilBytearrayConverter : Converter<PåklagetVedtakDetaljer, PGobject> {
        override fun convert(o: PåklagetVedtakDetaljer): PGobject =
            PGobject().apply {
                type = "json"
                value = jsonMapper.writeValueAsString(o)
            }
    }

    @ReadingConverter
    class BytearrayTilPåklagetVedtakDetaljerConverter : Converter<PGobject, PåklagetVedtakDetaljer> {
        override fun convert(pGobject: PGobject): PåklagetVedtakDetaljer = jsonMapper.readValue(pGobject.value!!)
    }

    @WritingConverter
    class OpprettetRevurderingTilBytearrayConverter : Converter<FagsystemRevurdering, PGobject> {
        override fun convert(o: FagsystemRevurdering): PGobject =
            PGobject().apply {
                type = "json"
                value = jsonMapper.writeValueAsString(o)
            }
    }

    @ReadingConverter
    class BytearrayTilOpprettetRevurderingConverter : Converter<PGobject, FagsystemRevurdering> {
        override fun convert(pGobject: PGobject): FagsystemRevurdering = jsonMapper.readValue(pGobject.value!!)
    }

    @WritingConverter
    class HjemlerTilJsonConverter : Converter<Hjemler, PGobject> {
        override fun convert(o: Hjemler): PGobject =
            PGobject().apply {
                type = "json"
                value = jsonMapper.writeValueAsString(o.hjemler)
            }
    }

    @ReadingConverter
    class JsonTilHjemlerConverter : Converter<PGobject, Hjemler> {
        override fun convert(pGobject: PGobject): Hjemler = Hjemler(jsonMapper.readValue(pGobject.value!!))
    }

    @ReadingConverter
    class PGobjectTilJsonWrapperConverter : Converter<PGobject, JsonWrapper?> {
        override fun convert(pGobject: PGobject): JsonWrapper? = pGobject.value?.let { JsonWrapper(it) }
    }

    @WritingConverter
    class JsonWrapperTilPGobjectConverter : Converter<JsonWrapper, PGobject> {
        override fun convert(jsonWrapper: JsonWrapper): PGobject =
            PGobject().apply {
                type = "json"
                value = jsonWrapper.json
            }
    }
}
