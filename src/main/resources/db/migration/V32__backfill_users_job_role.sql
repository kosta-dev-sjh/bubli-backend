UPDATE users
SET job_role = user_preferences.job_role
FROM user_preferences
WHERE user_preferences.user_id = users.id
  AND user_preferences.job_role IS NOT NULL
  AND users.job_role IS NULL;
