CREATE TABLE student_info (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    display_name VARCHAR(120),
    student_no VARCHAR(64),
    grade VARCHAR(64),
    class_name VARCHAR(120),
    phone VARCHAR(32),
    bio TEXT,
    avatar_path VARCHAR(255),
    avatar_updated_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_student_info_user UNIQUE (user_id),
    CONSTRAINT fk_student_info_user FOREIGN KEY (user_id) REFERENCES users(id)
);

