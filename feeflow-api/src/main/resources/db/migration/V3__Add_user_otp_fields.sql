-- Flyway Migration V3: Add OTP fields to users table
-- These fields are used for password reset functionality

ALTER TABLE public.users
    ADD COLUMN reset_otp character varying(10),
    ADD COLUMN otp_expiry timestamp without time zone;
