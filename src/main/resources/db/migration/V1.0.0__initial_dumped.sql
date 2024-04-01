--
-- PostgreSQL database dump
--
-- Dumped from database version 13.6
-- Dumped by pg_dump version 15.3
-- Started on 2024-03-30 23:35:40
--
-- TOC entry 201 (class 1259 OID 16387)
-- Name: accountability; Type: TABLE; Schema: public; Owner: -
--
CREATE TABLE public.accountability (
    child_id bigint NOT NULL,
    create_date timestamp(6) without time zone,
    id bigint NOT NULL,
    last_modified_date timestamp(6) without time zone,
    parent_id bigint NOT NULL,
    type character varying(255)
);

CREATE SEQUENCE public.accountability_id_seq START WITH 1 INCREMENT BY 1 NO MINVALUE NO MAXVALUE CACHE 1;

--
-- TOC entry 3066 (class 0 OID 0)
-- Dependencies: 200
-- Name: accountability_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--
ALTER SEQUENCE public.accountability_id_seq OWNED BY public.accountability.id;

--
-- TOC entry 203 (class 1259 OID 16396)
-- Name: event; Type: TABLE; Schema: public; Owner: -
--
CREATE TABLE public.event (
    is_a_course boolean,
    create_date timestamp(6) without time zone,
    end_time timestamp(6) with time zone,
    id bigint NOT NULL,
    last_modified_date timestamp(6) without time zone,
    organization_id bigint NOT NULL,
    start_time timestamp(6) with time zone,
    address character varying(255),
    description character varying(255),
    name character varying(255)
);

--
-- TOC entry 205 (class 1259 OID 16410)
-- Name: event_attendance; Type: TABLE; Schema: public; Owner: -
--
CREATE TABLE public.event_attendance (
    number_of_visitors integer,
    date_time timestamp(6) without time zone NOT NULL,
    id bigint NOT NULL,
    visitors_description text
);

--
-- TOC entry 202 (class 1259 OID 16394)
-- Name: event_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--
CREATE SEQUENCE public.event_id_seq START WITH 1 INCREMENT BY 1 NO MINVALUE NO MAXVALUE CACHE 1;

--
-- TOC entry 3067 (class 0 OID 0)
-- Dependencies: 202
-- Name: event_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--
ALTER SEQUENCE public.event_id_seq OWNED BY public.event.id;

--
-- TOC entry 204 (class 1259 OID 16405)
-- Name: event_speakers; Type: TABLE; Schema: public; Owner: -
--
CREATE TABLE public.event_speakers (
    event_id bigint NOT NULL,
    speakers_id bigint NOT NULL
);

CREATE TABLE public.organization (
    id bigint NOT NULL,
    type character varying(255)
);

--
-- TOC entry 208 (class 1259 OID 16426)
-- Name: party; Type: TABLE; Schema: public; Owner: -
--
CREATE TABLE public.party (
    create_date timestamp(6) without time zone,
    id bigint NOT NULL,
    last_modified_date timestamp(6) without time zone,
    address text,
    description text,
    name character varying(255),
    phone_number character varying(255)
);

--
-- TOC entry 207 (class 1259 OID 16424)
-- Name: party_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--
CREATE SEQUENCE public.party_id_seq START WITH 1 INCREMENT BY 1 NO MINVALUE NO MAXVALUE CACHE 1;

--
-- TOC entry 3068 (class 0 OID 0)
-- Dependencies: 207
-- Name: party_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--
ALTER SEQUENCE public.party_id_seq OWNED BY public.party.id;

--
-- TOC entry 209 (class 1259 OID 16435)
-- Name: person; Type: TABLE; Schema: public; Owner: -
--
CREATE TABLE public.person (
    age integer,
    birthdate date,
    has_been_baptized boolean,
    id bigint NOT NULL,
    first_name character varying(255),
    last_name character varying(255)
);

--
-- TOC entry 210 (class 1259 OID 16443)
-- Name: person_event_attendance; Type: TABLE; Schema: public; Owner: -
--
CREATE TABLE public.person_event_attendance (
    date_time timestamp(6) without time zone,
    event_attendance_id bigint NOT NULL,
    person_id bigint NOT NULL
);

--
-- TOC entry 2888 (class 2604 OID 16390)
-- Name: accountability id; Type: DEFAULT; Schema: public; Owner: -
--
ALTER TABLE
    ONLY public.accountability
ALTER COLUMN
    id
SET
    DEFAULT nextval('public.accountability_id_seq' :: regclass);

--
-- TOC entry 2889 (class 2604 OID 16399)
-- Name: event id; Type: DEFAULT; Schema: public; Owner: -
--
ALTER TABLE
    ONLY public.event
ALTER COLUMN
    id
SET
    DEFAULT nextval('public.event_id_seq' :: regclass);

--
-- TOC entry 2890 (class 2604 OID 16429)
-- Name: party id; Type: DEFAULT; Schema: public; Owner: -
--
ALTER TABLE
    ONLY public.party
ALTER COLUMN
    id
SET
    DEFAULT nextval('public.party_id_seq' :: regclass);

INSERT INTO
    public.organization (id, type)
VALUES
    (1, 'CHURCH');

INSERT INTO
    public.party (
        id,
        create_date,
        last_modified_date,
        name
    )
VALUES
    (
        1,
        NOW(),
        NOW(),
        'LCDP'
    );

SELECT
    pg_catalog.setval('public.accountability_id_seq', 1, false);

--
-- TOC entry 3070 (class 0 OID 0)
-- Dependencies: 202
-- Name: event_id_seq; Type: SEQUENCE SET; Schema: public; Owner: -
--
SELECT
    pg_catalog.setval('public.event_id_seq', 1, false);

SELECT
    pg_catalog.setval('public.party_id_seq', 2, false);

--
-- TOC entry 2894 (class 2606 OID 16393)
-- Name: accountability accountability_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--
ALTER TABLE
    ONLY public.accountability
ADD
    CONSTRAINT accountability_pkey PRIMARY KEY (id);

--
-- TOC entry 2900 (class 2606 OID 16417)
-- Name: event_attendance event_attendance_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--
ALTER TABLE
    ONLY public.event_attendance
ADD
    CONSTRAINT event_attendance_pkey PRIMARY KEY (id);

--
-- TOC entry 2896 (class 2606 OID 16404)
-- Name: event event_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--
ALTER TABLE
    ONLY public.event
ADD
    CONSTRAINT event_pkey PRIMARY KEY (id);

--
-- TOC entry 2898 (class 2606 OID 16409)
-- Name: event_speakers event_speakers_speakers_id_key; Type: CONSTRAINT; Schema: public; Owner: -
--
ALTER TABLE
    ONLY public.event_speakers
ADD
    CONSTRAINT event_speakers_speakers_id_key UNIQUE (speakers_id);

ALTER TABLE
    ONLY public.organization
ADD
    CONSTRAINT organization_pkey PRIMARY KEY (id);

--
-- TOC entry 2904 (class 2606 OID 16434)
-- Name: party party_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--
ALTER TABLE
    ONLY public.party
ADD
    CONSTRAINT party_pkey PRIMARY KEY (id);

--
-- TOC entry 2908 (class 2606 OID 16447)
-- Name: person_event_attendance person_event_attendance_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--
ALTER TABLE
    ONLY public.person_event_attendance
ADD
    CONSTRAINT person_event_attendance_pkey PRIMARY KEY (event_attendance_id, person_id);

--
-- TOC entry 2906 (class 2606 OID 16442)
-- Name: person person_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--
ALTER TABLE
    ONLY public.person
ADD
    CONSTRAINT person_pkey PRIMARY KEY (id);

--
-- TOC entry 2916 (class 2606 OID 16483)
-- Name: person fk1gqontwml1qdnt2jsh1fosomd; Type: FK CONSTRAINT; Schema: public; Owner: -
--
ALTER TABLE
    ONLY public.person
ADD
    CONSTRAINT fk1gqontwml1qdnt2jsh1fosomd FOREIGN KEY (id) REFERENCES public.party(id);

--
-- TOC entry 2912 (class 2606 OID 16468)
-- Name: event_speakers fk4mpddj4rg3nftjefml17ou0sm; Type: FK CONSTRAINT; Schema: public; Owner: -
--
ALTER TABLE
    ONLY public.event_speakers
ADD
    CONSTRAINT fk4mpddj4rg3nftjefml17ou0sm FOREIGN KEY (event_id) REFERENCES public.event(id);

--
-- TOC entry 2913 (class 2606 OID 16463)
-- Name: event_speakers fkad2vl2yymugehhjsqembv1yfa; Type: FK CONSTRAINT; Schema: public; Owner: -
--
ALTER TABLE
    ONLY public.event_speakers
ADD
    CONSTRAINT fkad2vl2yymugehhjsqembv1yfa FOREIGN KEY (speakers_id) REFERENCES public.person(id);

ALTER TABLE
    ONLY public.organization
ADD
    CONSTRAINT fkailb6lqw031h08ofia5xhhlcp FOREIGN KEY (id) REFERENCES public.party(id);

ALTER TABLE
    ONLY public.person_event_attendance
ADD
    CONSTRAINT fkaqyt36103u2nmduvrbdnil3 FOREIGN KEY (event_attendance_id) REFERENCES public.event_attendance(id);

ALTER TABLE
    ONLY public.accountability
ADD
    CONSTRAINT fkga3wdp84fsxjnhpuot9b0m0wq FOREIGN KEY (child_id) REFERENCES public.party(id);

ALTER TABLE
    ONLY public.event_attendance
ADD
    CONSTRAINT fkgsdsej7cecjt0joftcsudm761 FOREIGN KEY (id) REFERENCES public.event(id);

ALTER TABLE
    ONLY public.accountability
ADD
    CONSTRAINT fkhno51rj3c09dvdtkir9wnmqlj FOREIGN KEY (parent_id) REFERENCES public.party(id);

ALTER TABLE
    ONLY public.event
ADD
    CONSTRAINT fkkarqc3c84scr3r5ncv5stqbk2 FOREIGN KEY (organization_id) REFERENCES public.organization(id);

ALTER TABLE
    ONLY public.person_event_attendance
ADD
    CONSTRAINT fkm5pabanv10jnhsjopv9xwog9c FOREIGN KEY (person_id) REFERENCES public.person(id);