package com.articfox.hr.auditoria;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AuditoriaLoginRepository extends JpaRepository<AuditoriaLogin, UUID> {
}
