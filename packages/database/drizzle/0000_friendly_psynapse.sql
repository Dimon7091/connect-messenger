CREATE TYPE "public"."user_role" AS ENUM('admin', 'user');--> statement-breakpoint
CREATE TABLE "users" (
	"id" uuid PRIMARY KEY DEFAULT gen_random_uuid() NOT NULL,
	"user_name" varchar(255) NOT NULL,
	"password_hash" varchar(255) NOT NULL,
	"role" "user_role" DEFAULT 'user' NOT NULL,
	"first_name" varchar(255) NOT NULL,
	"last_name" varchar(255),
	"avatar_url" varchar(512),
	"is_failed_login_locked" boolean DEFAULT false NOT NULL,
	"is_banned" boolean DEFAULT false NOT NULL,
	"is_deleted" boolean DEFAULT false NOT NULL,
	"created_at" timestamp DEFAULT now() NOT NULL,
	CONSTRAINT "users_user_name_unique" UNIQUE("user_name")
);
--> statement-breakpoint
CREATE TABLE "invitations" (
	"token" varchar(32) PRIMARY KEY NOT NULL,
	"created_at" timestamp DEFAULT now() NOT NULL
);
--> statement-breakpoint
CREATE TABLE "chats" (
	"id" bigint PRIMARY KEY NOT NULL,
	"direct_key" text,
	"type" varchar(20) NOT NULL,
	"last_message" text,
	"created_at" timestamp DEFAULT now() NOT NULL,
	"updated_at" timestamp DEFAULT now() NOT NULL,
	"created_by" uuid NOT NULL,
	CONSTRAINT "chats_direct_key_unique" UNIQUE("direct_key")
);
--> statement-breakpoint
CREATE TABLE "chat_participants" (
	"chat_id" bigint NOT NULL,
	"user_id" uuid NOT NULL,
	"is_deleted" boolean DEFAULT false NOT NULL,
	"deleted_at" timestamp with time zone,
	"unread_count" integer DEFAULT 0 NOT NULL,
	"last_read_message_id" bigint,
	"last_read_at" timestamp with time zone,
	"is_chat_empty" boolean DEFAULT true,
	"is_muted" boolean DEFAULT false NOT NULL,
	"pinned_at" timestamp with time zone,
	"joined_at" timestamp with time zone DEFAULT now() NOT NULL,
	CONSTRAINT "chat_participants_chat_id_user_id_pk" PRIMARY KEY("chat_id","user_id")
);
--> statement-breakpoint
CREATE TABLE "messages" (
	"id" bigint PRIMARY KEY NOT NULL,
	"chat_id" bigint NOT NULL,
	"sender_id" uuid NOT NULL,
	"receiver_id" uuid,
	"text" text,
	"timestamp" timestamp with time zone DEFAULT now() NOT NULL,
	"created_at" timestamp with time zone DEFAULT now() NOT NULL,
	"status" varchar(20) DEFAULT 'SENT' NOT NULL,
	"reply_to_id" bigint,
	"attachments" jsonb DEFAULT '[]'::jsonb,
	"deleted_by" jsonb DEFAULT '[]'::jsonb NOT NULL,
	"read_by" jsonb DEFAULT '[]'::jsonb
);
--> statement-breakpoint
ALTER TABLE "chat_participants" ADD CONSTRAINT "chat_participants_chat_id_chats_id_fk" FOREIGN KEY ("chat_id") REFERENCES "public"."chats"("id") ON DELETE cascade ON UPDATE no action;--> statement-breakpoint
ALTER TABLE "chat_participants" ADD CONSTRAINT "chat_participants_user_id_users_id_fk" FOREIGN KEY ("user_id") REFERENCES "public"."users"("id") ON DELETE cascade ON UPDATE no action;--> statement-breakpoint
ALTER TABLE "messages" ADD CONSTRAINT "messages_chat_id_chats_id_fk" FOREIGN KEY ("chat_id") REFERENCES "public"."chats"("id") ON DELETE cascade ON UPDATE no action;--> statement-breakpoint
ALTER TABLE "messages" ADD CONSTRAINT "messages_sender_id_users_id_fk" FOREIGN KEY ("sender_id") REFERENCES "public"."users"("id") ON DELETE cascade ON UPDATE no action;--> statement-breakpoint
ALTER TABLE "messages" ADD CONSTRAINT "messages_receiver_id_users_id_fk" FOREIGN KEY ("receiver_id") REFERENCES "public"."users"("id") ON DELETE set null ON UPDATE no action;--> statement-breakpoint
CREATE INDEX "users_created_at_id_idx" ON "users" USING btree ("created_at","id");--> statement-breakpoint
CREATE INDEX "users_user_name_id_idx" ON "users" USING btree ("user_name","id");--> statement-breakpoint
CREATE INDEX "users_first_name_id_idx" ON "users" USING btree ("first_name","id");--> statement-breakpoint
CREATE INDEX "users_last_name_id_idx" ON "users" USING btree ("last_name","id");--> statement-breakpoint
CREATE INDEX "users_role_created_at_id_idx" ON "users" USING btree ("role","created_at","id");--> statement-breakpoint
CREATE INDEX "chat_participants_user_id_idx" ON "chat_participants" USING btree ("user_id");--> statement-breakpoint
CREATE INDEX "idx_messages_chat_timestamp" ON "messages" USING btree ("chat_id","timestamp","read_by");--> statement-breakpoint
CREATE INDEX "idx_messages_chat_id_desc" ON "messages" USING btree ("chat_id","id");