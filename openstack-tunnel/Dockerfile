FROM rust:1-alpine AS build
RUN apk add --no-cache musl-dev
RUN cargo install boringtun-cli

FROM alpine:3.20
RUN apk add --no-cache wireguard-tools iproute2 bash
COPY --from=build /usr/local/cargo/bin/boringtun-cli /usr/local/bin/boringtun-cli
COPY entrypoint.sh /entrypoint.sh
RUN chmod +x /entrypoint.sh
ENTRYPOINT ["/entrypoint.sh"]
