# Kafka & Kafka Streams POC

This is a fun side project I have been working on for a bit, mostly for learning purposes. 
The idea is to set up an end to end data pipeline, where the service receives messages via 
an API built using FastAPI, sends them to the Kafka cluster, which then sends them to a
Kafka Streams application. The Kafka Streams application performs basic aggregations over
the messages, and then returns them to the Kafka cluster. These aggregations are then 
consumed by a python-based consumer, which sends them to a Postgres database, from where
they are exposed via the FastAPI service.

Work to do:
1. Security of API,
2. Better handling of schemas of db via SQLAlchemy
3. Logging and monitoring,
4. Better handling of exceptions,
5. Better handling of inputs in API,
6. Testing,
7. Use schema registry and AVRO serialization to pass produce/consume messages,
8. Get env vars in the kafka streams code
