-- Modelo de datos del Incremento 1 (DERCAS 1.05, sección 6)

CREATE TABLE departamento (
    id     UUID PRIMARY KEY,
    nombre VARCHAR(120) NOT NULL UNIQUE
);

CREATE TABLE colaborador (
    id                 UUID PRIMARY KEY,
    codigo_empleado    VARCHAR(20)  NOT NULL UNIQUE,
    nombre             VARCHAR(150) NOT NULL,
    -- Se guarda en minúsculas para que el login no dependa de mayúsculas
    correo_empresarial VARCHAR(254) NOT NULL UNIQUE
        CHECK (correo_empresarial = lower(correo_empresarial)),
    puesto             VARCHAR(120) NOT NULL,
    departamento_id    UUID         NOT NULL REFERENCES departamento (id),
    estado             VARCHAR(10)  NOT NULL CHECK (estado IN ('ACTIVO', 'INACTIVO'))
);

CREATE INDEX colaborador_departamento_idx ON colaborador (departamento_id);

CREATE TABLE usuario (
    id                      UUID PRIMARY KEY,
    colaborador_id          UUID         NOT NULL UNIQUE REFERENCES colaborador (id),
    rol                     VARCHAR(3)   NOT NULL CHECK (rol IN ('CA', 'AHR', 'ADM')),
    activo                  BOOLEAN      NOT NULL DEFAULT TRUE,
    password_hash           VARCHAR(255) NOT NULL,
    intentos_fallidos       INTEGER      NOT NULL DEFAULT 0 CHECK (intentos_fallidos >= 0),
    bloqueado_hasta         TIMESTAMPTZ,
    debe_cambiar_contrasena BOOLEAN      NOT NULL DEFAULT TRUE,
    ultimo_acceso           TIMESTAMPTZ
);

CREATE TABLE auditoria_login (
    id               UUID PRIMARY KEY,
    -- Nulo cuando el correo intentado no existe
    usuario_id       UUID REFERENCES usuario (id),
    correo_intentado VARCHAR(254) NOT NULL,
    exitoso          BOOLEAN      NOT NULL,
    motivo           VARCHAR(30)  NOT NULL CHECK (motivo IN (
        'EXITOSO', 'USUARIO_NO_EXISTE', 'CONTRASENA_INCORRECTA', 'CUENTA_BLOQUEADA',
        'USUARIO_INACTIVO', 'COLABORADOR_INACTIVO', 'LIMITE_IP')),
    ip               VARCHAR(45)  NOT NULL,
    user_agent       VARCHAR(512),
    creado_en        TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE INDEX auditoria_login_usuario_idx ON auditoria_login (usuario_id, creado_en);
CREATE INDEX auditoria_login_creado_idx ON auditoria_login (creado_en);
