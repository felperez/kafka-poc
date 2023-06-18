# Kafka & Kafka Streams POC

## Introduction

This is a fun side project I have been working on for a bit, mostly for learning purposes. 
The idea is to set up an end to end data pipeline, where the service receives messages via 
an API built using FastAPI, sends them to the Kafka cluster, which then sends them to a
Kafka Streams application. The Kafka Streams application performs basic aggregations over
the messages, and then returns them to the Kafka cluster. These aggregations are then 
consumed by a python-based consumer, which sends them to a Postgres database, from where
they are exposed via the FastAPI service.

## Tools used

Some of the tools I used in this project:
1. FastAPI: in order to quickly make an API to send the messages to and retrieve the 
aggregations from, I decided to use FastAPI, as it requires minimal setup and very little code.
2. Postgres: this is the flavor of SQL I'm most familiar with.
3. Migration: this is a Golang based tool that I used to keep track of the database schema.
4. Pydantic: to set the models used for the API data.
5. Asyncio, aiohttp: to make async requests to simulate high traffic to the endpoint.
6. Kafka: for the messages streaming
7. Kafka Streams: to consume one of the Kafka topics and perform aggregations.

## To make it work

The Kafka cluster runs in docker, for which the docker-compose is used. It mounts:
1. Zookeper,
2. The brokers,
3. Schema registry (unused for now),
4. Kafka UI,
5. Rest proxy (unused for now).

One also needs to run the messages consumer python script, as well as the api. These are not dockerized
for development convenience purposes. Finally, one also needs to build and run the Kafka Streams app, for which
I used Gradle. The requirements have not been made explicit as I find it unlikely that anyone else will try to run
this.


## Work to do
1. Security of API,
2. Better handling of schemas of db via SQLAlchemy
3. Logging and monitoring,
4. Better handling of exceptions,
5. Better handling of inputs in API,
6. Testing,
7. Use schema registry and AVRO serialization to pass produce/consume messages,
8. Get env vars in the kafka streams code,
9. Cloud deployment.