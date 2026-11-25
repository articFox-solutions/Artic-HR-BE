package com.articfox.hr.usuario;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UsuarioRepository extends JpaRepository<Usuario, UUID> {

    /** Trae usuario, colaborador y departamento en una sola consulta para evitar N+1. */
    @Query("""
            select u from Usuario u
            join fetch u.colaborador c
            join fetch c.departamento
            where c.correoEmpresarial = :correo
            """)
    Optional<Usuario> findByCorreo(@Param("correo") String correo);

    @Query("""
            select u from Usuario u
            join fetch u.colaborador c
            join fetch c.departamento
            where u.id = :id
            """)
    Optional<Usuario> findConPerfilById(@Param("id") UUID id);

    @Query("""
            select new com.articfox.hr.usuario.EstadoAcceso(u.activo, c.estado, u.debeCambiarContrasena)
            from Usuario u join u.colaborador c
            where u.id = :id
            """)
    Optional<EstadoAcceso> findEstadoAcceso(@Param("id") UUID id);
}
