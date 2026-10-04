UPDATE student_info
SET grade = CASE BTRIM(grade)
    WHEN 'HIGH_1' THEN 'HIGH_1'
    WHEN 'HIGH_2' THEN 'HIGH_2'
    WHEN 'HIGH_3' THEN 'HIGH_3'
    WHEN '高一' THEN 'HIGH_1'
    WHEN '高二' THEN 'HIGH_2'
    WHEN '高三' THEN 'HIGH_3'
    ELSE NULL
END
WHERE grade IS NOT NULL;

UPDATE student_info
SET class_name = SUBSTRING(BTRIM(class_name) FROM '^([0-9]{1,3})班$')
WHERE class_name IS NOT NULL
  AND BTRIM(class_name) ~ '^[0-9]{1,3}班$';

UPDATE student_info
SET class_name = BTRIM(class_name)
WHERE class_name IS NOT NULL
  AND BTRIM(class_name) ~ '^[0-9]{1,3}$';

UPDATE student_info
SET class_name = NULL
WHERE class_name IS NOT NULL
  AND BTRIM(class_name) !~ '^[0-9]{1,3}$';
