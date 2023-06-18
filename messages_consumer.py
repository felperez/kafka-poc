import psycopg2
import datetime
import re
import os
import json
from kafka import KafkaConsumer
from dotenv import load_dotenv


load_dotenv()

kafka_server_url = os.getenv("KAFKA_BROKERS_URL")
kafka_server_port = os.getenv("KAFKA_BROKERS_PORT")
kafka_server = f"{kafka_server_url}:{kafka_server_port}"
kafka_topic = os.getenv("KAFKA_TOPIC_AGGREGATIONS")


db_host = os.getenv("DB_HOST")
db_port = os.getenv("DB_PORT")
db_name = os.getenv("DB_NAME")
db_user = os.getenv("DB_USER")
db_password = os.getenv("DB_PASSWORD")


connection = psycopg2.connect(
    host=db_host,
    port=db_port,
    database=db_name,
    user=db_user,
    password=db_password
)
cursor = connection.cursor()
consumer = KafkaConsumer(kafka_topic, bootstrap_servers=kafka_server, auto_offset_reset='latest')


for message in consumer:

    message_value = json.loads(message.value.decode('utf-8'))

    product_id, orders, window_start =  message_value.get("product_id"), message_value.get("orders"), message_value.get("timegroup")

    orders = int(orders)
    window_start = int(window_start)

    timegroup = datetime.datetime.fromtimestamp(window_start / 1000)
    data = (product_id, timegroup, orders)

    insert_query = "INSERT INTO orders_by_minute (product_id, timegroup, orders) VALUES (%s, %s, %s) ON CONFLICT (product_id, timegroup) DO UPDATE SET orders = excluded.orders;"
    cursor.execute(insert_query, data)
    connection.commit()

consumer.close()
