# LEARNER-OWNED (Standard W14 / Fast W11). Multi-stage image for the FastAPI pipeline service.
# Nothing is built in this course; deploy/ checks read this file statically.
#
# TODO stage 1 (build): a pinned Python 3.14 image, named (FROM ... AS build); install the pipeline
#      and its dependencies (pip) into a virtual environment or wheel folder.
# TODO stage 2 (runtime): a pinned slim Python image; copy only the installed result from the build
#      stage; run as a non-root user; EXPOSE 8000; start the FastAPI app.
# Secrets (PGPASSWORD, SPRING_API_TOKEN) come from the runtime environment, never ARG/ENV here.
