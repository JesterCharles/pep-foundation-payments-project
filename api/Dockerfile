# LEARNER-OWNED (Standard W14 / Fast W11). Multi-stage image for the Spring Boot API.
# Nothing is built in this course; deploy/ checks read this file statically.
#
# TODO stage 1 (build): a pinned Maven + JDK 25 image, named (FROM ... AS build); copy pom.xml and
#      src/, then package the application jar.
# TODO stage 2 (runtime): a pinned JRE-only (or otherwise slim) Java 25 image; copy only the jar from
#      the build stage; run as a non-root user; EXPOSE 8080; start the jar.
# Secrets (database password, JWT secret) come from the runtime environment, never ARG/ENV here.
