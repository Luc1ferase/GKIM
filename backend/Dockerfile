FROM debian:bookworm-slim AS build

WORKDIR /app
ENV CARGO_HOME=/usr/local/cargo
ENV RUSTUP_HOME=/usr/local/rustup
ENV PATH=/usr/local/cargo/bin:/usr/local/sbin:/usr/local/bin:/usr/sbin:/usr/bin:/sbin:/bin
ENV CARGO_TARGET_DIR=/tmp/target

RUN apt-get update \
    && apt-get install -y --no-install-recommends build-essential ca-certificates curl pkg-config \
    && rm -rf /var/lib/apt/lists/* \
    && curl https://sh.rustup.rs -sSf | sh -s -- -y --profile minimal --default-toolchain 1.88.0

COPY Cargo.toml Cargo.lock ./
COPY src ./src

RUN cargo build --release --bin gkim-im-backend

FROM debian:bookworm-slim

RUN apt-get update \
    && apt-get install -y --no-install-recommends ca-certificates \
    && rm -rf /var/lib/apt/lists/*

WORKDIR /app

COPY --from=build /tmp/target/release/gkim-im-backend /usr/local/bin/gkim-im-backend
COPY migrations ./migrations

ENV APP_SERVICE_NAME=gkim-im-backend
ENV APP_BIND_ADDR=0.0.0.0:8080
ENV APP_LOG_FILTER=info

EXPOSE 8080

ENTRYPOINT ["gkim-im-backend"]
