--
-- PostgreSQL database dump
--
-- Dumped from database version 13.6
-- Dumped by pg_dump version 15.3
-- Started on 2024-03-15 15:54:35
-- SET statement_timeout = 0;
-- SET lock_timeout = 0;
-- SET idle_in_transaction_session_timeout = 0;
-- SET client_encoding = 'UTF8';
-- SET standard_conforming_strings = on;
-- SELECT pg_catalog.set_config('search_path', '', false);
-- SET check_function_bodies = false;
-- SET xmloption = content;
-- SET client_min_messages = warning;
-- SET row_security = off;
--
-- TOC entry 4 (class 2615 OID 2200)
-- Name: public; Type: SCHEMA; Schema: -; Owner: -
--
-- CREATE SCHEMA public;
--
-- TOC entry 3042 (class 0 OID 0)
-- Dependencies: 4
-- Name: SCHEMA public; Type: COMMENT; Schema: -; Owner: -
--
-- COMMENT ON SCHEMA public IS 'standard public schema';
-- SET
--     default_tablespace = '';
-- SET
--     default_table_access_method = heap;
--
-- TOC entry 201 (class 1259 OID 106985)
-- Name: attendance; Type: TABLE; Schema: public; Owner: -
--
CREATE TABLE public.attendance (
    create_date timestamp(6) without time zone,
    event_id bigint NOT NULL,
    id bigint NOT NULL,
    last_modified_date timestamp(6) without time zone,
    person_id bigint NOT NULL
);

--
-- TOC entry 200 (class 1259 OID 106983)
-- Name: attendance_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--
CREATE SEQUENCE public.attendance_id_seq START WITH 1 INCREMENT BY 1 NO MINVALUE NO MAXVALUE CACHE 1;

--
-- TOC entry 3044 (class 0 OID 0)
-- Dependencies: 200
-- Name: attendance_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--
ALTER SEQUENCE public.attendance_id_seq OWNED BY public.attendance.id;

--
-- TOC entry 203 (class 1259 OID 106993)
-- Name: event; Type: TABLE; Schema: public; Owner: -
--
CREATE TABLE public.event (
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
-- TOC entry 202 (class 1259 OID 106991)
-- Name: event_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--
CREATE SEQUENCE public.event_id_seq START WITH 1 INCREMENT BY 1 NO MINVALUE NO MAXVALUE CACHE 1;

--
-- TOC entry 3045 (class 0 OID 0)
-- Dependencies: 202
-- Name: event_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--
ALTER SEQUENCE public.event_id_seq OWNED BY public.event.id;

--
-- TOC entry 204 (class 1259 OID 107002)
-- Name: event_speakers; Type: TABLE; Schema: public; Owner: -
--
CREATE TABLE public.event_speakers (
    event_id bigint NOT NULL,
    speakers_id bigint NOT NULL
);

--
-- TOC entry 206 (class 1259 OID 107009)
-- Name: organization; Type: TABLE; Schema: public; Owner: -
--
CREATE TABLE public.organization (
    create_date timestamp(6) without time zone,
    id bigint NOT NULL,
    last_modified_date timestamp(6) without time zone,
    parent_id bigint,
    address character varying(255),
    name character varying(255),
    phone_number character varying(255)
);

--
-- TOC entry 205 (class 1259 OID 107007)
-- Name: organization_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--
CREATE SEQUENCE public.organization_id_seq START WITH 1 INCREMENT BY 1 NO MINVALUE NO MAXVALUE CACHE 1;

--
-- TOC entry 3046 (class 0 OID 0)
-- Dependencies: 205
-- Name: organization_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--
ALTER SEQUENCE public.organization_id_seq OWNED BY public.organization.id;

--
-- TOC entry 208 (class 1259 OID 107020)
-- Name: person; Type: TABLE; Schema: public; Owner: -
--
CREATE TABLE public.person (
    age integer,
    birthdate date,
    has_been_baptized boolean,
    create_date timestamp(6) without time zone,
    id bigint NOT NULL,
    last_modified_date timestamp(6) without time zone,
    organization_id bigint NOT NULL,
    first_name character varying(255),
    last_name character varying(255),
    phone_number character varying(255)
);

--
-- TOC entry 207 (class 1259 OID 107018)
-- Name: person_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--
CREATE SEQUENCE public.person_id_seq START WITH 1 INCREMENT BY 1 NO MINVALUE NO MAXVALUE CACHE 1;

--
-- TOC entry 3047 (class 0 OID 0)
-- Dependencies: 207
-- Name: person_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--
ALTER SEQUENCE public.person_id_seq OWNED BY public.person.id;

--
-- TOC entry 2877 (class 2604 OID 106988)
-- Name: attendance id; Type: DEFAULT; Schema: public; Owner: -
--
ALTER TABLE
    ONLY public.attendance
ALTER COLUMN
    id
SET
    DEFAULT nextval('public.attendance_id_seq' :: regclass);

--
-- TOC entry 2878 (class 2604 OID 106996)
-- Name: event id; Type: DEFAULT; Schema: public; Owner: -
--
ALTER TABLE
    ONLY public.event
ALTER COLUMN
    id
SET
    DEFAULT nextval('public.event_id_seq' :: regclass);

--
-- TOC entry 2879 (class 2604 OID 107012)
-- Name: organization id; Type: DEFAULT; Schema: public; Owner: -
--
ALTER TABLE
    ONLY public.organization
ALTER COLUMN
    id
SET
    DEFAULT nextval('public.organization_id_seq' :: regclass);

--
-- TOC entry 2880 (class 2604 OID 107023)
-- Name: person id; Type: DEFAULT; Schema: public; Owner: -
--
ALTER TABLE
    ONLY public.person
ALTER COLUMN
    id
SET
    DEFAULT nextval('public.person_id_seq' :: regclass);

-- TOC entry 3048 (class 0 OID 0)
-- Dependencies: 200
-- Name: attendance_id_seq; Type: SEQUENCE SET; Schema: public; Owner: -
--
SELECT
    pg_catalog.setval('public.attendance_id_seq', 1, false);

--
-- TOC entry 3049 (class 0 OID 0)
-- Dependencies: 202
-- Name: event_id_seq; Type: SEQUENCE SET; Schema: public; Owner: -
--
SELECT
    pg_catalog.setval('public.event_id_seq', 1, false);

--
-- TOC entry 3050 (class 0 OID 0)
-- Dependencies: 205
-- Name: organization_id_seq; Type: SEQUENCE SET; Schema: public; Owner: -
--
SELECT
    pg_catalog.setval('public.organization_id_seq', 1, false);

--
-- TOC entry 3051 (class 0 OID 0)
-- Dependencies: 207
-- Name: person_id_seq; Type: SEQUENCE SET; Schema: public; Owner: -
--
SELECT
    pg_catalog.setval('public.person_id_seq', 1, false);

--
-- TOC entry 2882 (class 2606 OID 106990)
-- Name: attendance attendance_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--
ALTER TABLE
    ONLY public.attendance
ADD
    CONSTRAINT attendance_pkey PRIMARY KEY (id);

--
-- TOC entry 2884 (class 2606 OID 107001)
-- Name: event event_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--
ALTER TABLE
    ONLY public.event
ADD
    CONSTRAINT event_pkey PRIMARY KEY (id);

--
-- TOC entry 2886 (class 2606 OID 107006)
-- Name: event_speakers event_speakers_speakers_id_key; Type: CONSTRAINT; Schema: public; Owner: -
--
ALTER TABLE
    ONLY public.event_speakers
ADD
    CONSTRAINT event_speakers_speakers_id_key UNIQUE (speakers_id);

--
-- TOC entry 2888 (class 2606 OID 107017)
-- Name: organization organization_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--
ALTER TABLE
    ONLY public.organization
ADD
    CONSTRAINT organization_pkey PRIMARY KEY (id);

--
-- TOC entry 2890 (class 2606 OID 107028)
-- Name: person person_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--
ALTER TABLE
    ONLY public.person
ADD
    CONSTRAINT person_pkey PRIMARY KEY (id);

--
-- TOC entry 2897 (class 2606 OID 107059)
-- Name: person fk23c7ajo7g43ugmupov5banbas; Type: FK CONSTRAINT; Schema: public; Owner: -
--
ALTER TABLE
    ONLY public.person
ADD
    CONSTRAINT fk23c7ajo7g43ugmupov5banbas FOREIGN KEY (organization_id) REFERENCES public.organization(id);

--
-- TOC entry 2894 (class 2606 OID 107049)
-- Name: event_speakers fk4mpddj4rg3nftjefml17ou0sm; Type: FK CONSTRAINT; Schema: public; Owner: -
--
ALTER TABLE
    ONLY public.event_speakers
ADD
    CONSTRAINT fk4mpddj4rg3nftjefml17ou0sm FOREIGN KEY (event_id) REFERENCES public.event(id);

--
-- TOC entry 2895 (class 2606 OID 107044)
-- Name: event_speakers fkad2vl2yymugehhjsqembv1yfa; Type: FK CONSTRAINT; Schema: public; Owner: -
--
ALTER TABLE
    ONLY public.event_speakers
ADD
    CONSTRAINT fkad2vl2yymugehhjsqembv1yfa FOREIGN KEY (speakers_id) REFERENCES public.person(id);

--
-- TOC entry 2896 (class 2606 OID 107054)
-- Name: organization fkc30yedjwp9qw1f3nn2ytda7tj; Type: FK CONSTRAINT; Schema: public; Owner: -
--
ALTER TABLE
    ONLY public.organization
ADD
    CONSTRAINT fkc30yedjwp9qw1f3nn2ytda7tj FOREIGN KEY (parent_id) REFERENCES public.organization(id);

--
-- TOC entry 2891 (class 2606 OID 107034)
-- Name: attendance fkd5wmpdsbs8oyb3b2amd78lfev; Type: FK CONSTRAINT; Schema: public; Owner: -
--
ALTER TABLE
    ONLY public.attendance
ADD
    CONSTRAINT fkd5wmpdsbs8oyb3b2amd78lfev FOREIGN KEY (person_id) REFERENCES public.person(id);

--
-- TOC entry 2893 (class 2606 OID 107039)
-- Name: event fkkarqc3c84scr3r5ncv5stqbk2; Type: FK CONSTRAINT; Schema: public; Owner: -
--
ALTER TABLE
    ONLY public.event
ADD
    CONSTRAINT fkkarqc3c84scr3r5ncv5stqbk2 FOREIGN KEY (organization_id) REFERENCES public.organization(id);

--
-- TOC entry 2892 (class 2606 OID 107029)
-- Name: attendance fkni62lygquw44n5bjepuapl1be; Type: FK CONSTRAINT; Schema: public; Owner: -
--
ALTER TABLE
    ONLY public.attendance
ADD
    CONSTRAINT fkni62lygquw44n5bjepuapl1be FOREIGN KEY (event_id) REFERENCES public.event(id);