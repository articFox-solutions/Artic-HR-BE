-- Datos ficticios SOLO para desarrollo local (perfil local). Nunca se cargan en producción.
-- 25 usuarios de gestión: 15 CA, 9 AHR y 1 ADM (DERCAS 1.05, sección 5).
-- Contraseña de todos: ArticHR-Demo-2026!
-- Casos especiales para probar el frontend:
--   sofia.ramirez@artichr.test  debe cambiar su contraseña temporal (RF-08)
--   pedro.castillo@artichr.test usuario desactivado: el login siempre falla

INSERT INTO departamento (id, nombre) VALUES
    (gen_random_uuid(), 'Recursos Humanos'),
    (gen_random_uuid(), 'Finanzas'),
    (gen_random_uuid(), 'Tecnología'),
    (gen_random_uuid(), 'Operaciones'),
    (gen_random_uuid(), 'Administración');

INSERT INTO colaborador (id, codigo_empleado, nombre, correo_empresarial, puesto, departamento_id, estado)
SELECT gen_random_uuid(), d.codigo, d.nombre, d.correo, d.puesto, dep.id, 'ACTIVO'
FROM (VALUES
    ('EMP-001', 'Ana Lucía Morales',     'ana.morales@artichr.test',     'Gerente de Recursos Humanos',   'Recursos Humanos'),
    ('EMP-002', 'Luis Fernando Herrera', 'luis.herrera@artichr.test',    'Agente de Recursos Humanos',    'Recursos Humanos'),
    ('EMP-003', 'María José Cifuentes',  'maria.cifuentes@artichr.test', 'Agente de Recursos Humanos',    'Recursos Humanos'),
    ('EMP-004', 'Diego Alejandro Pérez', 'diego.perez@artichr.test',     'Agente de Recursos Humanos',    'Recursos Humanos'),
    ('EMP-005', 'Gabriela Estrada',      'gabriela.estrada@artichr.test','Agente de Reclutamiento',       'Recursos Humanos'),
    ('EMP-006', 'Javier Monterroso',     'javier.monterroso@artichr.test','Agente de Nómina',             'Recursos Humanos'),
    ('EMP-007', 'Andrea Solís',          'andrea.solis@artichr.test',    'Agente de Capacitación',        'Recursos Humanos'),
    ('EMP-008', 'Roberto Aguilar',       'roberto.aguilar@artichr.test', 'Agente de Relaciones Laborales','Recursos Humanos'),
    ('EMP-009', 'Paola Méndez',          'paola.mendez@artichr.test',    'Agente de Compensaciones',      'Recursos Humanos'),
    ('EMP-010', 'Héctor Juárez',         'hector.juarez@artichr.test',   'Agente de Recursos Humanos',    'Recursos Humanos'),
    ('EMP-011', 'Sofía Ramírez',         'sofia.ramirez@artichr.test',   'Analista Financiera',           'Finanzas'),
    ('EMP-012', 'Pedro Castillo',        'pedro.castillo@artichr.test',  'Contador General',              'Finanzas'),
    ('EMP-013', 'Lucía Barrios',         'lucia.barrios@artichr.test',   'Auxiliar Contable',             'Finanzas'),
    ('EMP-014', 'Mario Orellana',        'mario.orellana@artichr.test',  'Analista de Presupuesto',       'Finanzas'),
    ('EMP-015', 'Daniela Fuentes',       'daniela.fuentes@artichr.test', 'Desarrolladora de Software',    'Tecnología'),
    ('EMP-016', 'Kevin Mazariegos',      'kevin.mazariegos@artichr.test','Soporte Técnico',               'Tecnología'),
    ('EMP-017', 'Alejandra Ortiz',       'alejandra.ortiz@artichr.test', 'Administradora de Sistemas',    'Tecnología'),
    ('EMP-018', 'Fernando Lima',         'fernando.lima@artichr.test',   'Analista de Datos',             'Tecnología'),
    ('EMP-019', 'Valeria Chávez',        'valeria.chavez@artichr.test',  'Coordinadora de Operaciones',   'Operaciones'),
    ('EMP-020', 'Óscar Girón',           'oscar.giron@artichr.test',     'Supervisor de Logística',       'Operaciones'),
    ('EMP-021', 'Natalia Recinos',       'natalia.recinos@artichr.test', 'Analista de Procesos',          'Operaciones'),
    ('EMP-022', 'Samuel Rodas',          'samuel.rodas@artichr.test',    'Encargado de Compras',          'Operaciones'),
    ('EMP-023', 'Isabel Quiñónez',       'isabel.quinonez@artichr.test', 'Asistente Administrativa',      'Administración'),
    ('EMP-024', 'Ricardo Velásquez',     'ricardo.velasquez@artichr.test','Recepcionista',                'Administración'),
    ('EMP-025', 'Carmen Arévalo',        'carmen.arevalo@artichr.test',  'Encargada de Archivo',          'Administración')
) AS d (codigo, nombre, correo, puesto, departamento)
JOIN departamento dep ON dep.nombre = d.departamento;

INSERT INTO usuario (id, colaborador_id, rol, activo, password_hash, debe_cambiar_contrasena)
SELECT gen_random_uuid(), c.id,
       CASE
           WHEN c.codigo_empleado = 'EMP-001' THEN 'ADM'
           WHEN c.codigo_empleado BETWEEN 'EMP-002' AND 'EMP-010' THEN 'AHR'
           ELSE 'CA'
       END,
       c.codigo_empleado <> 'EMP-012',
       '$argon2id$v=19$m=19456,t=2,p=1$V/g0JGMWS7HRd4K73/OBPA$mm4A51GuBWbPxhjoTep+jEP8SYZdHqWwl8cjd5ygVUI',
       c.codigo_empleado = 'EMP-011'
FROM colaborador c;
