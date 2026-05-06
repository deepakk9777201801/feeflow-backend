-- Flyway Migration V2: Make user_roles.institute_id nullable
-- Allows super-admin users to have roles without institute association

ALTER TABLE public.user_roles
    ALTER COLUMN institute_id DROP NOT NULL;
