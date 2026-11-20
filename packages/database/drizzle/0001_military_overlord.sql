ALTER TABLE "usersProfiles" DISABLE ROW LEVEL SECURITY;--> statement-breakpoint
DROP TABLE "usersProfiles" CASCADE;--> statement-breakpoint
ALTER TABLE "users" ADD COLUMN "first_name" varchar(255) NOT NULL;--> statement-breakpoint
ALTER TABLE "users" ADD COLUMN "last_name" varchar(255);--> statement-breakpoint
ALTER TABLE "users" ADD COLUMN "avatar_url" varchar(512);--> statement-breakpoint
CREATE INDEX "users_created_at_id_idx" ON "users" USING btree ("created_at","id");--> statement-breakpoint
CREATE INDEX "users_user_name_id_idx" ON "users" USING btree ("user_name","id");--> statement-breakpoint
CREATE INDEX "users_first_name_id_idx" ON "users" USING btree ("first_name","id");--> statement-breakpoint
CREATE INDEX "users_last_name_id_idx" ON "users" USING btree ("last_name","id");--> statement-breakpoint
CREATE INDEX "users_role_created_at_id_idx" ON "users" USING btree ("role","created_at","id");