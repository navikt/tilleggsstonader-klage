package no.nav.tilleggsstonader.klage.formkrav

import no.nav.tilleggsstonader.klage.formkrav.domain.Form
import no.nav.tilleggsstonader.klage.repository.InsertUpdateRepository
import no.nav.tilleggsstonader.klage.repository.RepositoryInterface
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface FormRepository : RepositoryInterface<Form, UUID>, InsertUpdateRepository<Form>
