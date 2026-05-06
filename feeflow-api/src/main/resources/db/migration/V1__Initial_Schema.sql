-- Flyway Migration V1: Initial DB Setup

CREATE TABLE IF NOT EXISTS public.institutes
(
    id serial NOT NULL,
    name character varying(255) COLLATE pg_catalog."default",
    city character varying(255) COLLATE pg_catalog."default",
    state character varying(255) COLLATE pg_catalog."default",
    phone character varying(20) COLLATE pg_catalog."default",
    email character varying(255) COLLATE pg_catalog."default",
    created_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    updated_at timestamp without time zone,
    CONSTRAINT institutes_pkey PRIMARY KEY (id)
);

CREATE TABLE IF NOT EXISTS public.users
(
    id serial NOT NULL,
    institute_id integer,
    name character varying(255) COLLATE pg_catalog."default",
    email character varying(255) COLLATE pg_catalog."default",
    phone character varying(20) COLLATE pg_catalog."default",
    password_hash character varying(255) COLLATE pg_catalog."default",
    status character varying(50) COLLATE pg_catalog."default",
    created_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    updated_at timestamp without time zone,
    CONSTRAINT users_pkey PRIMARY KEY (id),
    CONSTRAINT users_institute_id_fkey FOREIGN KEY (institute_id)
        REFERENCES public.institutes (id) MATCH SIMPLE
        ON UPDATE NO ACTION
        ON DELETE NO ACTION
);

CREATE TABLE IF NOT EXISTS public.user_roles
(
    id serial NOT NULL,
    user_id integer NOT NULL,
    institute_id integer NOT NULL,
    role character varying(50) COLLATE pg_catalog."default",
    CONSTRAINT user_roles_pkey PRIMARY KEY (id),
    CONSTRAINT user_roles_user_id_fkey FOREIGN KEY (user_id)
        REFERENCES public.users (id) MATCH SIMPLE
        ON UPDATE NO ACTION
        ON DELETE NO ACTION,
    CONSTRAINT user_roles_institute_id_fkey FOREIGN KEY (institute_id)
        REFERENCES public.institutes (id) MATCH SIMPLE
        ON UPDATE NO ACTION
        ON DELETE NO ACTION,
    CONSTRAINT user_roles_unique UNIQUE (user_id, institute_id, role)
);

CREATE TABLE IF NOT EXISTS public.batches
(
    id serial NOT NULL,
    institute_id integer,
    name character varying(255) COLLATE pg_catalog."default",
    course_name character varying(255) COLLATE pg_catalog."default",
    start_date date,
    end_date date,
    status character varying(50) COLLATE pg_catalog."default",
    created_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT batches_pkey PRIMARY KEY (id),
    CONSTRAINT batches_institute_id_fkey FOREIGN KEY (institute_id)
        REFERENCES public.institutes (id) MATCH SIMPLE
        ON UPDATE NO ACTION
        ON DELETE NO ACTION
);

CREATE TABLE IF NOT EXISTS public.students
(
    id serial NOT NULL,
    institute_id integer,
    batch_id integer,
    name character varying(255) COLLATE pg_catalog."default",
    parent_name character varying(255) COLLATE pg_catalog."default",
    primary_phone character varying(20) COLLATE pg_catalog."default",
    whatsapp_phone character varying(20) COLLATE pg_catalog."default",
    enrollment_date date,
    status character varying(50) COLLATE pg_catalog."default",
    created_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    updated_at timestamp without time zone,
    CONSTRAINT students_pkey PRIMARY KEY (id),
    CONSTRAINT students_institute_id_fkey FOREIGN KEY (institute_id)
        REFERENCES public.institutes (id) MATCH SIMPLE
        ON UPDATE NO ACTION
        ON DELETE NO ACTION,
    CONSTRAINT students_batch_id_fkey FOREIGN KEY (batch_id)
        REFERENCES public.batches (id) MATCH SIMPLE
        ON UPDATE NO ACTION
        ON DELETE NO ACTION
);

CREATE TABLE IF NOT EXISTS public.audit_logs
(
    id serial NOT NULL,
    institute_id integer,
    user_id integer,
    action_type character varying(100) COLLATE pg_catalog."default",
    entity_type character varying(100) COLLATE pg_catalog."default",
    entity_id integer,
    metadata json,
    created_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT audit_logs_pkey PRIMARY KEY (id),
    CONSTRAINT audit_logs_institute_id_fkey FOREIGN KEY (institute_id)
        REFERENCES public.institutes (id) MATCH SIMPLE
        ON UPDATE NO ACTION
        ON DELETE NO ACTION,
    CONSTRAINT audit_logs_user_id_fkey FOREIGN KEY (user_id)
        REFERENCES public.users (id) MATCH SIMPLE
        ON UPDATE NO ACTION
        ON DELETE NO ACTION
);

CREATE TABLE IF NOT EXISTS public.broadcast_messages
(
    id serial NOT NULL,
    institute_id integer,
    created_by integer,
    message_body text COLLATE pg_catalog."default",
    target_batch_id integer,
    status character varying(50) COLLATE pg_catalog."default",
    created_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT broadcast_messages_pkey PRIMARY KEY (id),
    CONSTRAINT broadcast_messages_created_by_fkey FOREIGN KEY (created_by)
        REFERENCES public.users (id) MATCH SIMPLE
        ON UPDATE NO ACTION
        ON DELETE NO ACTION,
    CONSTRAINT broadcast_messages_institute_id_fkey FOREIGN KEY (institute_id)
        REFERENCES public.institutes (id) MATCH SIMPLE
        ON UPDATE NO ACTION
        ON DELETE NO ACTION,
    CONSTRAINT broadcast_messages_target_batch_id_fkey FOREIGN KEY (target_batch_id)
        REFERENCES public.batches (id) MATCH SIMPLE
        ON UPDATE NO ACTION
        ON DELETE NO ACTION
);

CREATE TABLE IF NOT EXISTS public.broadcast_recipients
(
    id serial NOT NULL,
    broadcast_id integer,
    student_id integer,
    status character varying(50) COLLATE pg_catalog."default",
    sent_at timestamp without time zone,
    CONSTRAINT broadcast_recipients_pkey PRIMARY KEY (id),
    CONSTRAINT broadcast_recipients_broadcast_id_fkey FOREIGN KEY (broadcast_id)
        REFERENCES public.broadcast_messages (id) MATCH SIMPLE
        ON UPDATE NO ACTION
        ON DELETE NO ACTION,
    CONSTRAINT broadcast_recipients_student_id_fkey FOREIGN KEY (student_id)
        REFERENCES public.students (id) MATCH SIMPLE
        ON UPDATE NO ACTION
        ON DELETE NO ACTION
);

CREATE TABLE IF NOT EXISTS public.fee_structures
(
    id serial NOT NULL,
    institute_id integer,
    batch_id integer,
    total_fee numeric(12, 2),
    currency character varying(10) COLLATE pg_catalog."default",
    is_active boolean,
    created_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fee_structures_pkey PRIMARY KEY (id),
    CONSTRAINT fee_structures_institute_id_fkey FOREIGN KEY (institute_id)
        REFERENCES public.institutes (id) MATCH SIMPLE
        ON UPDATE NO ACTION
        ON DELETE NO ACTION,
    CONSTRAINT fee_structures_batch_id_fkey FOREIGN KEY (batch_id)
        REFERENCES public.batches (id) MATCH SIMPLE
        ON UPDATE NO ACTION
        ON DELETE NO ACTION
);

CREATE TABLE IF NOT EXISTS public.fee_installment_templates
(
    id serial NOT NULL,
    fee_structure_id integer,
    installment_number integer,
    name character varying(255) COLLATE pg_catalog."default",
    amount numeric(12, 2),
    due_offset_days integer,
    created_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fee_installment_templates_pkey PRIMARY KEY (id),
    CONSTRAINT fee_installment_templates_fee_structure_id_fkey FOREIGN KEY (fee_structure_id)
        REFERENCES public.fee_structures (id) MATCH SIMPLE
        ON UPDATE NO ACTION
        ON DELETE NO ACTION
);

CREATE TABLE IF NOT EXISTS public.student_fees
(
    id serial NOT NULL,
    institute_id integer,
    student_id integer,
    batch_id integer,
    fee_structure_id integer,
    original_fee numeric(12, 2),
    final_fee numeric(12, 2),
    status character varying(50) COLLATE pg_catalog."default",
    created_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT student_fees_pkey PRIMARY KEY (id),
    CONSTRAINT student_fees_institute_id_fkey FOREIGN KEY (institute_id)
        REFERENCES public.institutes (id) MATCH SIMPLE
        ON UPDATE NO ACTION
        ON DELETE NO ACTION,
    CONSTRAINT student_fees_student_id_fkey FOREIGN KEY (student_id)
        REFERENCES public.students (id) MATCH SIMPLE
        ON UPDATE NO ACTION
        ON DELETE NO ACTION,
    CONSTRAINT student_fees_batch_id_fkey FOREIGN KEY (batch_id)
        REFERENCES public.batches (id) MATCH SIMPLE
        ON UPDATE NO ACTION
        ON DELETE NO ACTION,
    CONSTRAINT student_fees_fee_structure_id_fkey FOREIGN KEY (fee_structure_id)
        REFERENCES public.fee_structures (id) MATCH SIMPLE
        ON UPDATE NO ACTION
        ON DELETE NO ACTION
);

CREATE TABLE IF NOT EXISTS public.installments
(
    id serial NOT NULL,
    institute_id integer,
    student_fee_id integer,
    installment_number integer,
    due_date date,
    amount_due numeric(12, 2),
    amount_paid numeric(12, 2) DEFAULT 0,
    status character varying(50) COLLATE pg_catalog."default",
    created_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT installments_pkey PRIMARY KEY (id),
    CONSTRAINT installments_institute_id_fkey FOREIGN KEY (institute_id)
        REFERENCES public.institutes (id) MATCH SIMPLE
        ON UPDATE NO ACTION
        ON DELETE NO ACTION,
    CONSTRAINT installments_student_fee_id_fkey FOREIGN KEY (student_fee_id)
        REFERENCES public.student_fees (id) MATCH SIMPLE
        ON UPDATE NO ACTION
        ON DELETE NO ACTION
);

CREATE TABLE IF NOT EXISTS public.payments
(
    id serial NOT NULL,
    institute_id integer,
    student_id integer,
    amount numeric(12, 2),
    payment_mode character varying(50) COLLATE pg_catalog."default",
    reference_id character varying(255) COLLATE pg_catalog."default",
    marked_by integer,
    payment_date timestamp without time zone,
    created_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT payments_pkey PRIMARY KEY (id),
    CONSTRAINT payments_institute_id_fkey FOREIGN KEY (institute_id)
        REFERENCES public.institutes (id) MATCH SIMPLE
        ON UPDATE NO ACTION
        ON DELETE NO ACTION,
    CONSTRAINT payments_student_id_fkey FOREIGN KEY (student_id)
        REFERENCES public.students (id) MATCH SIMPLE
        ON UPDATE NO ACTION
        ON DELETE NO ACTION,
    CONSTRAINT payments_marked_by_fkey FOREIGN KEY (marked_by)
        REFERENCES public.users (id) MATCH SIMPLE
        ON UPDATE NO ACTION
        ON DELETE NO ACTION
);

CREATE TABLE IF NOT EXISTS public.payment_allocations
(
    id serial NOT NULL,
    payment_id integer,
    installment_id integer,
    allocated_amount numeric(12, 2),
    created_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT payment_allocations_pkey PRIMARY KEY (id),
    CONSTRAINT payment_allocations_payment_id_fkey FOREIGN KEY (payment_id)
        REFERENCES public.payments (id) MATCH SIMPLE
        ON UPDATE NO ACTION
        ON DELETE NO ACTION,
    CONSTRAINT payment_allocations_installment_id_fkey FOREIGN KEY (installment_id)
        REFERENCES public.installments (id) MATCH SIMPLE
        ON UPDATE NO ACTION
        ON DELETE NO ACTION
);

CREATE TABLE IF NOT EXISTS public.receipts
(
    id serial NOT NULL,
    institute_id integer,
    payment_id integer,
    receipt_number character varying(255) COLLATE pg_catalog."default",
    generated_at timestamp without time zone,
    sent_to_parent boolean,
    CONSTRAINT receipts_pkey PRIMARY KEY (id),
    CONSTRAINT receipts_institute_id_fkey FOREIGN KEY (institute_id)
        REFERENCES public.institutes (id) MATCH SIMPLE
        ON UPDATE NO ACTION
        ON DELETE NO ACTION,
    CONSTRAINT receipts_payment_id_fkey FOREIGN KEY (payment_id)
        REFERENCES public.payments (id) MATCH SIMPLE
        ON UPDATE NO ACTION
        ON DELETE NO ACTION
);

CREATE TABLE IF NOT EXISTS public.reminder_rules
(
    id serial NOT NULL,
    institute_id integer,
    type character varying(50) COLLATE pg_catalog."default",
    offset_days integer,
    template text COLLATE pg_catalog."default",
    is_active boolean,
    created_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT reminder_rules_pkey PRIMARY KEY (id),
    CONSTRAINT reminder_rules_institute_id_fkey FOREIGN KEY (institute_id)
        REFERENCES public.institutes (id) MATCH SIMPLE
        ON UPDATE NO ACTION
        ON DELETE NO ACTION
);

CREATE TABLE IF NOT EXISTS public.reminder_logs
(
    id serial NOT NULL,
    institute_id integer,
    installment_id integer,
    student_id integer,
    rule_id integer,
    channel character varying(50) COLLATE pg_catalog."default",
    parent_phone character varying(20) COLLATE pg_catalog."default",
    message_body text COLLATE pg_catalog."default",
    status character varying(50) COLLATE pg_catalog."default",
    sent_at timestamp without time zone,
    created_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT reminder_logs_pkey PRIMARY KEY (id),
    CONSTRAINT reminder_logs_institute_id_fkey FOREIGN KEY (institute_id)
        REFERENCES public.institutes (id) MATCH SIMPLE
        ON UPDATE NO ACTION
        ON DELETE NO ACTION,
    CONSTRAINT reminder_logs_installment_id_fkey FOREIGN KEY (installment_id)
        REFERENCES public.installments (id) MATCH SIMPLE
        ON UPDATE NO ACTION
        ON DELETE NO ACTION,
    CONSTRAINT reminder_logs_student_id_fkey FOREIGN KEY (student_id)
        REFERENCES public.students (id) MATCH SIMPLE
        ON UPDATE NO ACTION
        ON DELETE NO ACTION,
    CONSTRAINT reminder_logs_rule_id_fkey FOREIGN KEY (rule_id)
        REFERENCES public.reminder_rules (id) MATCH SIMPLE
        ON UPDATE NO ACTION
        ON DELETE NO ACTION
);

CREATE TABLE IF NOT EXISTS public.message_delivery_events
(
    id serial NOT NULL,
    reminder_log_id integer,
    event_type character varying(50) COLLATE pg_catalog."default",
    provider_status character varying(50) COLLATE pg_catalog."default",
    payload json,
    event_time timestamp without time zone,
    CONSTRAINT message_delivery_events_pkey PRIMARY KEY (id),
    CONSTRAINT message_delivery_events_reminder_log_id_fkey FOREIGN KEY (reminder_log_id)
        REFERENCES public.reminder_logs (id) MATCH SIMPLE
        ON UPDATE NO ACTION
        ON DELETE NO ACTION
);

CREATE TABLE IF NOT EXISTS public.message_retry_queue
(
    id serial NOT NULL,
    reminder_log_id integer,
    retry_count integer,
    next_retry_at timestamp without time zone,
    last_error text COLLATE pg_catalog."default",
    status character varying(50) COLLATE pg_catalog."default",
    created_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT message_retry_queue_pkey PRIMARY KEY (id),
    CONSTRAINT message_retry_queue_reminder_log_id_fkey FOREIGN KEY (reminder_log_id)
        REFERENCES public.reminder_logs (id) MATCH SIMPLE
        ON UPDATE NO ACTION
        ON DELETE NO ACTION
);

CREATE TABLE IF NOT EXISTS public.student_fee_discounts
(
    id serial NOT NULL,
    student_fee_id integer,
    discount_type character varying(50) COLLATE pg_catalog."default",
    amount numeric(12, 2),
    reason text COLLATE pg_catalog."default",
    apply_to_remaining_installments boolean,
    created_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT student_fee_discounts_pkey PRIMARY KEY (id),
    CONSTRAINT student_fee_discounts_student_fee_id_fkey FOREIGN KEY (student_fee_id)
        REFERENCES public.student_fees (id) MATCH SIMPLE
        ON UPDATE NO ACTION
        ON DELETE NO ACTION
);
