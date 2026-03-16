/* =========== 1. TABLES CREATION ========== */

-- 1. ROLES (e.g., ADMIN,USER)
CREATE TABLE roles (
    id SERIAL CONSTRAINT pk_roles PRIMARY KEY,
    name VARCHAR(50) CONSTRAINT uq_roles_name UNIQUE NOT NULL,
    description TEXT
);

INSERT INTO roles (name,description) VALUES
('ADMIN','System Administrator - Full Access'),
('USER','Class Instructor, Learner');

-- 2. USERS (TEACHERS, STUDENTS)
CREATE TABLE users (
    id BIGSERIAL CONSTRAINT pk_users PRIMARY KEY,
    role_id INT NOT NULL CONSTRAINT fk_users_role REFERENCES roles(id),
    email VARCHAR(255) CONSTRAINT uq_users_email UNIQUE NOT NULL,
    password VARCHAR(255),
    full_name VARCHAR(100) NOT NULL,
    avatar_url TEXT,
    avatar_key TEXT,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    two_factor_enabled BOOLEAN NOT NULL DEFAULT FALSE,
    show_phone_to_students BOOLEAN NOT NULL DEFAULT FALSE,
    hide_student_section BOOLEAN NOT NULL DEFAULT FALSE,
    hide_teacher_section BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    promoted_by BIGINT CONSTRAINT fk_promoted_by REFERENCES users(id),
    promoted_at TIMESTAMP WITH TIME ZONE
);

-- 3. PHONE_NUMBER (e.g., +910123456789)
CREATE TABLE phone_numbers (
    id BIGSERIAL CONSTRAINT pk_phone_numbers PRIMARY KEY,
    user_id BIGINT NOT NULL CONSTRAINT fk_phone_number_user REFERENCES users(id) ON DELETE CASCADE,
    phone_number varchar(20) NOT NULL UNIQUE,
    is_primary BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- 4. SOCIAL_ACCOUNTS (auth providers)
CREATE TABLE social_accounts (
    id BIGSERIAL CONSTRAINT pk_social_accounts PRIMARY KEY,
    user_id BIGINT NOT NULL CONSTRAINT fk_social_accounts_user REFERENCES users(id) ON DELETE CASCADE,
    provider VARCHAR(20) NOT NULL CHECK (provider IN ('GOOGLE', 'GITHUB')),
    provider_id VARCHAR(255) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_social_accounts_provider_id UNIQUE(provider, provider_id)
);

-- 5. SUBJECTS (e.g., ENGLISH - Class 8th, JAVA - BCA(PLAIN) SEM 3)
CREATE TABLE subjects (
    id BIGSERIAL CONSTRAINT pk_subjects PRIMARY KEY,
    teacher_id BIGINT NOT NULL CONSTRAINT fk_subjects_teacher REFERENCES users(id) ON DELETE RESTRICT,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    join_code VARCHAR(10) CONSTRAINT uq_subjects_join_code UNIQUE NOT NULL,
    syllabus_file_url TEXT,
    syllabus_key TEXT,
    current_topic_id BIGINT,
    next_topic_id BIGINT,
    is_locked BOOLEAN NOT NULL DEFAULT FALSE,
    is_archived BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_current_next_topic_differ CHECK (current_topic_id IS NULL
    OR next_topic_id IS NULL OR current_topic_id != next_topic_id),
    CONSTRAINT uq_subject_teacher_name UNIQUE(teacher_id, name)
);

-- 6. UNITS (1,2,3...)
CREATE TABLE units (
    id BIGSERIAL CONSTRAINT pk_units PRIMARY KEY,
    subject_id BIGINT NOT NULL CONSTRAINT fk_units_subject REFERENCES subjects(id) ON DELETE CASCADE,
    title VARCHAR(255) NOT NULL,
    order_index INT NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_units_subject_order UNIQUE(subject_id,order_index),
    CONSTRAINT uq_units_subject_title UNIQUE(subject_id, title)
);

-- 7. TOPICS (e.g., History of JAVA)
CREATE TABLE topics (
    id BIGSERIAL CONSTRAINT pk_topics PRIMARY KEY,
    unit_id BIGINT NOT NULL CONSTRAINT fk_topics_unit REFERENCES units(id) ON DELETE CASCADE,
    title VARCHAR(255) NOT NULL,
    order_index INT NOT NULL,
    completed_at TIMESTAMP WITH TIME ZONE, -- Null = Not Completed
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_topics_unit_order UNIQUE(unit_id,order_index),
    CONSTRAINT uq_topics_unit_title UNIQUE(unit_id, title)
);

-- circular dependency prevention
ALTER TABLE subjects
    ADD CONSTRAINT fk_current_topic_id
    FOREIGN KEY (current_topic_id) REFERENCES topics(id) ON DELETE SET NULL;

ALTER TABLE subjects
    ADD CONSTRAINT fk_next_topic_id
    FOREIGN KEY (next_topic_id) REFERENCES topics(id) ON DELETE SET NULL;

-- 8. TOPIC_MATERIALS (pdf, doc, video, etc.)
CREATE TABLE topic_materials (
    id BIGSERIAL CONSTRAINT pk_topic_materials PRIMARY KEY,
    topic_id BIGINT NOT NULL CONSTRAINT fk_topic_materials_topic REFERENCES topics(id) ON DELETE CASCADE,
    title VARCHAR(255) NOT NULL,
    url TEXT NOT NULL,
    type VARCHAR(25) NOT NULL CONSTRAINT chk_topic_materials_type
                                        CHECK (type IN ('PDF', 'DOC', 'VIDEO', 'IMAGE', 'LINK')),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- 9. ENROLLMENTS (Students Enrolled in Subjects)
CREATE TABLE enrollments (
    id BIGSERIAL CONSTRAINT pk_enrollments PRIMARY KEY,
    student_id BIGINT NOT NULL CONSTRAINT fk_enrollments_student REFERENCES users(id) ON DELETE CASCADE,
    subject_id BIGINT NOT NULL CONSTRAINT fk_enrollments_subject REFERENCES subjects(id) ON DELETE CASCADE,
    joined_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_enrollments_student_subject UNIQUE(student_id, subject_id) -- Prevent a student joining the same class twice
);

-- 10. HOMEWORK (e.g., Read about the history of java)
CREATE TABLE homework (
    id BIGSERIAL CONSTRAINT pk_homework PRIMARY KEY,
    title VARCHAR(255) NOT NULL,
    subject_id BIGINT NOT NULL CONSTRAINT fk_homework_subject REFERENCES subjects(id) ON DELETE CASCADE,
    description TEXT,
    due_date TIMESTAMP WITH TIME ZONE NOT NULL ,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- 11. HOMEWORK TOPICS (separate table for multiple topic linking)
CREATE TABLE homework_topics (
    homework_id BIGINT NOT NULL CONSTRAINT fk_ht_homework REFERENCES homework(id) ON DELETE CASCADE,
    topic_id BIGINT NOT NULL CONSTRAINT fk_ht_topic REFERENCES topics(id) ON DELETE CASCADE,
    CONSTRAINT pk_homework_topics PRIMARY KEY (homework_id, topic_id) -- prevent duplicate links
);

-- 12. REFRESH TOKENS
CREATE TABLE refresh_tokens (
    id BIGSERIAL CONSTRAINT pk_refresh_tokens PRIMARY KEY,
    token VARCHAR(100) NOT NULL CONSTRAINT uq_refresh_tokens_token UNIQUE,
    expiry_date TIMESTAMP WITH TIME ZONE NOT NULL,
    user_id BIGINT NOT NULL CONSTRAINT fk_refresh_tokens_user REFERENCES users(id) ON DELETE CASCADE
);

/* =========== 2. INDEXES ========== */

-- 1. ACCOUNT MANAGEMENT
-- User opens profile settings OR deletes their account.
CREATE INDEX idx_social_accounts_user ON social_accounts(user_id);

-- For deleteById method which is used by logoutAll method
CREATE INDEX idx_refresh_tokens_user ON refresh_tokens(user_id);

-- Fetching users phone number
CREATE INDEX idx_phone_numbers_user_id ON phone_numbers(user_id);

-- 2. DASHBOARDS
-- Teacher logs in and needs to see all subjects they created excluding archived.
CREATE INDEX idx_subjects_teacher_active ON subjects(teacher_id)
    WHERE is_archived = FALSE;

-- Student logs in and needs to see all subjects they have joined.
CREATE INDEX idx_subjects_student ON enrollments(student_id);

-- Teacher clicks on a subject to see the list of enrolled students.
CREATE INDEX idx_enrollments_subject ON enrollments(subject_id);

-- 3. SYLLABUS
-- User clicks a Subject -> Fetch all Units in order
CREATE INDEX idx_units_subject ON units(subject_id);

-- User clicks a Unit -> Fetch all Topics in order
CREATE INDEX idx_topics_unit ON topics(unit_id);

-- User clicks a Topic -> Fetch all PDFs/Videos
CREATE INDEX idx_materials_topic ON topic_materials(topic_id);

-- 4. HOMEWORK & DEADLINES
-- User logs in and looks at Upcoming Deadlines
CREATE INDEX idx_homework_due_date ON homework(subject_id, due_date);

-- Fetching homework linked to a specific topic
CREATE INDEX idx_homework_topics_topic ON homework_topics(topic_id);