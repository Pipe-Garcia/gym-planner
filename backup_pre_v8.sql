--
-- PostgreSQL database dump
--

\restrict ISWahcvkbC8QGbWcbbqI5e4r162PE2zonSEbHhcRjpNgKqQ5Fjyv5zMpYoUK1R7

-- Dumped from database version 16.13
-- Dumped by pg_dump version 16.13

SET statement_timeout = 0;
SET lock_timeout = 0;
SET idle_in_transaction_session_timeout = 0;
SET client_encoding = 'UTF8';
SET standard_conforming_strings = on;
SELECT pg_catalog.set_config('search_path', '', false);
SET check_function_bodies = false;
SET xmloption = content;
SET client_min_messages = warning;
SET row_security = off;

SET default_tablespace = '';

SET default_table_access_method = heap;

--
-- Name: exercise_tag_assignments; Type: TABLE; Schema: public; Owner: gym_user
--

CREATE TABLE public.exercise_tag_assignments (
    exercise_id bigint NOT NULL,
    tag_id bigint NOT NULL
);


ALTER TABLE public.exercise_tag_assignments OWNER TO gym_user;

--
-- Name: exercise_tags; Type: TABLE; Schema: public; Owner: gym_user
--

CREATE TABLE public.exercise_tags (
    id bigint NOT NULL,
    gym_id bigint NOT NULL,
    type character varying(30) NOT NULL,
    name character varying(100) NOT NULL,
    slug character varying(100) NOT NULL,
    CONSTRAINT exercise_tags_type_check CHECK (((type)::text = ANY ((ARRAY['BODY_AREA'::character varying, 'MUSCLE_GROUP'::character varying, 'MOVEMENT_PATTERN'::character varying, 'OBJECTIVE'::character varying, 'LEVEL'::character varying, 'EQUIPMENT'::character varying])::text[])))
);


ALTER TABLE public.exercise_tags OWNER TO gym_user;

--
-- Name: exercise_tags_id_seq; Type: SEQUENCE; Schema: public; Owner: gym_user
--

CREATE SEQUENCE public.exercise_tags_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE public.exercise_tags_id_seq OWNER TO gym_user;

--
-- Name: exercise_tags_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: gym_user
--

ALTER SEQUENCE public.exercise_tags_id_seq OWNED BY public.exercise_tags.id;


--
-- Name: exercises; Type: TABLE; Schema: public; Owner: gym_user
--

CREATE TABLE public.exercises (
    id bigint NOT NULL,
    gym_id bigint NOT NULL,
    name character varying(150) NOT NULL,
    slug character varying(150) NOT NULL,
    description text,
    technical_notes text,
    default_measurement character varying(30) DEFAULT 'REPS_WEIGHT'::character varying NOT NULL,
    video_url character varying(500),
    image_url character varying(500),
    active boolean DEFAULT true NOT NULL,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    updated_at timestamp with time zone DEFAULT now() NOT NULL,
    CONSTRAINT exercises_default_measurement_check CHECK (((default_measurement)::text = ANY ((ARRAY['REPS_WEIGHT'::character varying, 'REPS_ONLY'::character varying, 'TIME'::character varying, 'DISTANCE'::character varying, 'CIRCUIT_REPS'::character varying])::text[])))
);


ALTER TABLE public.exercises OWNER TO gym_user;

--
-- Name: exercises_id_seq; Type: SEQUENCE; Schema: public; Owner: gym_user
--

CREATE SEQUENCE public.exercises_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE public.exercises_id_seq OWNER TO gym_user;

--
-- Name: exercises_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: gym_user
--

ALTER SEQUENCE public.exercises_id_seq OWNED BY public.exercises.id;


--
-- Name: flyway_schema_history; Type: TABLE; Schema: public; Owner: gym_user
--

CREATE TABLE public.flyway_schema_history (
    installed_rank integer NOT NULL,
    version character varying(50),
    description character varying(200) NOT NULL,
    type character varying(20) NOT NULL,
    script character varying(1000) NOT NULL,
    checksum integer,
    installed_by character varying(100) NOT NULL,
    installed_on timestamp without time zone DEFAULT now() NOT NULL,
    execution_time integer NOT NULL,
    success boolean NOT NULL
);


ALTER TABLE public.flyway_schema_history OWNER TO gym_user;

--
-- Name: gyms; Type: TABLE; Schema: public; Owner: gym_user
--

CREATE TABLE public.gyms (
    id bigint NOT NULL,
    name character varying(150) NOT NULL,
    owner_name character varying(150),
    phone character varying(50),
    email character varying(150),
    address character varying(255),
    logo_url character varying(500),
    primary_color character varying(7),
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    updated_at timestamp with time zone DEFAULT now() NOT NULL
);


ALTER TABLE public.gyms OWNER TO gym_user;

--
-- Name: gyms_id_seq; Type: SEQUENCE; Schema: public; Owner: gym_user
--

CREATE SEQUENCE public.gyms_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE public.gyms_id_seq OWNER TO gym_user;

--
-- Name: gyms_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: gym_user
--

ALTER SEQUENCE public.gyms_id_seq OWNED BY public.gyms.id;


--
-- Name: routine_blocks; Type: TABLE; Schema: public; Owner: gym_user
--

CREATE TABLE public.routine_blocks (
    id bigint NOT NULL,
    routine_id bigint NOT NULL,
    order_index integer NOT NULL,
    title character varying(150) NOT NULL,
    structural_type character varying(30) NOT NULL,
    purpose character varying(30),
    total_duration_seconds integer,
    target_rounds integer,
    block_notes text,
    CONSTRAINT routine_blocks_purpose_check CHECK (((purpose)::text = ANY ((ARRAY['WARMUP'::character varying, 'ACTIVATION'::character varying, 'MAIN_LIFT'::character varying, 'ACCESSORY'::character varying, 'CONDITIONING'::character varying, 'CORE'::character varying, 'COOLDOWN'::character varying, 'OTHER'::character varying])::text[]))),
    CONSTRAINT routine_blocks_structural_type_check CHECK (((structural_type)::text = ANY ((ARRAY['STANDARD'::character varying, 'CIRCUIT'::character varying, 'PYRAMID'::character varying, 'REVERSE_PYRAMID'::character varying, 'DROP_SET'::character varying, 'REST_PAUSE'::character varying, 'CLUSTER'::character varying])::text[])))
);


ALTER TABLE public.routine_blocks OWNER TO gym_user;

--
-- Name: routine_blocks_id_seq; Type: SEQUENCE; Schema: public; Owner: gym_user
--

CREATE SEQUENCE public.routine_blocks_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE public.routine_blocks_id_seq OWNER TO gym_user;

--
-- Name: routine_blocks_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: gym_user
--

ALTER SEQUENCE public.routine_blocks_id_seq OWNED BY public.routine_blocks.id;


--
-- Name: routine_exercise_sets; Type: TABLE; Schema: public; Owner: gym_user
--

CREATE TABLE public.routine_exercise_sets (
    id bigint NOT NULL,
    routine_exercise_id bigint NOT NULL,
    set_number integer NOT NULL,
    set_kind character varying(30) DEFAULT 'NORMAL'::character varying NOT NULL,
    target_reps integer,
    target_reps_min integer,
    target_reps_max integer,
    target_weight_kg numeric(6,2),
    target_time_seconds integer,
    target_distance_meters numeric(7,2),
    rest_after_seconds integer,
    tempo character varying(20),
    rpe integer,
    notes text,
    to_failure boolean DEFAULT false NOT NULL,
    CONSTRAINT routine_exercise_sets_rpe_check CHECK (((rpe >= 1) AND (rpe <= 10))),
    CONSTRAINT routine_exercise_sets_set_kind_check CHECK (((set_kind)::text = ANY ((ARRAY['NORMAL'::character varying, 'WARMUP'::character varying, 'FAILURE'::character varying, 'DROP'::character varying, 'REST_PAUSE_PORTION'::character varying])::text[])))
);


ALTER TABLE public.routine_exercise_sets OWNER TO gym_user;

--
-- Name: routine_exercise_sets_id_seq; Type: SEQUENCE; Schema: public; Owner: gym_user
--

CREATE SEQUENCE public.routine_exercise_sets_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE public.routine_exercise_sets_id_seq OWNER TO gym_user;

--
-- Name: routine_exercise_sets_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: gym_user
--

ALTER SEQUENCE public.routine_exercise_sets_id_seq OWNED BY public.routine_exercise_sets.id;


--
-- Name: routine_exercises; Type: TABLE; Schema: public; Owner: gym_user
--

CREATE TABLE public.routine_exercises (
    id bigint NOT NULL,
    block_id bigint NOT NULL,
    exercise_id bigint NOT NULL,
    order_index integer NOT NULL,
    exercise_notes text
);


ALTER TABLE public.routine_exercises OWNER TO gym_user;

--
-- Name: routine_exercises_id_seq; Type: SEQUENCE; Schema: public; Owner: gym_user
--

CREATE SEQUENCE public.routine_exercises_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE public.routine_exercises_id_seq OWNER TO gym_user;

--
-- Name: routine_exercises_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: gym_user
--

ALTER SEQUENCE public.routine_exercises_id_seq OWNED BY public.routine_exercises.id;


--
-- Name: routines; Type: TABLE; Schema: public; Owner: gym_user
--

CREATE TABLE public.routines (
    id bigint NOT NULL,
    student_id bigint NOT NULL,
    name character varying(150) NOT NULL,
    objective character varying(150),
    source_template_id bigint,
    status character varying(20) DEFAULT 'ACTIVE'::character varying NOT NULL,
    assigned_date date NOT NULL,
    finished_date date,
    general_notes text,
    internal_notes text,
    created_by_user_id bigint NOT NULL,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    updated_at timestamp with time zone DEFAULT now() NOT NULL,
    CONSTRAINT routines_status_check CHECK (((status)::text = ANY ((ARRAY['ACTIVE'::character varying, 'FINISHED'::character varying, 'ARCHIVED'::character varying, 'DRAFT'::character varying])::text[])))
);


ALTER TABLE public.routines OWNER TO gym_user;

--
-- Name: routines_id_seq; Type: SEQUENCE; Schema: public; Owner: gym_user
--

CREATE SEQUENCE public.routines_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE public.routines_id_seq OWNER TO gym_user;

--
-- Name: routines_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: gym_user
--

ALTER SEQUENCE public.routines_id_seq OWNED BY public.routines.id;


--
-- Name: student_injuries; Type: TABLE; Schema: public; Owner: gym_user
--

CREATE TABLE public.student_injuries (
    id bigint NOT NULL,
    student_id bigint NOT NULL,
    body_area character varying(100) NOT NULL,
    description text NOT NULL,
    severity character varying(20) NOT NULL,
    started_at date,
    resolved_at date,
    active boolean DEFAULT true NOT NULL,
    notes text,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    updated_at timestamp with time zone DEFAULT now() NOT NULL,
    CONSTRAINT student_injuries_severity_check CHECK (((severity)::text = ANY ((ARRAY['LEVE'::character varying, 'MODERADA'::character varying, 'GRAVE'::character varying])::text[])))
);


ALTER TABLE public.student_injuries OWNER TO gym_user;

--
-- Name: student_injuries_id_seq; Type: SEQUENCE; Schema: public; Owner: gym_user
--

CREATE SEQUENCE public.student_injuries_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE public.student_injuries_id_seq OWNER TO gym_user;

--
-- Name: student_injuries_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: gym_user
--

ALTER SEQUENCE public.student_injuries_id_seq OWNED BY public.student_injuries.id;


--
-- Name: student_notes; Type: TABLE; Schema: public; Owner: gym_user
--

CREATE TABLE public.student_notes (
    id bigint NOT NULL,
    student_id bigint NOT NULL,
    author_user_id bigint NOT NULL,
    content text NOT NULL,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    updated_at timestamp with time zone DEFAULT now() NOT NULL
);


ALTER TABLE public.student_notes OWNER TO gym_user;

--
-- Name: student_notes_id_seq; Type: SEQUENCE; Schema: public; Owner: gym_user
--

CREATE SEQUENCE public.student_notes_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE public.student_notes_id_seq OWNER TO gym_user;

--
-- Name: student_notes_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: gym_user
--

ALTER SEQUENCE public.student_notes_id_seq OWNED BY public.student_notes.id;


--
-- Name: students; Type: TABLE; Schema: public; Owner: gym_user
--

CREATE TABLE public.students (
    id bigint NOT NULL,
    gym_id bigint NOT NULL,
    first_name character varying(100) NOT NULL,
    last_name character varying(100) NOT NULL,
    document_id character varying(50),
    phone character varying(50),
    email character varying(150),
    birth_date date,
    sport character varying(100),
    objective character varying(150),
    level character varying(50),
    general_notes text,
    active boolean DEFAULT true NOT NULL,
    started_at date,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    updated_at timestamp with time zone DEFAULT now() NOT NULL
);


ALTER TABLE public.students OWNER TO gym_user;

--
-- Name: students_id_seq; Type: SEQUENCE; Schema: public; Owner: gym_user
--

CREATE SEQUENCE public.students_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE public.students_id_seq OWNER TO gym_user;

--
-- Name: students_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: gym_user
--

ALTER SEQUENCE public.students_id_seq OWNED BY public.students.id;


--
-- Name: template_blocks; Type: TABLE; Schema: public; Owner: gym_user
--

CREATE TABLE public.template_blocks (
    id bigint NOT NULL,
    template_id bigint NOT NULL,
    order_index integer NOT NULL,
    title character varying(150) NOT NULL,
    structural_type character varying(30) NOT NULL,
    purpose character varying(30),
    total_duration_seconds integer,
    target_rounds integer,
    block_notes text,
    CONSTRAINT template_blocks_purpose_check CHECK (((purpose)::text = ANY ((ARRAY['WARMUP'::character varying, 'ACTIVATION'::character varying, 'MAIN_LIFT'::character varying, 'ACCESSORY'::character varying, 'CONDITIONING'::character varying, 'CORE'::character varying, 'COOLDOWN'::character varying, 'OTHER'::character varying])::text[]))),
    CONSTRAINT template_blocks_structural_type_check CHECK (((structural_type)::text = ANY ((ARRAY['STANDARD'::character varying, 'CIRCUIT'::character varying, 'PYRAMID'::character varying, 'REVERSE_PYRAMID'::character varying, 'DROP_SET'::character varying, 'REST_PAUSE'::character varying, 'CLUSTER'::character varying])::text[])))
);


ALTER TABLE public.template_blocks OWNER TO gym_user;

--
-- Name: template_blocks_id_seq; Type: SEQUENCE; Schema: public; Owner: gym_user
--

CREATE SEQUENCE public.template_blocks_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE public.template_blocks_id_seq OWNER TO gym_user;

--
-- Name: template_blocks_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: gym_user
--

ALTER SEQUENCE public.template_blocks_id_seq OWNED BY public.template_blocks.id;


--
-- Name: template_exercise_sets; Type: TABLE; Schema: public; Owner: gym_user
--

CREATE TABLE public.template_exercise_sets (
    id bigint NOT NULL,
    template_exercise_id bigint NOT NULL,
    set_number integer NOT NULL,
    set_kind character varying(30) DEFAULT 'NORMAL'::character varying NOT NULL,
    target_reps integer,
    target_reps_min integer,
    target_reps_max integer,
    target_weight_kg numeric(6,2),
    target_time_seconds integer,
    target_distance_meters numeric(7,2),
    rest_after_seconds integer,
    tempo character varying(20),
    rpe integer,
    notes text,
    to_failure boolean DEFAULT false NOT NULL,
    CONSTRAINT template_exercise_sets_rpe_check CHECK (((rpe >= 1) AND (rpe <= 10))),
    CONSTRAINT template_exercise_sets_set_kind_check CHECK (((set_kind)::text = ANY ((ARRAY['NORMAL'::character varying, 'WARMUP'::character varying, 'FAILURE'::character varying, 'DROP'::character varying, 'REST_PAUSE_PORTION'::character varying])::text[])))
);


ALTER TABLE public.template_exercise_sets OWNER TO gym_user;

--
-- Name: template_exercise_sets_id_seq; Type: SEQUENCE; Schema: public; Owner: gym_user
--

CREATE SEQUENCE public.template_exercise_sets_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE public.template_exercise_sets_id_seq OWNER TO gym_user;

--
-- Name: template_exercise_sets_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: gym_user
--

ALTER SEQUENCE public.template_exercise_sets_id_seq OWNED BY public.template_exercise_sets.id;


--
-- Name: template_exercises; Type: TABLE; Schema: public; Owner: gym_user
--

CREATE TABLE public.template_exercises (
    id bigint NOT NULL,
    block_id bigint NOT NULL,
    exercise_id bigint NOT NULL,
    order_index integer NOT NULL,
    exercise_notes text
);


ALTER TABLE public.template_exercises OWNER TO gym_user;

--
-- Name: template_exercises_id_seq; Type: SEQUENCE; Schema: public; Owner: gym_user
--

CREATE SEQUENCE public.template_exercises_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE public.template_exercises_id_seq OWNER TO gym_user;

--
-- Name: template_exercises_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: gym_user
--

ALTER SEQUENCE public.template_exercises_id_seq OWNED BY public.template_exercises.id;


--
-- Name: training_templates; Type: TABLE; Schema: public; Owner: gym_user
--

CREATE TABLE public.training_templates (
    id bigint NOT NULL,
    gym_id bigint NOT NULL,
    name character varying(150) NOT NULL,
    description text,
    sport character varying(100),
    objective character varying(150),
    level character varying(50),
    estimated_duration_minutes integer,
    general_notes text,
    active boolean DEFAULT true NOT NULL,
    created_by_user_id bigint NOT NULL,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    updated_at timestamp with time zone DEFAULT now() NOT NULL
);


ALTER TABLE public.training_templates OWNER TO gym_user;

--
-- Name: training_templates_id_seq; Type: SEQUENCE; Schema: public; Owner: gym_user
--

CREATE SEQUENCE public.training_templates_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE public.training_templates_id_seq OWNER TO gym_user;

--
-- Name: training_templates_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: gym_user
--

ALTER SEQUENCE public.training_templates_id_seq OWNED BY public.training_templates.id;


--
-- Name: users; Type: TABLE; Schema: public; Owner: gym_user
--

CREATE TABLE public.users (
    id bigint NOT NULL,
    gym_id bigint NOT NULL,
    email character varying(150) NOT NULL,
    password_hash character varying(255) NOT NULL,
    full_name character varying(150) NOT NULL,
    role character varying(20) NOT NULL,
    active boolean DEFAULT true NOT NULL,
    last_login_at timestamp with time zone,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    updated_at timestamp with time zone DEFAULT now() NOT NULL,
    CONSTRAINT users_role_check CHECK (((role)::text = ANY ((ARRAY['OWNER'::character varying, 'TRAINER'::character varying])::text[])))
);


ALTER TABLE public.users OWNER TO gym_user;

--
-- Name: users_id_seq; Type: SEQUENCE; Schema: public; Owner: gym_user
--

CREATE SEQUENCE public.users_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE public.users_id_seq OWNER TO gym_user;

--
-- Name: users_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: gym_user
--

ALTER SEQUENCE public.users_id_seq OWNED BY public.users.id;


--
-- Name: exercise_tags id; Type: DEFAULT; Schema: public; Owner: gym_user
--

ALTER TABLE ONLY public.exercise_tags ALTER COLUMN id SET DEFAULT nextval('public.exercise_tags_id_seq'::regclass);


--
-- Name: exercises id; Type: DEFAULT; Schema: public; Owner: gym_user
--

ALTER TABLE ONLY public.exercises ALTER COLUMN id SET DEFAULT nextval('public.exercises_id_seq'::regclass);


--
-- Name: gyms id; Type: DEFAULT; Schema: public; Owner: gym_user
--

ALTER TABLE ONLY public.gyms ALTER COLUMN id SET DEFAULT nextval('public.gyms_id_seq'::regclass);


--
-- Name: routine_blocks id; Type: DEFAULT; Schema: public; Owner: gym_user
--

ALTER TABLE ONLY public.routine_blocks ALTER COLUMN id SET DEFAULT nextval('public.routine_blocks_id_seq'::regclass);


--
-- Name: routine_exercise_sets id; Type: DEFAULT; Schema: public; Owner: gym_user
--

ALTER TABLE ONLY public.routine_exercise_sets ALTER COLUMN id SET DEFAULT nextval('public.routine_exercise_sets_id_seq'::regclass);


--
-- Name: routine_exercises id; Type: DEFAULT; Schema: public; Owner: gym_user
--

ALTER TABLE ONLY public.routine_exercises ALTER COLUMN id SET DEFAULT nextval('public.routine_exercises_id_seq'::regclass);


--
-- Name: routines id; Type: DEFAULT; Schema: public; Owner: gym_user
--

ALTER TABLE ONLY public.routines ALTER COLUMN id SET DEFAULT nextval('public.routines_id_seq'::regclass);


--
-- Name: student_injuries id; Type: DEFAULT; Schema: public; Owner: gym_user
--

ALTER TABLE ONLY public.student_injuries ALTER COLUMN id SET DEFAULT nextval('public.student_injuries_id_seq'::regclass);


--
-- Name: student_notes id; Type: DEFAULT; Schema: public; Owner: gym_user
--

ALTER TABLE ONLY public.student_notes ALTER COLUMN id SET DEFAULT nextval('public.student_notes_id_seq'::regclass);


--
-- Name: students id; Type: DEFAULT; Schema: public; Owner: gym_user
--

ALTER TABLE ONLY public.students ALTER COLUMN id SET DEFAULT nextval('public.students_id_seq'::regclass);


--
-- Name: template_blocks id; Type: DEFAULT; Schema: public; Owner: gym_user
--

ALTER TABLE ONLY public.template_blocks ALTER COLUMN id SET DEFAULT nextval('public.template_blocks_id_seq'::regclass);


--
-- Name: template_exercise_sets id; Type: DEFAULT; Schema: public; Owner: gym_user
--

ALTER TABLE ONLY public.template_exercise_sets ALTER COLUMN id SET DEFAULT nextval('public.template_exercise_sets_id_seq'::regclass);


--
-- Name: template_exercises id; Type: DEFAULT; Schema: public; Owner: gym_user
--

ALTER TABLE ONLY public.template_exercises ALTER COLUMN id SET DEFAULT nextval('public.template_exercises_id_seq'::regclass);


--
-- Name: training_templates id; Type: DEFAULT; Schema: public; Owner: gym_user
--

ALTER TABLE ONLY public.training_templates ALTER COLUMN id SET DEFAULT nextval('public.training_templates_id_seq'::regclass);


--
-- Name: users id; Type: DEFAULT; Schema: public; Owner: gym_user
--

ALTER TABLE ONLY public.users ALTER COLUMN id SET DEFAULT nextval('public.users_id_seq'::regclass);


--
-- Data for Name: exercise_tag_assignments; Type: TABLE DATA; Schema: public; Owner: gym_user
--

COPY public.exercise_tag_assignments (exercise_id, tag_id) FROM stdin;
1	11
1	48
1	3
1	13
1	45
1	24
1	32
2	2
2	16
2	31
2	42
2	44
2	47
3	3
3	30
3	34
3	43
3	45
3	55
4	1
4	6
4	32
4	45
4	48
5	52
5	11
5	33
5	3
5	44
5	32
\.


--
-- Data for Name: exercise_tags; Type: TABLE DATA; Schema: public; Owner: gym_user
--

COPY public.exercise_tags (id, gym_id, type, name, slug) FROM stdin;
1	1	BODY_AREA	Tren superior	tren-superior
2	1	BODY_AREA	Zona media	zona-media
3	1	BODY_AREA	Tren inferior	tren-inferior
4	1	BODY_AREA	Cuerpo completo	cuerpo-completo
5	1	MUSCLE_GROUP	Pecho	pecho
6	1	MUSCLE_GROUP	Espalda	espalda
7	1	MUSCLE_GROUP	Hombros	hombros
8	1	MUSCLE_GROUP	Bíceps	biceps
9	1	MUSCLE_GROUP	Tríceps	triceps
10	1	MUSCLE_GROUP	Antebrazos	antebrazos
11	1	MUSCLE_GROUP	Cuádriceps	cuadriceps
12	1	MUSCLE_GROUP	Isquiotibiales	isquiotibiales
13	1	MUSCLE_GROUP	Glúteos	gluteos
14	1	MUSCLE_GROUP	Aductores	aductores
15	1	MUSCLE_GROUP	Gemelos	gemelos
16	1	MUSCLE_GROUP	Core	core
17	1	MUSCLE_GROUP	Lumbares	lumbares
18	1	MUSCLE_GROUP	Oblicuos	oblicuos
19	1	MUSCLE_GROUP	Cuello	cuello
20	1	MOVEMENT_PATTERN	Empuje vertical	empuje-vertical
21	1	MOVEMENT_PATTERN	Empuje horizontal	empuje-horizontal
22	1	MOVEMENT_PATTERN	Tirón vertical	tiron-vertical
23	1	MOVEMENT_PATTERN	Tirón horizontal	tiron-horizontal
24	1	MOVEMENT_PATTERN	Sentadilla	sentadilla
25	1	MOVEMENT_PATTERN	Bisagra de cadera	bisagra-de-cadera
26	1	MOVEMENT_PATTERN	Zancada	zancada
27	1	MOVEMENT_PATTERN	Rotación	rotacion
28	1	MOVEMENT_PATTERN	Anti-rotación	anti-rotacion
29	1	MOVEMENT_PATTERN	Locomoción	locomocion
30	1	MOVEMENT_PATTERN	Salto	salto
31	1	MOVEMENT_PATTERN	Isométrico	isometrico
32	1	OBJECTIVE	Fuerza	fuerza
33	1	OBJECTIVE	Hipertrofia	hipertrofia
34	1	OBJECTIVE	Potencia	potencia
35	1	OBJECTIVE	Resistencia muscular	resistencia-muscular
36	1	OBJECTIVE	Resistencia cardiovascular	resistencia-cardiovascular
37	1	OBJECTIVE	Movilidad	movilidad
38	1	OBJECTIVE	Flexibilidad	flexibilidad
39	1	OBJECTIVE	Prevención	prevencion
40	1	OBJECTIVE	Rehabilitación	rehabilitacion
41	1	OBJECTIVE	Técnica	tecnica
42	1	OBJECTIVE	Activación	activacion
43	1	OBJECTIVE	Coordinación	coordinacion
44	1	LEVEL	Iniciación	iniciacion
45	1	LEVEL	Intermedio	intermedio
46	1	LEVEL	Avanzado	avanzado
47	1	EQUIPMENT	Peso corporal	peso-corporal
48	1	EQUIPMENT	Mancuernas	mancuernas
49	1	EQUIPMENT	Barra olímpica	barra-olimpica
50	1	EQUIPMENT	Barra Z	barra-z
51	1	EQUIPMENT	Polea	polea
52	1	EQUIPMENT	Máquina guiada	maquina-guiada
53	1	EQUIPMENT	Banda elástica	banda-elastica
54	1	EQUIPMENT	Kettlebell	kettlebell
55	1	EQUIPMENT	Cajón	cajon
56	1	EQUIPMENT	Soga (battle ropes)	soga-battle-ropes
57	1	EQUIPMENT	TRX	trx
58	1	EQUIPMENT	Bicicleta	bicicleta
59	1	EQUIPMENT	Elíptica	eliptica
60	1	EQUIPMENT	Cinta	cinta
61	1	EQUIPMENT	Remo	remo
62	1	EQUIPMENT	Disco	disco
63	1	EQUIPMENT	Slam ball	slam-ball
64	1	EQUIPMENT	Medicine ball	medicine-ball
65	1	EQUIPMENT	Bosu	bosu
66	1	EQUIPMENT	Fitball	fitball
67	1	OBJECTIVE	Estabilidad	estabilidad
\.


--
-- Data for Name: exercises; Type: TABLE DATA; Schema: public; Owner: gym_user
--

COPY public.exercises (id, gym_id, name, slug, description, technical_notes, default_measurement, video_url, image_url, active, created_at, updated_at) FROM stdin;
1	1	Sentadilla Goblet	sentadilla-goblet	Variante de sentadilla con mancuerna.	Mantener columna neutra y rodillas alineadas.	REPS_WEIGHT	\N	\N	t	2026-05-08 19:32:38.880079+00	2026-05-08 19:34:33.017419+00
2	1	Plancha frontal	plancha-frontal	Ejercicio isométrico de zona media.	Mantener cadera alineada y abdomen activo.	TIME	\N	\N	t	2026-05-09 20:09:15.797737+00	2026-05-09 20:09:15.797737+00
3	1	Saltos al cajón	saltos-al-cajon	Trabajo pliométrico para potencia de tren inferior.	Aterrizar suave y controlar rodillas.	REPS_ONLY	\N	\N	t	2026-05-09 20:12:00.07699+00	2026-05-09 20:12:00.07699+00
4	1	Remo con mancuerna	remo-con-mancuerna	Trabajo unilateral de espalda.	Evitar rotación del tronco.	REPS_WEIGHT	\N	\N	t	2026-05-09 20:13:28.379171+00	2026-05-09 20:13:28.379171+00
5	1	Extensión de piernas	extension-de-piernas	Llevar las piernas hasta la maxima extensión	\N	REPS_WEIGHT	\N	\N	t	2026-05-09 21:35:05.379898+00	2026-05-09 21:35:45.368195+00
\.


--
-- Data for Name: flyway_schema_history; Type: TABLE DATA; Schema: public; Owner: gym_user
--

COPY public.flyway_schema_history (installed_rank, version, description, type, script, checksum, installed_by, installed_on, execution_time, success) FROM stdin;
1	1	create gym and users	SQL	V1__create_gym_and_users.sql	207851663	gym_user	2026-05-08 00:50:24.650233	36	t
2	2	seed initial gym and owner	SQL	V2__seed_initial_gym_and_owner.sql	42966417	gym_user	2026-05-08 00:50:24.711071	10	t
3	3	create students and injuries	SQL	V3__create_students_and_injuries.sql	1437617029	gym_user	2026-05-08 16:15:48.608415	73	t
4	4	create exercises and tags	SQL	V4__create_exercises_and_tags.sql	-750082331	gym_user	2026-05-08 16:15:48.708917	39	t
5	5	seed default tags	SQL	V5__seed_default_tags.sql	97407018	gym_user	2026-05-08 16:15:48.772478	8	t
6	6	create templates and routines	SQL	V6__create_templates_and_routines.sql	-1616965401	gym_user	2026-05-09 16:56:21.229934	135	t
7	7	add missing tags	SQL	V7__add_missing_tags.sql	-8935522	gym_user	2026-05-10 00:46:26.050367	22	t
\.


--
-- Data for Name: gyms; Type: TABLE DATA; Schema: public; Owner: gym_user
--

COPY public.gyms (id, name, owner_name, phone, email, address, logo_url, primary_color, created_at, updated_at) FROM stdin;
1	Gym Planner Demo	Sergio Carrión	+54 9 3491 432123	admin@gymplanner.local	Independencia 2876	\N	#2563EB	2026-05-08 03:50:24.718623+00	2026-05-08 04:14:54.44714+00
\.


--
-- Data for Name: routine_blocks; Type: TABLE DATA; Schema: public; Owner: gym_user
--

COPY public.routine_blocks (id, routine_id, order_index, title, structural_type, purpose, total_duration_seconds, target_rounds, block_notes) FROM stdin;
1	1	1	Calentamiento	STANDARD	ACTIVATION	\N	\N	\N
2	1	2	Parte principal	STANDARD	MAIN_LIFT	\N	\N	\N
3	1	3	Vuelta a la calma	STANDARD	COOLDOWN	\N	\N	\N
4	1	4	Circuito Metabólico	CIRCUIT	MAIN_LIFT	720	6	\N
9	3	1	Calentamiento	STANDARD	ACTIVATION	\N	\N	\N
10	3	2	Parte principal	STANDARD	MAIN_LIFT	\N	\N	\N
11	3	3	Circuito Metabólico	CIRCUIT	MAIN_LIFT	720	6	\N
12	3	4	Vuelta a la calma	STANDARD	COOLDOWN	\N	\N	\N
\.


--
-- Data for Name: routine_exercise_sets; Type: TABLE DATA; Schema: public; Owner: gym_user
--

COPY public.routine_exercise_sets (id, routine_exercise_id, set_number, set_kind, target_reps, target_reps_min, target_reps_max, target_weight_kg, target_time_seconds, target_distance_meters, rest_after_seconds, tempo, rpe, notes, to_failure) FROM stdin;
1	1	1	NORMAL	\N	\N	\N	\N	40	\N	60	\N	\N	\N	f
2	1	2	NORMAL	\N	\N	\N	\N	40	\N	60	\N	\N	\N	f
3	1	3	NORMAL	\N	\N	\N	\N	40	\N	60	\N	\N	\N	f
4	2	1	NORMAL	10	\N	\N	\N	\N	\N	60	\N	\N	\N	f
5	2	2	NORMAL	10	\N	\N	\N	\N	\N	60	\N	\N	\N	f
6	2	3	NORMAL	10	\N	\N	\N	\N	\N	60	\N	\N	\N	f
7	3	1	NORMAL	10	\N	\N	\N	\N	\N	60	\N	\N	\N	f
8	3	2	NORMAL	10	\N	\N	\N	\N	\N	60	\N	\N	\N	f
9	3	3	NORMAL	10	\N	\N	\N	\N	\N	60	\N	\N	\N	f
10	3	4	NORMAL	10	\N	\N	\N	\N	\N	60	\N	\N	\N	f
11	4	1	NORMAL	10	\N	\N	\N	\N	\N	60	\N	\N	\N	f
12	4	2	NORMAL	10	\N	\N	\N	\N	\N	60	\N	\N	\N	f
13	5	1	NORMAL	6	\N	\N	\N	\N	\N	60	\N	\N	\N	f
14	5	2	NORMAL	6	\N	\N	\N	\N	\N	60	\N	\N	\N	f
15	5	3	NORMAL	6	\N	\N	\N	\N	\N	60	\N	\N	\N	f
16	5	4	NORMAL	6	\N	\N	\N	\N	\N	60	\N	\N	\N	f
17	6	1	NORMAL	\N	\N	\N	\N	40	\N	60	\N	\N	\N	f
18	6	2	NORMAL	\N	\N	\N	\N	40	\N	60	\N	\N	\N	f
19	6	3	NORMAL	\N	\N	\N	\N	40	\N	60	\N	\N	\N	f
39	13	1	NORMAL	\N	\N	\N	\N	40	\N	60	\N	\N	\N	f
40	13	2	NORMAL	\N	\N	\N	\N	40	\N	60	\N	\N	\N	f
41	13	3	NORMAL	\N	\N	\N	\N	40	\N	60	\N	\N	\N	f
42	14	1	NORMAL	10	\N	\N	\N	\N	\N	60	\N	\N	\N	f
43	14	2	NORMAL	10	\N	\N	\N	\N	\N	60	\N	\N	\N	f
44	14	3	NORMAL	10	\N	\N	\N	\N	\N	60	\N	\N	\N	f
45	15	1	NORMAL	10	\N	\N	\N	\N	\N	60	\N	\N	\N	f
46	15	2	NORMAL	10	\N	\N	\N	\N	\N	60	\N	\N	\N	f
47	15	3	NORMAL	10	\N	\N	\N	\N	\N	60	\N	\N	\N	f
48	15	4	NORMAL	10	\N	\N	\N	\N	\N	60	\N	\N	\N	f
49	16	1	NORMAL	6	\N	\N	\N	\N	\N	60	\N	\N	\N	f
50	16	2	NORMAL	6	\N	\N	\N	\N	\N	60	\N	\N	\N	f
51	16	3	NORMAL	6	\N	\N	\N	\N	\N	60	\N	\N	\N	f
52	16	4	NORMAL	6	\N	\N	\N	\N	\N	60	\N	\N	\N	f
53	17	1	NORMAL	\N	\N	\N	\N	40	\N	60	\N	\N	\N	f
54	17	2	NORMAL	\N	\N	\N	\N	40	\N	60	\N	\N	\N	f
55	17	3	NORMAL	\N	\N	\N	\N	40	\N	60	\N	\N	\N	f
56	18	1	NORMAL	10	\N	\N	\N	\N	\N	60	\N	\N	\N	f
57	18	2	NORMAL	10	\N	\N	\N	\N	\N	60	\N	\N	\N	f
\.


--
-- Data for Name: routine_exercises; Type: TABLE DATA; Schema: public; Owner: gym_user
--

COPY public.routine_exercises (id, block_id, exercise_id, order_index, exercise_notes) FROM stdin;
1	1	2	1	Espalda erguida y antebrazos apoyados.
2	2	5	1	Rodilla al pecho y extender lo máximo posible.
3	2	3	2	Aterrizaje suave y salto potente.
4	3	4	1	-
5	4	1	1	\N
6	4	2	2	-
13	9	2	1	Espalda erguida y antebrazos apoyados.
14	10	5	1	Rodilla al pecho y extender lo máximo posible.
15	10	3	2	Aterrizaje suave y salto potente.
16	11	1	1	\N
17	11	2	2	-
18	12	4	1	-
\.


--
-- Data for Name: routines; Type: TABLE DATA; Schema: public; Owner: gym_user
--

COPY public.routines (id, student_id, name, objective, source_template_id, status, assigned_date, finished_date, general_notes, internal_notes, created_by_user_id, created_at, updated_at) FROM stdin;
1	1	Plantilla Fútbol	Desarrollo de potencia y prevención de lesiones	1	ACTIVE	2026-05-10	\N	\N	\N	1	2026-05-10 04:02:18.667562+00	2026-05-10 04:02:18.667562+00
3	3	Plantilla Fútbol	Desarrollo de potencia y prevención de lesiones	1	ACTIVE	2026-05-10	\N	\N	\N	1	2026-05-10 04:06:15.389909+00	2026-05-10 04:06:15.389909+00
\.


--
-- Data for Name: student_injuries; Type: TABLE DATA; Schema: public; Owner: gym_user
--

COPY public.student_injuries (id, student_id, body_area, description, severity, started_at, resolved_at, active, notes, created_at, updated_at) FROM stdin;
1	1	Isquiotibial	Desgarro en el isquiotibial	LEVE	2026-05-03	\N	t	-	2026-05-08 19:25:38.808332+00	2026-05-08 19:25:38.808332+00
\.


--
-- Data for Name: student_notes; Type: TABLE DATA; Schema: public; Owner: gym_user
--

COPY public.student_notes (id, student_id, author_user_id, content, created_at, updated_at) FROM stdin;
1	1	1	Trabajo de rehabilitación de isquiotibiales	2026-05-08 19:26:30.312293+00	2026-05-08 19:26:30.312293+00
\.


--
-- Data for Name: students; Type: TABLE DATA; Schema: public; Owner: gym_user
--

COPY public.students (id, gym_id, first_name, last_name, document_id, phone, email, birth_date, sport, objective, level, general_notes, active, started_at, created_at, updated_at) FROM stdin;
1	1	Alejo Javier	Baldi	44332233	3491 990811	alejojavierbaldi@gmail.com	2002-05-24	Futbol	Potencia y prevención de lesiones	Intermedio	Forma parte del Club Atlético Tostado	t	2026-05-08	2026-05-08 19:23:11.260732+00	2026-05-09 00:32:18.319929+00
2	1	Maria	Gonzalez	46412124	3491 422422	mariagonzalez@gmail.com	2006-03-13	Voley	Saltabilidad	Avanzado	-	t	2026-03-02	2026-05-08 22:59:12.514047+00	2026-05-09 21:15:25.997469+00
3	1	Marcos	Mantovani	44235168	3491 772277	marcossmantovanii@gmail.com	2002-10-07	Fútbol	Desarrollo de potencia y prevención de lesiones	Intermedio	\N	t	2026-04-27	2026-05-09 21:17:45.645973+00	2026-05-09 21:17:45.645973+00
\.


--
-- Data for Name: template_blocks; Type: TABLE DATA; Schema: public; Owner: gym_user
--

COPY public.template_blocks (id, template_id, order_index, title, structural_type, purpose, total_duration_seconds, target_rounds, block_notes) FROM stdin;
4	1	1	Calentamiento	STANDARD	ACTIVATION	\N	\N	\N
5	1	2	Parte principal	STANDARD	MAIN_LIFT	\N	\N	\N
6	1	3	Vuelta a la calma	STANDARD	COOLDOWN	\N	\N	\N
7	1	4	Circuito Metabólico	CIRCUIT	MAIN_LIFT	720	6	\N
\.


--
-- Data for Name: template_exercise_sets; Type: TABLE DATA; Schema: public; Owner: gym_user
--

COPY public.template_exercise_sets (id, template_exercise_id, set_number, set_kind, target_reps, target_reps_min, target_reps_max, target_weight_kg, target_time_seconds, target_distance_meters, rest_after_seconds, tempo, rpe, notes, to_failure) FROM stdin;
13	5	1	NORMAL	\N	\N	\N	\N	40	\N	60	\N	\N	\N	f
14	5	2	NORMAL	\N	\N	\N	\N	40	\N	60	\N	\N	\N	f
15	5	3	NORMAL	\N	\N	\N	\N	40	\N	60	\N	\N	\N	f
16	6	1	NORMAL	10	\N	\N	\N	\N	\N	60	\N	\N	\N	f
17	6	2	NORMAL	10	\N	\N	\N	\N	\N	60	\N	\N	\N	f
18	6	3	NORMAL	10	\N	\N	\N	\N	\N	60	\N	\N	\N	f
19	7	1	NORMAL	10	\N	\N	\N	\N	\N	60	\N	\N	\N	f
20	7	2	NORMAL	10	\N	\N	\N	\N	\N	60	\N	\N	\N	f
21	7	3	NORMAL	10	\N	\N	\N	\N	\N	60	\N	\N	\N	f
22	7	4	NORMAL	10	\N	\N	\N	\N	\N	60	\N	\N	\N	f
23	8	1	NORMAL	10	\N	\N	\N	\N	\N	60	\N	\N	\N	f
24	8	2	NORMAL	10	\N	\N	\N	\N	\N	60	\N	\N	\N	f
25	9	1	NORMAL	6	\N	\N	\N	\N	\N	60	\N	\N	\N	f
26	9	2	NORMAL	6	\N	\N	\N	\N	\N	60	\N	\N	\N	f
27	9	3	NORMAL	6	\N	\N	\N	\N	\N	60	\N	\N	\N	f
28	9	4	NORMAL	6	\N	\N	\N	\N	\N	60	\N	\N	\N	f
29	10	1	NORMAL	\N	\N	\N	\N	40	\N	60	\N	\N	\N	f
30	10	2	NORMAL	\N	\N	\N	\N	40	\N	60	\N	\N	\N	f
31	10	3	NORMAL	\N	\N	\N	\N	40	\N	60	\N	\N	\N	f
\.


--
-- Data for Name: template_exercises; Type: TABLE DATA; Schema: public; Owner: gym_user
--

COPY public.template_exercises (id, block_id, exercise_id, order_index, exercise_notes) FROM stdin;
5	4	2	1	Espalda erguida y antebrazos apoyados.
6	5	5	1	Rodilla al pecho y extender lo máximo posible.
7	5	3	2	Aterrizaje suave y salto potente.
8	6	4	1	-
9	7	1	1	\N
10	7	2	2	-
\.


--
-- Data for Name: training_templates; Type: TABLE DATA; Schema: public; Owner: gym_user
--

COPY public.training_templates (id, gym_id, name, description, sport, objective, level, estimated_duration_minutes, general_notes, active, created_by_user_id, created_at, updated_at) FROM stdin;
1	1	Plantilla Fútbol	Plantilla base para jugadores de fútbol con foco en tren inferior, core y prevención.	Fútbol	Desarrollo de potencia y prevención de lesiones	Avanzado	\N	\N	t	1	2026-05-10 03:52:11.951765+00	2026-05-10 03:52:11.951765+00
\.


--
-- Data for Name: users; Type: TABLE DATA; Schema: public; Owner: gym_user
--

COPY public.users (id, gym_id, email, password_hash, full_name, role, active, last_login_at, created_at, updated_at) FROM stdin;
1	1	admin@gymplanner.local	$2a$12$4R6qzOmnCNaUu.BZUknvQOoc3khx2pQJO32mouS8JA/nljpHelqUi	Owner Demo	OWNER	t	2026-05-09 20:05:21.064277+00	2026-05-08 03:50:24.718623+00	2026-05-09 20:05:21.130081+00
\.


--
-- Name: exercise_tags_id_seq; Type: SEQUENCE SET; Schema: public; Owner: gym_user
--

SELECT pg_catalog.setval('public.exercise_tags_id_seq', 67, true);


--
-- Name: exercises_id_seq; Type: SEQUENCE SET; Schema: public; Owner: gym_user
--

SELECT pg_catalog.setval('public.exercises_id_seq', 5, true);


--
-- Name: gyms_id_seq; Type: SEQUENCE SET; Schema: public; Owner: gym_user
--

SELECT pg_catalog.setval('public.gyms_id_seq', 1, true);


--
-- Name: routine_blocks_id_seq; Type: SEQUENCE SET; Schema: public; Owner: gym_user
--

SELECT pg_catalog.setval('public.routine_blocks_id_seq', 12, true);


--
-- Name: routine_exercise_sets_id_seq; Type: SEQUENCE SET; Schema: public; Owner: gym_user
--

SELECT pg_catalog.setval('public.routine_exercise_sets_id_seq', 57, true);


--
-- Name: routine_exercises_id_seq; Type: SEQUENCE SET; Schema: public; Owner: gym_user
--

SELECT pg_catalog.setval('public.routine_exercises_id_seq', 18, true);


--
-- Name: routines_id_seq; Type: SEQUENCE SET; Schema: public; Owner: gym_user
--

SELECT pg_catalog.setval('public.routines_id_seq', 3, true);


--
-- Name: student_injuries_id_seq; Type: SEQUENCE SET; Schema: public; Owner: gym_user
--

SELECT pg_catalog.setval('public.student_injuries_id_seq', 1, true);


--
-- Name: student_notes_id_seq; Type: SEQUENCE SET; Schema: public; Owner: gym_user
--

SELECT pg_catalog.setval('public.student_notes_id_seq', 1, true);


--
-- Name: students_id_seq; Type: SEQUENCE SET; Schema: public; Owner: gym_user
--

SELECT pg_catalog.setval('public.students_id_seq', 3, true);


--
-- Name: template_blocks_id_seq; Type: SEQUENCE SET; Schema: public; Owner: gym_user
--

SELECT pg_catalog.setval('public.template_blocks_id_seq', 7, true);


--
-- Name: template_exercise_sets_id_seq; Type: SEQUENCE SET; Schema: public; Owner: gym_user
--

SELECT pg_catalog.setval('public.template_exercise_sets_id_seq', 31, true);


--
-- Name: template_exercises_id_seq; Type: SEQUENCE SET; Schema: public; Owner: gym_user
--

SELECT pg_catalog.setval('public.template_exercises_id_seq', 10, true);


--
-- Name: training_templates_id_seq; Type: SEQUENCE SET; Schema: public; Owner: gym_user
--

SELECT pg_catalog.setval('public.training_templates_id_seq', 1, true);


--
-- Name: users_id_seq; Type: SEQUENCE SET; Schema: public; Owner: gym_user
--

SELECT pg_catalog.setval('public.users_id_seq', 1, true);


--
-- Name: exercise_tag_assignments exercise_tag_assignments_pkey; Type: CONSTRAINT; Schema: public; Owner: gym_user
--

ALTER TABLE ONLY public.exercise_tag_assignments
    ADD CONSTRAINT exercise_tag_assignments_pkey PRIMARY KEY (exercise_id, tag_id);


--
-- Name: exercise_tags exercise_tags_pkey; Type: CONSTRAINT; Schema: public; Owner: gym_user
--

ALTER TABLE ONLY public.exercise_tags
    ADD CONSTRAINT exercise_tags_pkey PRIMARY KEY (id);


--
-- Name: exercises exercises_pkey; Type: CONSTRAINT; Schema: public; Owner: gym_user
--

ALTER TABLE ONLY public.exercises
    ADD CONSTRAINT exercises_pkey PRIMARY KEY (id);


--
-- Name: flyway_schema_history flyway_schema_history_pk; Type: CONSTRAINT; Schema: public; Owner: gym_user
--

ALTER TABLE ONLY public.flyway_schema_history
    ADD CONSTRAINT flyway_schema_history_pk PRIMARY KEY (installed_rank);


--
-- Name: gyms gyms_pkey; Type: CONSTRAINT; Schema: public; Owner: gym_user
--

ALTER TABLE ONLY public.gyms
    ADD CONSTRAINT gyms_pkey PRIMARY KEY (id);


--
-- Name: routine_blocks routine_blocks_pkey; Type: CONSTRAINT; Schema: public; Owner: gym_user
--

ALTER TABLE ONLY public.routine_blocks
    ADD CONSTRAINT routine_blocks_pkey PRIMARY KEY (id);


--
-- Name: routine_exercise_sets routine_exercise_sets_pkey; Type: CONSTRAINT; Schema: public; Owner: gym_user
--

ALTER TABLE ONLY public.routine_exercise_sets
    ADD CONSTRAINT routine_exercise_sets_pkey PRIMARY KEY (id);


--
-- Name: routine_exercises routine_exercises_pkey; Type: CONSTRAINT; Schema: public; Owner: gym_user
--

ALTER TABLE ONLY public.routine_exercises
    ADD CONSTRAINT routine_exercises_pkey PRIMARY KEY (id);


--
-- Name: routines routines_pkey; Type: CONSTRAINT; Schema: public; Owner: gym_user
--

ALTER TABLE ONLY public.routines
    ADD CONSTRAINT routines_pkey PRIMARY KEY (id);


--
-- Name: student_injuries student_injuries_pkey; Type: CONSTRAINT; Schema: public; Owner: gym_user
--

ALTER TABLE ONLY public.student_injuries
    ADD CONSTRAINT student_injuries_pkey PRIMARY KEY (id);


--
-- Name: student_notes student_notes_pkey; Type: CONSTRAINT; Schema: public; Owner: gym_user
--

ALTER TABLE ONLY public.student_notes
    ADD CONSTRAINT student_notes_pkey PRIMARY KEY (id);


--
-- Name: students students_pkey; Type: CONSTRAINT; Schema: public; Owner: gym_user
--

ALTER TABLE ONLY public.students
    ADD CONSTRAINT students_pkey PRIMARY KEY (id);


--
-- Name: template_blocks template_blocks_pkey; Type: CONSTRAINT; Schema: public; Owner: gym_user
--

ALTER TABLE ONLY public.template_blocks
    ADD CONSTRAINT template_blocks_pkey PRIMARY KEY (id);


--
-- Name: template_exercise_sets template_exercise_sets_pkey; Type: CONSTRAINT; Schema: public; Owner: gym_user
--

ALTER TABLE ONLY public.template_exercise_sets
    ADD CONSTRAINT template_exercise_sets_pkey PRIMARY KEY (id);


--
-- Name: template_exercises template_exercises_pkey; Type: CONSTRAINT; Schema: public; Owner: gym_user
--

ALTER TABLE ONLY public.template_exercises
    ADD CONSTRAINT template_exercises_pkey PRIMARY KEY (id);


--
-- Name: training_templates training_templates_pkey; Type: CONSTRAINT; Schema: public; Owner: gym_user
--

ALTER TABLE ONLY public.training_templates
    ADD CONSTRAINT training_templates_pkey PRIMARY KEY (id);


--
-- Name: exercises uk_exercises_gym_slug; Type: CONSTRAINT; Schema: public; Owner: gym_user
--

ALTER TABLE ONLY public.exercises
    ADD CONSTRAINT uk_exercises_gym_slug UNIQUE (gym_id, slug);


--
-- Name: routine_blocks uk_rb_routine_order; Type: CONSTRAINT; Schema: public; Owner: gym_user
--

ALTER TABLE ONLY public.routine_blocks
    ADD CONSTRAINT uk_rb_routine_order UNIQUE (routine_id, order_index);


--
-- Name: routine_exercises uk_re_block_order; Type: CONSTRAINT; Schema: public; Owner: gym_user
--

ALTER TABLE ONLY public.routine_exercises
    ADD CONSTRAINT uk_re_block_order UNIQUE (block_id, order_index);


--
-- Name: routine_exercise_sets uk_res_re_setnum; Type: CONSTRAINT; Schema: public; Owner: gym_user
--

ALTER TABLE ONLY public.routine_exercise_sets
    ADD CONSTRAINT uk_res_re_setnum UNIQUE (routine_exercise_id, set_number);


--
-- Name: students uk_students_gym_doc; Type: CONSTRAINT; Schema: public; Owner: gym_user
--

ALTER TABLE ONLY public.students
    ADD CONSTRAINT uk_students_gym_doc UNIQUE (gym_id, document_id);


--
-- Name: exercise_tags uk_tag_gym_type_slug; Type: CONSTRAINT; Schema: public; Owner: gym_user
--

ALTER TABLE ONLY public.exercise_tags
    ADD CONSTRAINT uk_tag_gym_type_slug UNIQUE (gym_id, type, slug);


--
-- Name: template_blocks uk_tb_template_order; Type: CONSTRAINT; Schema: public; Owner: gym_user
--

ALTER TABLE ONLY public.template_blocks
    ADD CONSTRAINT uk_tb_template_order UNIQUE (template_id, order_index);


--
-- Name: template_exercises uk_te_block_order; Type: CONSTRAINT; Schema: public; Owner: gym_user
--

ALTER TABLE ONLY public.template_exercises
    ADD CONSTRAINT uk_te_block_order UNIQUE (block_id, order_index);


--
-- Name: template_exercise_sets uk_tes_te_setnum; Type: CONSTRAINT; Schema: public; Owner: gym_user
--

ALTER TABLE ONLY public.template_exercise_sets
    ADD CONSTRAINT uk_tes_te_setnum UNIQUE (template_exercise_id, set_number);


--
-- Name: users users_email_key; Type: CONSTRAINT; Schema: public; Owner: gym_user
--

ALTER TABLE ONLY public.users
    ADD CONSTRAINT users_email_key UNIQUE (email);


--
-- Name: users users_pkey; Type: CONSTRAINT; Schema: public; Owner: gym_user
--

ALTER TABLE ONLY public.users
    ADD CONSTRAINT users_pkey PRIMARY KEY (id);


--
-- Name: flyway_schema_history_s_idx; Type: INDEX; Schema: public; Owner: gym_user
--

CREATE INDEX flyway_schema_history_s_idx ON public.flyway_schema_history USING btree (success);


--
-- Name: idx_eta_tag; Type: INDEX; Schema: public; Owner: gym_user
--

CREATE INDEX idx_eta_tag ON public.exercise_tag_assignments USING btree (tag_id);


--
-- Name: idx_exercises_gym_active; Type: INDEX; Schema: public; Owner: gym_user
--

CREATE INDEX idx_exercises_gym_active ON public.exercises USING btree (gym_id, active);


--
-- Name: idx_exercises_search; Type: INDEX; Schema: public; Owner: gym_user
--

CREATE INDEX idx_exercises_search ON public.exercises USING btree (gym_id, name);


--
-- Name: idx_injuries_student; Type: INDEX; Schema: public; Owner: gym_user
--

CREATE INDEX idx_injuries_student ON public.student_injuries USING btree (student_id);


--
-- Name: idx_res_re; Type: INDEX; Schema: public; Owner: gym_user
--

CREATE INDEX idx_res_re ON public.routine_exercise_sets USING btree (routine_exercise_id, set_number);


--
-- Name: idx_routine_blocks_routine; Type: INDEX; Schema: public; Owner: gym_user
--

CREATE INDEX idx_routine_blocks_routine ON public.routine_blocks USING btree (routine_id, order_index);


--
-- Name: idx_routine_exercises_block; Type: INDEX; Schema: public; Owner: gym_user
--

CREATE INDEX idx_routine_exercises_block ON public.routine_exercises USING btree (block_id, order_index);


--
-- Name: idx_routines_status; Type: INDEX; Schema: public; Owner: gym_user
--

CREATE INDEX idx_routines_status ON public.routines USING btree (student_id, status);


--
-- Name: idx_routines_student; Type: INDEX; Schema: public; Owner: gym_user
--

CREATE INDEX idx_routines_student ON public.routines USING btree (student_id, assigned_date DESC);


--
-- Name: idx_student_notes_student; Type: INDEX; Schema: public; Owner: gym_user
--

CREATE INDEX idx_student_notes_student ON public.student_notes USING btree (student_id, created_at DESC);


--
-- Name: idx_students_active; Type: INDEX; Schema: public; Owner: gym_user
--

CREATE INDEX idx_students_active ON public.students USING btree (gym_id, active);


--
-- Name: idx_students_gym; Type: INDEX; Schema: public; Owner: gym_user
--

CREATE INDEX idx_students_gym ON public.students USING btree (gym_id);


--
-- Name: idx_students_search; Type: INDEX; Schema: public; Owner: gym_user
--

CREATE INDEX idx_students_search ON public.students USING btree (gym_id, last_name, first_name);


--
-- Name: idx_tags_type; Type: INDEX; Schema: public; Owner: gym_user
--

CREATE INDEX idx_tags_type ON public.exercise_tags USING btree (gym_id, type);


--
-- Name: idx_template_blocks_template; Type: INDEX; Schema: public; Owner: gym_user
--

CREATE INDEX idx_template_blocks_template ON public.template_blocks USING btree (template_id, order_index);


--
-- Name: idx_template_exercises_block; Type: INDEX; Schema: public; Owner: gym_user
--

CREATE INDEX idx_template_exercises_block ON public.template_exercises USING btree (block_id, order_index);


--
-- Name: idx_templates_gym_active; Type: INDEX; Schema: public; Owner: gym_user
--

CREATE INDEX idx_templates_gym_active ON public.training_templates USING btree (gym_id, active);


--
-- Name: idx_tes_te; Type: INDEX; Schema: public; Owner: gym_user
--

CREATE INDEX idx_tes_te ON public.template_exercise_sets USING btree (template_exercise_id, set_number);


--
-- Name: idx_users_gym; Type: INDEX; Schema: public; Owner: gym_user
--

CREATE INDEX idx_users_gym ON public.users USING btree (gym_id);


--
-- Name: exercise_tag_assignments exercise_tag_assignments_exercise_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: gym_user
--

ALTER TABLE ONLY public.exercise_tag_assignments
    ADD CONSTRAINT exercise_tag_assignments_exercise_id_fkey FOREIGN KEY (exercise_id) REFERENCES public.exercises(id) ON DELETE CASCADE;


--
-- Name: exercise_tag_assignments exercise_tag_assignments_tag_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: gym_user
--

ALTER TABLE ONLY public.exercise_tag_assignments
    ADD CONSTRAINT exercise_tag_assignments_tag_id_fkey FOREIGN KEY (tag_id) REFERENCES public.exercise_tags(id) ON DELETE CASCADE;


--
-- Name: exercise_tags exercise_tags_gym_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: gym_user
--

ALTER TABLE ONLY public.exercise_tags
    ADD CONSTRAINT exercise_tags_gym_id_fkey FOREIGN KEY (gym_id) REFERENCES public.gyms(id);


--
-- Name: exercises exercises_gym_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: gym_user
--

ALTER TABLE ONLY public.exercises
    ADD CONSTRAINT exercises_gym_id_fkey FOREIGN KEY (gym_id) REFERENCES public.gyms(id);


--
-- Name: routine_blocks routine_blocks_routine_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: gym_user
--

ALTER TABLE ONLY public.routine_blocks
    ADD CONSTRAINT routine_blocks_routine_id_fkey FOREIGN KEY (routine_id) REFERENCES public.routines(id) ON DELETE CASCADE;


--
-- Name: routine_exercise_sets routine_exercise_sets_routine_exercise_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: gym_user
--

ALTER TABLE ONLY public.routine_exercise_sets
    ADD CONSTRAINT routine_exercise_sets_routine_exercise_id_fkey FOREIGN KEY (routine_exercise_id) REFERENCES public.routine_exercises(id) ON DELETE CASCADE;


--
-- Name: routine_exercises routine_exercises_block_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: gym_user
--

ALTER TABLE ONLY public.routine_exercises
    ADD CONSTRAINT routine_exercises_block_id_fkey FOREIGN KEY (block_id) REFERENCES public.routine_blocks(id) ON DELETE CASCADE;


--
-- Name: routine_exercises routine_exercises_exercise_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: gym_user
--

ALTER TABLE ONLY public.routine_exercises
    ADD CONSTRAINT routine_exercises_exercise_id_fkey FOREIGN KEY (exercise_id) REFERENCES public.exercises(id);


--
-- Name: routines routines_created_by_user_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: gym_user
--

ALTER TABLE ONLY public.routines
    ADD CONSTRAINT routines_created_by_user_id_fkey FOREIGN KEY (created_by_user_id) REFERENCES public.users(id);


--
-- Name: routines routines_source_template_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: gym_user
--

ALTER TABLE ONLY public.routines
    ADD CONSTRAINT routines_source_template_id_fkey FOREIGN KEY (source_template_id) REFERENCES public.training_templates(id) ON DELETE SET NULL;


--
-- Name: routines routines_student_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: gym_user
--

ALTER TABLE ONLY public.routines
    ADD CONSTRAINT routines_student_id_fkey FOREIGN KEY (student_id) REFERENCES public.students(id);


--
-- Name: student_injuries student_injuries_student_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: gym_user
--

ALTER TABLE ONLY public.student_injuries
    ADD CONSTRAINT student_injuries_student_id_fkey FOREIGN KEY (student_id) REFERENCES public.students(id) ON DELETE CASCADE;


--
-- Name: student_notes student_notes_author_user_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: gym_user
--

ALTER TABLE ONLY public.student_notes
    ADD CONSTRAINT student_notes_author_user_id_fkey FOREIGN KEY (author_user_id) REFERENCES public.users(id);


--
-- Name: student_notes student_notes_student_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: gym_user
--

ALTER TABLE ONLY public.student_notes
    ADD CONSTRAINT student_notes_student_id_fkey FOREIGN KEY (student_id) REFERENCES public.students(id) ON DELETE CASCADE;


--
-- Name: students students_gym_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: gym_user
--

ALTER TABLE ONLY public.students
    ADD CONSTRAINT students_gym_id_fkey FOREIGN KEY (gym_id) REFERENCES public.gyms(id);


--
-- Name: template_blocks template_blocks_template_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: gym_user
--

ALTER TABLE ONLY public.template_blocks
    ADD CONSTRAINT template_blocks_template_id_fkey FOREIGN KEY (template_id) REFERENCES public.training_templates(id) ON DELETE CASCADE;


--
-- Name: template_exercise_sets template_exercise_sets_template_exercise_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: gym_user
--

ALTER TABLE ONLY public.template_exercise_sets
    ADD CONSTRAINT template_exercise_sets_template_exercise_id_fkey FOREIGN KEY (template_exercise_id) REFERENCES public.template_exercises(id) ON DELETE CASCADE;


--
-- Name: template_exercises template_exercises_block_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: gym_user
--

ALTER TABLE ONLY public.template_exercises
    ADD CONSTRAINT template_exercises_block_id_fkey FOREIGN KEY (block_id) REFERENCES public.template_blocks(id) ON DELETE CASCADE;


--
-- Name: template_exercises template_exercises_exercise_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: gym_user
--

ALTER TABLE ONLY public.template_exercises
    ADD CONSTRAINT template_exercises_exercise_id_fkey FOREIGN KEY (exercise_id) REFERENCES public.exercises(id);


--
-- Name: training_templates training_templates_created_by_user_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: gym_user
--

ALTER TABLE ONLY public.training_templates
    ADD CONSTRAINT training_templates_created_by_user_id_fkey FOREIGN KEY (created_by_user_id) REFERENCES public.users(id);


--
-- Name: training_templates training_templates_gym_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: gym_user
--

ALTER TABLE ONLY public.training_templates
    ADD CONSTRAINT training_templates_gym_id_fkey FOREIGN KEY (gym_id) REFERENCES public.gyms(id);


--
-- Name: users users_gym_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: gym_user
--

ALTER TABLE ONLY public.users
    ADD CONSTRAINT users_gym_id_fkey FOREIGN KEY (gym_id) REFERENCES public.gyms(id);


--
-- PostgreSQL database dump complete
--

\unrestrict ISWahcvkbC8QGbWcbbqI5e4r162PE2zonSEbHhcRjpNgKqQ5Fjyv5zMpYoUK1R7

