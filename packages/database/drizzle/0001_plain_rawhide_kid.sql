CREATE TABLE "invitations" (
	"token" varchar(32) PRIMARY KEY NOT NULL,
	"created_at" timestamp DEFAULT now() NOT NULL
);
